package education.cccp.academy.opencode

import contracts.runtime.LlmProviderKind
import education.cccp.academy.byok.ByokProviderCatalog
import education.cccp.academy.byok.ByokProviderSpec

/**
 * Pure `opencode.json` renderer (ACADEMY-5-1, multi-provider since ACADEMY-7-1)
 * — a deterministic function from [OpenCodeConfig] to the learner agent
 * configuration. No I/O, no Gradle types, and **no serialization library**: the
 * JSON is built by explicit literal concatenation (D-ACADEMY-5-3 / D-ACADEMY-7-4),
 * keeping the generator dependency-free.
 *
 * Contract:
 *  - the config declares the opencode `$schema` for editor validation
 *  - the provider is resolved from the N0 kind through [ByokProviderCatalog]:
 *    id, AI-SDK package and display name all come from the catalog (single
 *    source of truth — ids never drift, audit S-010)
 *  - the default is the **embedded ollama** service of the compose scaffold,
 *    addressed at runtime by its service name (`http://ollama:11434/v1`)
 *  - `baseURL` is emitted only when the provider needs one (official packages
 *    such as Gemini carry their own endpoint)
 *  - a key is referenced **only by the name of its environment variable**
 *    (`"apiKey": "{env:NAME}"`, the official opencode interpolation) — the
 *    value never appears here (Secrets rule, BYOK ACADEMY-7)
 */
object OpenCodeConfigGenerator {

    /** Renders the learner `opencode.json`. */
    fun render(config: OpenCodeConfig): String {
        val spec = resolveSpec(config)

        val options = mutableListOf<String>()
        spec.baseUrl?.let { options += "        \"baseURL\": \"$it\"" }
        spec.apiKeyEnvVar?.let { options += "        \"apiKey\": \"{env:$it}\"" }

        val lines = mutableListOf(
            "{",
            "  \"\$schema\": \"https://opencode.ai/config.json\",",
            "  \"model\": \"${spec.id}/${config.model}\",",
            "  \"provider\": {",
            "    \"${spec.id}\": {",
            "      \"npm\": \"${spec.npm}\",",
            "      \"name\": \"${spec.displayName}\",",
            "      \"options\": {",
        )
        lines += options.joinToString(",\n")
        lines += listOf(
            "      },",
            "      \"models\": {",
            "        \"${config.model}\": {",
            "          \"name\": \"${config.model}\"",
            "        }",
            "      }",
            "    }",
            "  },",
            "  \"instructions\": [",
            "    \"AGENTS.md\"",
            "  ]",
            "}",
        )
        return lines.joinToString("\n", postfix = "\n")
    }

    /**
     * Resolves the concrete provider description: the catalog entry, refined by
     * the extension.
     *
     * - `baseUrl`: supplied by the extension for the providers the learner
     *   addresses himself ([LlmProviderKind.OLLAMA_LOCAL], [LlmProviderKind.CUSTOM]);
     *   otherwise the catalog endpoint (or none, for official packages such as
     *   Gemini that carry their own).
     * - `apiKeyEnvVar`: the extension name wins when set, else the documented
     *   per-provider default from [ByokProviderCatalog] (D-ACADEMY-7-6).
     */
    private fun resolveSpec(config: OpenCodeConfig): ByokProviderSpec {
        val base = ByokProviderCatalog.specFor(config.provider)
        val baseUrl = when (config.provider) {
            LlmProviderKind.OLLAMA_LOCAL,
            LlmProviderKind.CUSTOM,
            -> config.providerUrl
            LlmProviderKind.OLLAMA_CLOUD,
            LlmProviderKind.GEMINI,
            LlmProviderKind.HUGGINGFACE,
            -> base.baseUrl
        }
        return base.copy(
            baseUrl = baseUrl,
            apiKeyEnvVar = config.apiKeyEnvVar ?: base.apiKeyEnvVar,
        )
    }
}

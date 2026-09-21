package education.cccp.academy.opencode

/**
 * Pure `opencode.json` renderer (ACADEMY-5-1) — a deterministic function from
 * [OpenCodeConfig] to the learner agent configuration. No I/O, no Gradle types,
 * and **no serialization library**: the JSON is built by explicit literal
 * concatenation (D-ACADEMY-5-3), keeping the plugin dependency-free
 * (D-ACADEMY-6-8).
 *
 * Contract:
 *  - the config declares the opencode `$schema` for editor validation
 *  - the default provider is the **embedded ollama** service of the compose
 *    scaffold, addressed at runtime by its service name (`http://ollama:11434/v1`)
 *  - the model is the learner's choice (`gpt-oss:120b-cloud` by default)
 *  - `instructions` points at the generated learner guide (`AGENTS.md`)
 *  - **no credential is ever embedded** (Secrets rule): the learner's LLM key
 *    is the concern of ACADEMY-7 (BYOK) and flows through the environment
 */
object OpenCodeConfigGenerator {

    /** Provider key used in the rendered config (documented, stable). */
    const val PROVIDER_KEY: String = "ollama"

    /** Renders the learner `opencode.json`. */
    fun render(config: OpenCodeConfig): String {
        val lines = listOf(
            "{",
            "  \"\$schema\": \"https://opencode.ai/config.json\",",
            "  \"model\": \"$PROVIDER_KEY/${config.model}\",",
            "  \"provider\": {",
            "    \"$PROVIDER_KEY\": {",
            "      \"npm\": \"@ai-sdk/openai-compatible\",",
            "      \"name\": \"Academy Ollama (embedded)\",",
            "      \"options\": {",
            "        \"baseURL\": \"${config.providerUrl}\"",
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
}

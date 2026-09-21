package education.cccp.academy.byok

import contracts.runtime.LlmProviderKind

/**
 * Pure mapping from the N0 contract [LlmProviderKind] to the opencode provider
 * description ([ByokProviderSpec]) — the single source of truth consumed by
 * [education.cccp.academy.opencode.OpenCodeConfigGenerator].
 *
 * **Every entry is verified** against models.dev (`api.json`) and the opencode
 * provider documentation (audit S-010) — never invented. `OLLAMA_LOCAL` and
 * `CUSTOM` carry no endpoint here: it comes from the extension at render time
 * (`ByokProviderSpec.copy`), and `CUSTOM` requires it (D-ACADEMY-7-7).
 */
object ByokProviderCatalog {

    /** The embedded local runtime — the ACADEMY-5 default, no key. */
    val OLLAMA_LOCAL: ByokProviderSpec = ByokProviderSpec(
        id = "ollama",
        npm = "@ai-sdk/openai-compatible",
        displayName = "Academy Ollama (embedded)",
    )

    /** The mapping keyed by the N0 provider kind. */
    val specs: Map<LlmProviderKind, ByokProviderSpec> = mapOf(
        LlmProviderKind.OLLAMA_LOCAL to OLLAMA_LOCAL,
        LlmProviderKind.OLLAMA_CLOUD to ByokProviderSpec(
            id = "ollama-cloud",
            npm = "@ai-sdk/openai-compatible",
            displayName = "Ollama Cloud",
            baseUrl = "https://ollama.com/v1",
            apiKeyEnvVar = "OLLAMA_API_KEY",
        ),
        LlmProviderKind.GEMINI to ByokProviderSpec(
            id = "google",
            npm = "@ai-sdk/google",
            displayName = "Google Gemini",
            apiKeyEnvVar = "GEMINI_API_KEY",
        ),
        LlmProviderKind.HUGGINGFACE to ByokProviderSpec(
            id = "huggingface",
            npm = "@ai-sdk/openai-compatible",
            displayName = "Hugging Face",
            baseUrl = "https://router.huggingface.co/v1",
            apiKeyEnvVar = "HF_TOKEN",
        ),
        LlmProviderKind.CUSTOM to ByokProviderSpec(
            id = "custom",
            npm = "@ai-sdk/openai-compatible",
            displayName = "Custom provider",
        ),
    )

    /** The provider description for [kind] (total: every kind is mapped). */
    fun specFor(kind: LlmProviderKind): ByokProviderSpec =
        requireNotNull(specs[kind]) { "no opencode provider mapped for $kind" }
}

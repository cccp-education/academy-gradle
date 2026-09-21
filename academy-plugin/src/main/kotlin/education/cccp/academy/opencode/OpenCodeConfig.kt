package education.cccp.academy.opencode

import contracts.runtime.LlmProviderKind

/**
 * Immutable, resolved configuration of the learner's opencode exposure
 * (ACADEMY-5-1, extended by ACADEMY-7-1) — the input of the pure
 * [OpenCodeConfigGenerator], [LearnerGuideGenerator] and
 * [WorkspaceDockerfileGenerator].
 *
 * Built by the Gradle task from the lazy `Property`s of the `academyInstaller`
 * extension, so the domain stays free of Gradle types and unit-testable in
 * isolation (DDD pattern [education.cccp.academy.installer.InstallerPlatform]).
 *
 * Credentials are deliberately NOT modelled here: [apiKeyEnvVar] is the **name**
 * of the environment variable the agent reads at runtime — this data class
 * describes where the agent points and which model it uses, never a value
 * (BYOK, ACADEMY-7; Secrets rule).
 *
 * @param providerUrl OpenAI-compatible base URL (the embedded ollama service of
 *   the compose scaffold, `http://ollama:11434/v1`, is the local default)
 * @param model model id served by the provider (never blank)
 * @param provider N0 provider kind (default `OLLAMA_LOCAL` = ACADEMY-5 behaviour)
 * @param apiKeyEnvVar name of the environment variable holding the key (null =
 *   no key, embedded runtime; required for external providers by
 *   [contracts.runtime.ByokLlmConfig])
 */
data class OpenCodeConfig(
    val providerUrl: String,
    val model: String,
    val provider: LlmProviderKind = LlmProviderKind.OLLAMA_LOCAL,
    val apiKeyEnvVar: String? = null,
) {
    init {
        require(providerUrl.isNotBlank()) { "providerUrl must not be blank" }
        require(model.isNotBlank()) { "model must not be blank" }
        require(apiKeyEnvVar == null || apiKeyEnvVar.isNotBlank()) {
            "apiKeyEnvVar must not be blank when present"
        }
        require(
            provider != LlmProviderKind.CUSTOM || providerUrl.isNotBlank(),
        ) {
            "CUSTOM provider requires an explicit base url"
        }
        require(
            !isExternalProvider(provider) || !apiKeyEnvVar.isNullOrBlank(),
        ) {
            "$provider provider requires an apiKeyEnvVar name"
        }
    }

    private companion object {
        /**
         * Providers whose key is resolved by [contracts.runtime.ByokLlmConfig]:
         * external providers require the env var name, `OLLAMA_CLOUD` has a
         * documented default ([education.cccp.academy.byok.ByokProviderCatalog]).
         */
        fun isExternalProvider(provider: LlmProviderKind): Boolean = when (provider) {
            LlmProviderKind.CUSTOM -> true
            LlmProviderKind.OLLAMA_LOCAL,
            LlmProviderKind.OLLAMA_CLOUD,
            LlmProviderKind.GEMINI,
            LlmProviderKind.HUGGINGFACE,
            -> false
        }
    }
}

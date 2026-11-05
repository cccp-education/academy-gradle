package education.cccp.academy.workstation

import contracts.runtime.LlmProviderKind
import education.cccp.academy.byok.ByokProviderCatalog

/**
 * The effective LLM configuration of a learner workstation (ACADEMY-16 US-4,
 * D-LWA-5), derived from the selected [WorkstationProfile].
 *
 * The profile is the learner-facing choice; the resolver turns it into the
 * concrete opencode provider and the compose decision:
 *  - `LOCAL_OLLAMA` keeps the embedded runtime (no key, heavy machine);
 *  - `CLOUD_BYOK` removes the local runtime and defaults to **Ollama Cloud**
 *    when the configured provider is still the local one, so the generated
 *    `opencode.json` never points to a service that is not in the compose.
 *
 * @property profile the selected profile
 * @property provider the effective N0 provider kind
 * @property providerUrl the effective OpenAI-compatible base URL (used by the
 *   generator only for `OLLAMA_LOCAL`/`CUSTOM`, which carry their own endpoint)
 * @property apiKeyEnvVar the environment variable **name**, never a value
 * @property ollamaEnabled whether the compose scaffold embeds the local runtime
 */
data class ResolvedWorkstationLlm(
    val profile: WorkstationProfile,
    val provider: LlmProviderKind,
    val providerUrl: String,
    val apiKeyEnvVar: String?,
    val ollamaEnabled: Boolean,
) {
    init {
        require(providerUrl.isNotBlank()) { "providerUrl must not be blank" }
        require(apiKeyEnvVar == null || apiKeyEnvVar.isNotBlank()) {
            "apiKeyEnvVar must not be blank when present"
        }
    }
}

/**
 * Pure resolution of the workstation LLM profile (ACADEMY-16 US-4).
 *
 * The resolver never invents a credential or a model id: it either keeps the
 * configured provider or falls back to a **verified** catalog entry
 * (`OLLAMA_CLOUD`) when the cloud profile would otherwise point at the absent
 * local runtime. The `url` is only meaningful for `OLLAMA_LOCAL`/`CUSTOM`;
 * official packages (Gemini, Hugging Face) carry their own endpoint.
 */
object WorkstationProfileResolver {
    /** The embedded-runtime endpoint (ACADEMY-5 default). */
    const val LOCAL_OLLAMA_URL: String = "http://ollama:11434/v1"

    fun resolve(
        profile: WorkstationProfile,
        configuredProvider: LlmProviderKind,
        configuredProviderUrl: String,
        configuredApiKeyEnvVar: String?,
    ): ResolvedWorkstationLlm =
        when (profile) {
            WorkstationProfile.LOCAL_OLLAMA ->
                ResolvedWorkstationLlm(
                    profile = profile,
                    provider = LlmProviderKind.OLLAMA_LOCAL,
                    providerUrl = configuredProviderUrl.ifBlank { LOCAL_OLLAMA_URL },
                    apiKeyEnvVar = null,
                    ollamaEnabled = true,
                )
            WorkstationProfile.CLOUD_BYOK ->
                if (configuredProvider != LlmProviderKind.OLLAMA_LOCAL) {
                    ResolvedWorkstationLlm(
                        profile = profile,
                        provider = configuredProvider,
                        providerUrl =
                            configuredProviderUrl.ifBlank {
                                ByokProviderCatalog.specFor(configuredProvider).baseUrl ?: LOCAL_OLLAMA_URL
                            },
                        apiKeyEnvVar = configuredApiKeyEnvVar?.takeIf { it.isNotBlank() },
                        ollamaEnabled = false,
                    )
                } else {
                    // A cloud profile with the local provider would point at a
                    // service the compose no longer starts: resolve to the
                    // verified cloud provider instead (D-LWA-5).
                    val cloud = ByokProviderCatalog.specFor(LlmProviderKind.OLLAMA_CLOUD)
                    ResolvedWorkstationLlm(
                        profile = profile,
                        provider = LlmProviderKind.OLLAMA_CLOUD,
                        providerUrl = requireNotNull(cloud.baseUrl) { "OLLAMA_CLOUD must declare a baseUrl" },
                        apiKeyEnvVar = cloud.apiKeyEnvVar,
                        ollamaEnabled = false,
                    )
                }
        }
}

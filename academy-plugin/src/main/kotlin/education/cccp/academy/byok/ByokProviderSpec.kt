package education.cccp.academy.byok

/**
 * Immutable description of an opencode provider (ACADEMY-7-1) — the pure input
 * of [education.cccp.academy.opencode.OpenCodeConfigGenerator].
 *
 * Built by the pure [ByokProviderCatalog] from the N0 contract
 * [contracts.runtime.LlmProviderKind], possibly refined by the extension
 * (`baseUrl` for `OLLAMA_LOCAL`/`CUSTOM`, `apiKeyEnvVar` for `CUSTOM`).
 * Credentials are deliberately never modelled: [apiKeyEnvVar] is the **name**
 * of the environment variable opencode reads at runtime (`{env:NAME}`), never
 * a value (Secrets rule, D-ACADEMY-7-2).
 *
 * Every id/npm/endpoint is verified against models.dev and the opencode
 * provider documentation (audit S-010), never invented — cf. the S-007
 * `moodle:4.5` lesson.
 *
 * @property id opencode provider key, the prefix of `model` (never blank)
 * @property npm AI-SDK package opencode loads the provider with (never blank)
 * @property displayName human-readable provider name shown by opencode (never blank)
 * @property baseUrl OpenAI-compatible endpoint, when the provider is not the
 *   official package endpoint (null means "use the package default")
 * @property apiKeyEnvVar name of the environment variable holding the key
 *   (null means "no key: embedded local runtime")
 */
data class ByokProviderSpec(
    val id: String,
    val npm: String,
    val displayName: String,
    val baseUrl: String? = null,
    val apiKeyEnvVar: String? = null,
) {
    init {
        require(id.isNotBlank()) { "provider id must not be blank" }
        require(npm.isNotBlank()) { "provider npm must not be blank" }
        require(displayName.isNotBlank()) { "provider displayName must not be blank" }
        require(baseUrl == null || baseUrl.isNotBlank()) { "baseUrl must not be blank when present" }
        require(apiKeyEnvVar == null || apiKeyEnvVar.isNotBlank()) { "apiKeyEnvVar must not be blank when present" }
    }
}

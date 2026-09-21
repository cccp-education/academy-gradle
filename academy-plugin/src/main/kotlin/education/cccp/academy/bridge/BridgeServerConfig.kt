package education.cccp.academy.bridge

/**
 * Immutable, resolved configuration of the webhook bridge (ACADEMY-8-1) — the
 * pure input of the Ktor module. Built by the Gradle task from the lazy
 * `Property`s of the `academyInstaller` extension, so the server layer stays
 * free of Gradle types and testable through `testApplication`.
 *
 * The bridge is **local-only by design** (`AGENT.adoc`: no VPS, single-user
 * build artefact) and never carries a credential: [providerId]/[model] are
 * descriptive, the key stays in the environment (BYOK, ACADEMY-7).
 *
 * @property host bind address (local only in practice)
 * @property port bind port
 * @property providerId resolved opencode provider id (never blank)
 * @property model resolved model id (never blank)
 */
data class BridgeServerConfig(
    val host: String,
    val port: Int,
    val providerId: String,
    val model: String,
) {
    init {
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65535) { "port must be in 1..65535, got: $port" }
        require(providerId.isNotBlank()) { "providerId must not be blank" }
        require(model.isNotBlank()) { "model must not be blank" }
    }
}

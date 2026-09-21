package education.cccp.academy.opencode

/**
 * Optional bridge information rendered into the learner guide (ACADEMY-8-2) —
 * present only when the webhook bridge is enabled, so a disabled bridge is
 * never documented (D-ACADEMY-8-9).
 *
 * @property host bind address shown to the learner
 * @property port bind port shown to the learner
 */
data class BridgeGuide(
    val host: String,
    val port: Int,
) {
    init {
        require(host.isNotBlank()) { "bridge host must not be blank" }
        require(port in 1..65535) { "bridge port must be in 1..65535, got: $port" }
    }
}

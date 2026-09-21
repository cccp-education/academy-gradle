package education.cccp.academy.bridge

/**
 * The bridge's HTTP surface, declared once (ACADEMY-8-1) — the single source of
 * truth consumed by the Ktor module, the learner guide and the tests, so a
 * documented route can never drift from a served route (pattern
 * [education.cccp.academy.opencode.OpenCodeCatalog]).
 *
 * @property method HTTP method (`GET`/`POST`)
 * @property path route path, absolute
 * @property description human-readable purpose (never blank)
 */
data class BridgeRoute(
    val method: String,
    val path: String,
    val description: String,
) {
    init {
        require(method.isNotBlank()) { "route method must not be blank" }
        require(path.isNotBlank()) { "route path must not be blank" }
        require(description.isNotBlank()) { "route $path must be documented" }
    }
}

object BridgeRoutes {

    /** The health probe — reports the resolved LLM provider and model. */
    val HEALTH: BridgeRoute = BridgeRoute(
        method = "GET",
        path = "/health",
        description = "Health probe reporting the resolved provider and model",
    )

    /** The inbound LMS event sink — translates an event into a session prompt. */
    val MOODLE_EVENT: BridgeRoute = BridgeRoute(
        method = "POST",
        path = "/events/moodle",
        description = "Accepts a formation event and acknowledges it with a session response",
    )

    /** The inbound session sink — accepts an N0 session prompt directly. */
    val SESSION: BridgeRoute = BridgeRoute(
        method = "POST",
        path = "/session",
        description = "Accepts an N0 session prompt and acknowledges it",
    )

    /** Every route, in declaration order (deterministic). */
    val routes: List<BridgeRoute> = listOf(HEALTH, MOODLE_EVENT, SESSION)

    /** The method of [path], or `null` when the route is unknown. */
    fun methodFor(path: String): String? = routes.firstOrNull { it.path == path }?.method
}

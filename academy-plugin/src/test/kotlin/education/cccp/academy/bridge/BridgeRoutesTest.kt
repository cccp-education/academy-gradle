package education.cccp.academy.bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-8-1 — the bridge route table is the single source of truth consumed
 * by the Ktor module, the learner guide and the tests, so a documented route
 * can never drift from a served route (pattern `OpenCodeCatalog`).
 */
class BridgeRoutesTest {

    @Test
    fun `declares the health probe, the moodle event sink and the session sink`() {
        val paths = BridgeRoutes.routes.map { it.path }

        assertTrue(paths.contains("/health"), "the bridge must expose a health probe")
        assertTrue(paths.contains("/events/moodle"), "the bridge must expose the moodle event sink")
        assertTrue(paths.contains("/session"), "the bridge must expose the session prompt sink")
    }

    @Test
    fun `every route carries an http method and a description`() {
        BridgeRoutes.routes.forEach { route ->
            assertTrue(route.method.isNotBlank(), "${route.path} must declare a method")
            assertTrue(route.description.isNotBlank(), "${route.path} must be documented")
        }
    }

    @Test
    fun `the health probe is a GET and the sinks are POSTs`() {
        assertEquals("GET", BridgeRoutes.methodFor("/health"))
        assertEquals("POST", BridgeRoutes.methodFor("/events/moodle"))
        assertEquals("POST", BridgeRoutes.methodFor("/session"))
    }

    @Test
    fun `the route table is the single source of truth`() {
        assertEquals(BridgeRoutes.routes.size, BridgeRoutes.routes.distinctBy { it.path }.size)
        assertTrue(BridgeRoutes.methodFor("/unknown") == null, "an unknown route has no method")
    }
}

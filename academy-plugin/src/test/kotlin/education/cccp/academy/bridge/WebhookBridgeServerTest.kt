package education.cccp.academy.bridge

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-8-1 — the Ktor server, exercised through `testApplication`
 * (D-ACADEMY-8-10): real HTTP routing, JSON negotiation and status codes, but
 * **no real port** — so no network flake and no port collision in CI. The real
 * port is proven separately by the dogfooding (pattern S-007).
 */
class WebhookBridgeServerTest {

    private val config = BridgeServerConfig(
        host = "127.0.0.1",
        port = 8765,
        providerId = "ollama",
        model = "gpt-oss:120b-cloud",
    )

    @Test
    fun `the health probe reports the resolved provider and model`() = testApplication {
        application { installBridge(config) }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"up\""), "the probe must report up: $body")
        assertTrue(body.contains("\"provider\":\"ollama\""), "the probe must report the provider: $body")
        assertTrue(body.contains("\"model\":\"gpt-oss:120b-cloud\""), "the probe must report the model: $body")
    }

    @Test
    fun `the moodle event sink accepts an event and acknowledges it`() = testApplication {
        application { installBridge(config) }

        val response = client.post("/events/moodle") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "eventName": "course_module_completed",
                  "courseId": "academy-seed",
                  "userId": "learner-1",
                  "payload": { "module": "M01" }
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"COMPLETED\""), "the sink must acknowledge the event: $body")
        assertTrue(body.contains("course_module_completed"), "the acknowledgement must echo the event: $body")
    }

    @Test
    fun `the session sink accepts an N0 session prompt`() = testApplication {
        application { installBridge(config) }

        val response = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody("""{ "prompt": "explain module M01", "maxActions": 3 }""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"COMPLETED\""), "the session sink must acknowledge: $body")
    }

    @Test
    fun `a malformed event is rejected with a client error`() = testApplication {
        application { installBridge(config) }

        val response = client.post("/events/moodle") {
            contentType(ContentType.Application.Json)
            setBody("""{ "eventName": "" }""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, "a blank event name must be a client error")
    }

    @Test
    fun `a malformed json body is rejected without crashing the server`() = testApplication {
        application { installBridge(config) }

        val response = client.post("/events/moodle") {
            contentType(ContentType.Application.Json)
            setBody("not json at all")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `an unknown route is a not found`() = testApplication {
        application { installBridge(config) }

        val response = client.get("/unknown")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `the bridge never embeds a credential in its responses`() = testApplication {
        application { installBridge(config) }

        val responses = listOf(
            client.get("/health").bodyAsText(),
            client.post("/session") {
                contentType(ContentType.Application.Json)
                setBody("""{ "prompt": "hello" }""")
            }.bodyAsText(),
        )

        responses.forEach { body ->
            listOf("sk-", "ghp_", "gho_", "AIza", "{env:").forEach { leaked ->
                assertTrue(!body.contains(leaked), "the bridge must never leak $leaked: $body")
            }
        }
    }
}

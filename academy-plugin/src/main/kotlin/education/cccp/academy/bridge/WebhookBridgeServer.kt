package education.cccp.academy.bridge

import contracts.session.SessionPrompt
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Health payload served by [BridgeRoutes.HEALTH] (ACADEMY-8-1). */
@Serializable
data class BridgeHealth(
    val status: String,
    val provider: String,
    val model: String,
)

/**
 * The local webhook bridge (ACADEMY-8-1) — a Ktor module exposing the
 * [BridgeRoutes] surface. It **routes and acknowledges**, it never thinks
 * (D-ACADEMY-8-6): no LLM call, no Gradle task, no file write. The agent stays
 * opencode; the bridge is the transport an external producer (Moodle, browser,
 * `curl`) pushes into.
 *
 * The N0 contracts are never serialized directly: the wire schemas live in
 * [BridgeWire] (anti-corruption layer), so the shared contracts stay pure
 * (S-011: `contracts.session.*` carry no `@Serializable`).
 *
 * The same module is installed by [startBridge] (production task) and by the
 * tests through `testApplication` (D-ACADEMY-8-10) — one code path, no
 * test-only duplicate.
 */
fun Application.installBridge(config: BridgeServerConfig) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    routing {
        get(BridgeRoutes.HEALTH.path) {
            call.respond(
                BridgeHealth(
                    status = "up",
                    provider = config.providerId,
                    model = config.model,
                ),
            )
        }

        post(BridgeRoutes.MOODLE_EVENT.path) {
            val event = try {
                call.receive<MoodleEvent>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid event", e))
                return@post
            }

            val prompt = BridgeEventTranslator.toSessionPrompt(event)
            call.respond(acknowledge(prompt, "event ${event.eventName}", event.eventName))
        }

        post(BridgeRoutes.SESSION.path) {
            val prompt = try {
                call.receive<SessionRequest>().toSessionPrompt()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, badRequest("Invalid session prompt", e))
                return@post
            }

            call.respond(acknowledge(prompt, "session prompt"))
        }
    }
}

/**
 * Starts the bridge on the configured host/port and blocks until the process is
 * interrupted (daemon mode, pattern codebase `SessionProtocolDaemonTask`).
 */
fun startBridge(config: BridgeServerConfig) {
    embeddedServer(Netty, host = config.host, port = config.port) {
        installBridge(config)
    }.start(wait = true)
}

/** Acknowledges a routed prompt without executing it (D-ACADEMY-8-6). */
private fun acknowledge(
    prompt: SessionPrompt,
    what: String,
    eventName: String? = null,
): BridgeAck = BridgeAck(
    sessionId = prompt.sessionId.toString(),
    output = "accepted $what (bridge routes, the agent executes)",
    status = "COMPLETED",
    eventName = eventName,
)

private fun badRequest(prefix: String, cause: Exception): BridgeAck = BridgeAck(
    sessionId = java.util.UUID.randomUUID().toString(),
    output = "$prefix: ${cause.message}",
    status = "ERROR",
)

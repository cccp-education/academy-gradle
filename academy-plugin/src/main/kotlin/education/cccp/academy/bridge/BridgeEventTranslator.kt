package education.cccp.academy.bridge

import contracts.session.AgentContext
import contracts.session.SessionPrompt
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Pure translator from an inbound [MoodleEvent] to the N0 [SessionPrompt]
 * (ACADEMY-8-1) — deterministic, no I/O, no Ktor, no LLM (D-ACADEMY-8-4/6).
 *
 * Determinism matters: the `sessionId` is derived from the event identity, not
 * from `randomUUID`, so replaying the same event always yields the same session
 * (idempotent, testable, safe for at-least-once delivery).
 */
object BridgeEventTranslator {

    /**
     * Translates [event] into the session prompt the formation runtime consumes.
     *
     * The prompt is a deterministic sentence built from the event vocabulary;
     * the payload is carried into [AgentContext.eagerRules] — the N0 contract
     * has no free-form map, so the key/value detail rides the eager-rules slot
     * (never invented context when the payload is empty).
     */
    fun toSessionPrompt(event: MoodleEvent): SessionPrompt = SessionPrompt(
        sessionId = stableSessionId(event),
        prompt = buildPrompt(event),
        context = AgentContext(eagerRules = renderPayload(event.payload)),
    )

    private fun buildPrompt(event: MoodleEvent): String =
        "Formation event ${event.eventName} for learner ${event.userId} " +
            "on course ${event.courseId}."

    /** Stable UUID (version 5, name-based) over the event identity — rejouable. */
    private fun stableSessionId(event: MoodleEvent): UUID {
        val name = "${event.eventName}|${event.courseId}|${event.userId}"
        return UUID.nameUUIDFromBytes(name.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Renders the opaque payload as `key=value` lines, sorted for determinism.
     * An empty payload renders as an empty string — the bridge never invents
     * context the producer did not send.
     */
    private fun renderPayload(payload: Map<String, String>): String =
        payload.entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value}" }
}

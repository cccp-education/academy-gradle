package education.cccp.academy.bridge

import contracts.session.SessionPrompt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-8-1 — the pure translator from an inbound [MoodleEvent] to the N0
 * [SessionPrompt]. Deterministic, no I/O, no Ktor: the translation is unit
 * testable in isolation and the bridge stays a dumb router
 * (D-ACADEMY-8-4/6).
 */
class BridgeEventTranslatorTest {

    @Test
    fun `builds a session prompt from the event identity`() {
        val prompt = BridgeEventTranslator.toSessionPrompt(
            MoodleEvent(
                eventName = "course_module_completed",
                courseId = "academy-seed",
                userId = "learner-1",
            ),
        )

        assertTrue(prompt.prompt.isNotBlank())
        assertTrue(prompt.prompt.contains("course_module_completed"), "the prompt must carry the event name")
        assertTrue(prompt.prompt.contains("academy-seed"), "the prompt must carry the course")
        assertTrue(prompt.prompt.contains("learner-1"), "the prompt must carry the learner")
    }

    @Test
    fun `the translation is deterministic and replayable`() {
        val event = MoodleEvent(eventName = "quiz_submitted", courseId = "academy-seed", userId = "learner-1")

        val first = BridgeEventTranslator.toSessionPrompt(event)
        val second = BridgeEventTranslator.toSessionPrompt(event)

        assertEquals(first.sessionId, second.sessionId, "the session id must be stable, not random (replayable bridge)")
        assertEquals(first.prompt, second.prompt)
    }

    @Test
    fun `the payload is carried into the agent context`() {
        val prompt = BridgeEventTranslator.toSessionPrompt(
            MoodleEvent(
                eventName = "course_module_completed",
                courseId = "academy-seed",
                userId = "learner-1",
                payload = mapOf("module" to "M01", "score" to "0.8"),
            ),
        )

        val context = checkNotNull(prompt.context) { "the payload must be carried into the agent context" }
        assertTrue(context.eagerRules.contains("module"), "the payload keys must be carried")
        assertTrue(context.eagerRules.contains("M01"), "the payload values must be carried")
        assertTrue(context.eagerRules.contains("score"))
    }

    @Test
    fun `an empty payload yields no invented context`() {
        val prompt = BridgeEventTranslator.toSessionPrompt(
            MoodleEvent(eventName = "course_viewed", courseId = "academy-seed", userId = "learner-1"),
        )

        assertEquals("", prompt.context?.eagerRules, "an empty payload must not invent context")
    }

    @Test
    fun `the different events yield different stable session ids`() {
        val a = BridgeEventTranslator.toSessionPrompt(
            MoodleEvent(eventName = "course_viewed", courseId = "c1", userId = "u1"),
        )
        val b = BridgeEventTranslator.toSessionPrompt(
            MoodleEvent(eventName = "quiz_submitted", courseId = "c1", userId = "u1"),
        )

        assertTrue(a.sessionId != b.sessionId, "distinct events must map to distinct sessions")
    }

    @Test
    fun `the prompt is a SessionPrompt contract value not a string blob`() {
        val prompt: SessionPrompt = BridgeEventTranslator.toSessionPrompt(
            MoodleEvent(eventName = "course_viewed", courseId = "c", userId = "u"),
        )

        assertTrue(prompt.maxActions > 0)
    }
}

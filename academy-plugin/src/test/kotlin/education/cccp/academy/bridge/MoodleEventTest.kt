package education.cccp.academy.bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * ACADEMY-8-1 — the inbound event schema is **academy's own** (D-ACADEMY-8-5):
 * never a claimed Moodle format (no third-party plugin payload is frozen). The
 * name `MoodleEvent` is business vocabulary — what the learner's LMS *means* —
 * not an assertion about an official wire format.
 */
class MoodleEventTest {

    @Test
    fun `holds the event identity and an opaque payload`() {
        val event = MoodleEvent(
            eventName = "course_module_completed",
            courseId = "academy-seed",
            userId = "learner-1",
            payload = mapOf("module" to "M01"),
        )

        assertEquals("course_module_completed", event.eventName)
        assertEquals("academy-seed", event.courseId)
        assertEquals("learner-1", event.userId)
        assertEquals(mapOf("module" to "M01"), event.payload)
    }

    @Test
    fun `the payload is optional and empty by default`() {
        val event = event()

        assertTrue(event.payload.isEmpty(), "a bare event must carry an empty payload")
    }

    @Test
    fun `rejects a blank identity`() {
        assertFailsWith<IllegalArgumentException> { event(eventName = "  ") }
        assertFailsWith<IllegalArgumentException> { event(courseId = "") }
        assertFailsWith<IllegalArgumentException> { event(userId = "  ") }
    }

    private fun event(
        eventName: String = "course_module_completed",
        courseId: String = "academy-seed",
        userId: String = "learner-1",
        payload: Map<String, String> = emptyMap(),
    ) = MoodleEvent(
        eventName = eventName,
        courseId = courseId,
        userId = userId,
        payload = payload,
    )
}

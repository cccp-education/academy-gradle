package education.cccp.academy.bridge

import kotlinx.serialization.Serializable

/**
 * Inbound formation event, as academy models it (ACADEMY-8-1).
 *
 * **This schema is academy's own** (D-ACADEMY-8-5): Moodle's core has no
 * outgoing webhook, and any third-party plugin payload is neither shipped with
 * the scaffold image nor documented in a verifiable primary source (audit
 * S-011). Freezing an unverified format would reproduce the S-007 `moodle:4.5`
 * bug class. The name is therefore *business vocabulary* — what the learner's
 * LMS means — not a claim about an official wire format. Any producer (Moodle
 * plugin, script, `curl`, a test) posts exactly this shape.
 *
 * `payload` is deliberately opaque: the bridge routes and acknowledges, it
 * does not interpret the LMS (D-ACADEMY-8-6).
 *
 * @property eventName formation event name, e.g. `course_module_completed` (never blank)
 * @property courseId course identity in the LMS (never blank)
 * @property userId learner identity (never blank)
 * @property payload free-form key/value detail, carried verbatim (may be empty)
 */
@Serializable
data class MoodleEvent(
    val eventName: String,
    val courseId: String,
    val userId: String,
    val payload: Map<String, String> = emptyMap(),
) {
    init {
        require(eventName.isNotBlank()) { "eventName must not be blank" }
        require(courseId.isNotBlank()) { "courseId must not be blank" }
        require(userId.isNotBlank()) { "userId must not be blank" }
    }
}

package education.cccp.academy.a11y

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** ACADEMY-15 — the accessibility standard the report refers to. */
@Serializable
enum class A11yStandard {
    WCAG_21_AA,
    RGAA_41,
}

/** ACADEMY-15 — the deliverable family a finding belongs to. */
@Serializable
enum class A11yDeliverable {
    HTML,
    EPUB,
    PDF,
    VIDEO,
    MOODLE,
}

/** ACADEMY-15 — the finding severity. Only `ERROR` can fail the strict gate. */
@Serializable
enum class A11ySeverity {
    ERROR,
    WARNING,
    INFO,
}

/** ACADEMY-15 — stable rule identifiers, shared with producer reports. */
object A11yRules {
    const val VIDEO_SUBTITLES_MISSING = "video-subtitles-missing"
    const val VIDEO_TRANSCRIPT_MISSING = "video-transcript-missing"
    const val AUDIO_DESCRIPTION_MISSING = "audio-description-missing"
    const val PRODUCER_REPORT_MISSING = "producer-report-missing"
    const val PRODUCER_REPORT_INCOMPLETE = "producer-report-incomplete"
    const val PRODUCER_FINDING = "producer-finding"
    const val PDF_NOT_TAGGED = "pdf-not-tagged"
    const val MOODLE_LANG_MISSING = "moodle-lang-missing"
    const val MOODLE_IMAGE_ALT_MISSING = "moodle-image-alt-missing"
    const val MOODLE_FORM_CONTROL_LABEL_MISSING = "moodle-form-control-label-missing"
    const val MOODLE_TABLE_HEADER_MISSING = "moodle-table-header-missing"
    const val MOODLE_H1_MISSING = "moodle-h1-missing"
    const val MOODLE_PAGE_UNREACHABLE = "moodle-page-unreachable"
}

/**
 * ACADEMY-15 — one accessibility finding.
 *
 * `rule` is a stable string, not an enum, so a producer report can carry a rule
 * academy does not know yet without breaking the reader.
 */
@Serializable
data class A11yFinding(
    val deliverable: A11yDeliverable,
    val rule: String,
    val severity: A11ySeverity,
    val location: String,
    val message: String,
)

/** ACADEMY-15 — the overall verdict of one material audit. */
@Serializable
enum class A11yReadiness {
    VERIFIED,
    DEGRADED,
    MISSING,
}

/**
 * ACADEMY-15 — the material accessibility report, written next to the audit.
 *
 * `limits` is part of the deliverable: a report that does not declare what it
 * did not check is a false promise.
 */
@Serializable
data class MaterialA11yReport(
    val version: Int = 1,
    val standard: A11yStandard = A11yStandard.WCAG_21_AA,
    val readiness: A11yReadiness,
    val findings: List<A11yFinding> = emptyList(),
    val limits: List<String> = emptyList(),
) {
    companion object {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        fun toJson(report: MaterialA11yReport): String = json.encodeToString(report)

        fun fromJson(text: String): MaterialA11yReport = json.decodeFromString(text)
    }
}

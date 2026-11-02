package education.cccp.academy.a11y

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ACADEMY-15 / GBL-011-5 — one audited layer of the box and its verdict.
 *
 * `layer` is a stable name (`material`, `moodle`); a layer is present only when
 * its producer report was provided, so a missing layer is visible in the
 * acceptance instead of being silently absent.
 */
@Serializable
data class A11yLayerReport(
    val layer: String,
    val readiness: A11yReadiness,
    val findings: List<A11yFinding> = emptyList(),
    val limits: List<String> = emptyList(),
)

/**
 * ACADEMY-15 / GBL-011-5 — the box acceptance report.
 *
 * It aggregates the per-layer audits into one versioned contract consumed by the
 * pilot. The declared limits include the usage test with a concerned person:
 * a machine audit never replaces it, and no accessibility claim may be made
 * without it (GBL-011 acceptance rule).
 */
@Serializable
data class AcademyA11yReport(
    val version: Int = 1,
    val standard: A11yStandard = A11yStandard.WCAG_21_AA,
    val readiness: A11yReadiness,
    val layers: List<A11yLayerReport> = emptyList(),
    val limits: List<String> = emptyList(),
) {
    companion object {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        fun toJson(report: AcademyA11yReport): String = json.encodeToString(report)

        fun fromJson(text: String): AcademyA11yReport = json.decodeFromString(text)
    }
}

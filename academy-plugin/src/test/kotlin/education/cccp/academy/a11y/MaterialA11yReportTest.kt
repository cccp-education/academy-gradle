package education.cccp.academy.a11y

import kotlin.test.Test
import kotlin.test.assertEquals

/** ACADEMY-15 — the report is a versioned, round-trippable JSON contract. */
class MaterialA11yReportTest {
    @Test
    fun `a report round-trips through json`() {
        val report =
            MaterialA11yReport(
                readiness = A11yReadiness.DEGRADED,
                findings =
                    listOf(
                        A11yFinding(
                            deliverable = A11yDeliverable.VIDEO,
                            rule = A11yRules.VIDEO_SUBTITLES_MISSING,
                            severity = A11ySeverity.ERROR,
                            location = "slide-01.mp4",
                            message = "no subtitles",
                        ),
                    ),
                limits = listOf("PDF not PDF/UA"),
            )

        assertEquals(report, MaterialA11yReport.fromJson(MaterialA11yReport.toJson(report)))
    }
}

package education.cccp.academy.a11y

import kotlin.test.Test
import kotlin.test.assertEquals

/** ACADEMY-15 / GBL-011-5 — the acceptance report is a versioned JSON contract. */
class AcademyA11yReportTest {
    @Test
    fun `an acceptance report round-trips through json`() {
        val report =
            AcademyA11yReport(
                readiness = A11yReadiness.DEGRADED,
                layers =
                    listOf(
                        A11yLayerReport(
                            layer = "material",
                            readiness = A11yReadiness.VERIFIED,
                        ),
                        A11yLayerReport(
                            layer = "moodle",
                            readiness = A11yReadiness.DEGRADED,
                            findings =
                                listOf(
                                    A11yFinding(
                                        deliverable = A11yDeliverable.MOODLE,
                                        rule = A11yRules.MOODLE_LANG_MISSING,
                                        severity = A11ySeverity.ERROR,
                                        location = "http://localhost:8080/login/index.php",
                                        message = "no lang",
                                    ),
                                ),
                        ),
                    ),
                limits = listOf("usage test with a concerned person is required"),
            )

        assertEquals(report, AcademyA11yReport.fromJson(AcademyA11yReport.toJson(report)))
    }
}

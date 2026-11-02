package education.cccp.academy.a11y

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 (GBL-011-5) — the Moodle accessibility verdict.
 *
 * The audit aggregates the static findings of every observed page. An
 * unreachable page is a declared `WARNING`, never a crash: the box may not be
 * running when the audit is invoked, and the gate must say so.
 */
class MoodleA11yAuditorTest {
    private val accessible =
        MoodlePage(
            location = "http://localhost:8080/login/index.php",
            html =
                """
                <html lang="fr">
                  <body>
                    <h1>Connexion</h1>
                    <label for="username">Nom</label>
                    <input id="username" type="text">
                  </body>
                </html>
                """.trimIndent(),
        )

    @Test
    fun `no observed page is a missing report`() {
        val report = MoodleA11yAuditor.audit(emptyList())

        assertEquals(A11yReadiness.MISSING, report.readiness)
        assertTrue(report.limits.isNotEmpty())
    }

    @Test
    fun `an accessible page is verified`() {
        val report = MoodleA11yAuditor.audit(listOf(accessible))

        assertEquals(A11yReadiness.VERIFIED, report.readiness)
        assertTrue(report.findings.isEmpty())
    }

    @Test
    fun `a page finding degrades the report`() {
        val report = MoodleA11yAuditor.audit(listOf(accessible.copy(html = "<html><body>no lang</body></html>")))

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.findings.any { it.rule == A11yRules.MOODLE_LANG_MISSING })
    }

    @Test
    fun `an unreachable page is a warning and never a crash`() {
        val report =
            MoodleA11yAuditor.audit(
                listOf(MoodlePage(location = "http://localhost:8080/login/index.php", reachable = false)),
            )

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.findings.any { it.rule == A11yRules.MOODLE_PAGE_UNREACHABLE })
    }

    @Test
    fun `every page contributes to the verdict`() {
        val report = MoodleA11yAuditor.audit(listOf(accessible, accessible.copy(location = "http://localhost:8080/course/view.php", reachable = false)))

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertEquals(1, report.findings.size)
    }

    @Test
    fun `the report declares what it did not measure`() {
        val report = MoodleA11yAuditor.audit(listOf(accessible))

        assertTrue(report.limits.any { it.contains("Static HTML") })
        assertTrue(report.limits.any { it.contains("opencode") })
    }
}

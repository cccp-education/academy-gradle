package education.cccp.academy.a11y

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 — the material audit verdict.
 *
 * `MISSING` when there is no material, `DEGRADED` when a finding exists,
 * `VERIFIED` otherwise. A text deliverable without a producer report is a
 * warning, never a silent "verified".
 */
class MaterialA11yAuditorTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `absent material is missing, not a crash`() {
        val report = MaterialA11yAuditor.audit(null)

        assertEquals(A11yReadiness.MISSING, report.readiness)
        assertTrue(report.limits.isNotEmpty())
    }

    @Test
    fun `an empty material directory is missing`() {
        assertEquals(A11yReadiness.MISSING, MaterialA11yAuditor.audit(dir).readiness)
    }

    @Test
    fun `a video without companions degrades the report`() {
        File(dir, "slide-01.mp4").writeText("x")

        val report = MaterialA11yAuditor.audit(dir)

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.findings.any { it.rule == A11yRules.VIDEO_SUBTITLES_MISSING })
    }

    @Test
    fun `a text deliverable without producer report is unverifiable`() {
        File(dir, "cours.html").writeText("<html></html>")

        val report = MaterialA11yAuditor.audit(dir)

        assertTrue(report.findings.any { it.rule == A11yRules.PRODUCER_REPORT_MISSING })
    }

    @Test
    fun `a producer report is aggregated with its limits`() {
        File(dir, "cours.html").writeText("<html></html>")
        File(dir, "a11y-report.json").writeText(
            """
            {
              "deliverables": [
                {
                  "kind": "HTML",
                  "findings": [
                    { "rule": "html-lang-missing", "severity": "ERROR", "location": "cours.html", "message": "lang" }
                  ],
                  "limits": ["PDF not tagged"]
                }
              ]
            }
            """.trimIndent(),
        )

        val report = MaterialA11yAuditor.audit(dir)

        assertTrue(report.findings.any { it.rule == "html-lang-missing" && it.severity == A11ySeverity.ERROR })
        assertTrue(report.limits.contains("PDF not tagged"))
    }
}

package education.cccp.academy.a11y

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 — the producer report reader (GBL-011-3 consumer side).
 *
 * The producer (`document`) owns the checks; academy transports and aggregates
 * them. Two contracts matter here:
 *  - every report under the material directory contributes (a producer may write
 *    one report per artifact directory);
 *  - a report only verifies the deliverable kinds it declares — a silent report
 *    never verifies a text deliverable it does not cover.
 */
class ProducerReportInspectorTest {
    @TempDir
    lateinit var dir: File

    private fun file(
        relative: String,
        content: String = "x",
    ): File =
        File(dir, relative).apply {
            parentFile.mkdirs()
            writeText(content)
        }

    private fun report(
        relative: String,
        json: String,
    ): File = file(relative).apply { writeText(json) }

    private fun materialFiles(): List<File> = dir.walkTopDown().filter { it.isFile }.toList()

    @Test
    fun `a report covering html leaves a pdf deliverable unverified`() {
        file("cours.html", "<html lang=\"fr\"><h1>c</h1></html>")
        file("cours.pdf")
        report(
            "a11y-report.json",
            """
            { "deliverables": [ { "kind": "HTML", "findings": [], "limits": [] } ] }
            """.trimIndent(),
        )

        val inspection = ProducerReportInspector.inspect(dir, materialFiles())

        val incomplete = inspection.findings.filter { it.rule == A11yRules.PRODUCER_REPORT_INCOMPLETE }
        assertEquals(1, incomplete.size)
        assertEquals(A11yDeliverable.PDF, incomplete.single().deliverable)
        assertEquals("cours.pdf", incomplete.single().location)
        assertTrue(inspection.findings.none { it.rule == A11yRules.PRODUCER_REPORT_MISSING })
    }

    @Test
    fun `every report contributes findings and limits`() {
        file("cours.html", "<html lang=\"fr\"><h1>c</h1></html>")
        file("livre.epub")
        report(
            "html/a11y-report.json",
            """
            { "deliverables": [ { "kind": "HTML", "findings": [], "limits": ["html limit"] } ] }
            """.trimIndent(),
        )
        report(
            "epub/a11y-report.json",
            """
            {
              "deliverables": [
                {
                  "kind": "EPUB",
                  "findings": [
                    { "rule": "epub-lang-missing", "severity": "ERROR", "location": "livre.epub", "message": "lang" }
                  ],
                  "limits": ["epub limit"]
                }
              ]
            }
            """.trimIndent(),
        )

        val inspection = ProducerReportInspector.inspect(dir, materialFiles())

        assertTrue(inspection.findings.any { it.rule == "epub-lang-missing" && it.severity == A11ySeverity.ERROR })
        assertTrue(inspection.findings.none { it.rule == A11yRules.PRODUCER_REPORT_INCOMPLETE })
        assertTrue(inspection.limits.containsAll(listOf("html limit", "epub limit")))
    }

    @Test
    fun `a malformed report does not hide a valid one`() {
        file("cours.html", "<html lang=\"fr\"><h1>c</h1></html>")
        report("broken/a11y-report.json", "{ not json")
        report(
            "valid/a11y-report.json",
            """
            { "deliverables": [ { "kind": "HTML", "findings": [], "limits": [] } ] }
            """.trimIndent(),
        )

        val inspection = ProducerReportInspector.inspect(dir, materialFiles())

        assertTrue(inspection.findings.none { it.rule == A11yRules.PRODUCER_REPORT_MISSING })
        assertTrue(inspection.findings.none { it.rule == A11yRules.PRODUCER_REPORT_INCOMPLETE })
        assertTrue(inspection.limits.any { it.contains("a11y-report.json") && it.contains("Malformed") })
    }

    @Test
    fun `a report that is only malformed leaves the text deliverables unverifiable`() {
        file("cours.html", "<html lang=\"fr\"><h1>c</h1></html>")
        report("a11y-report.json", "{ not json")

        val inspection = ProducerReportInspector.inspect(dir, materialFiles())

        val missing = inspection.findings.filter { it.rule == A11yRules.PRODUCER_REPORT_MISSING }
        assertEquals(1, missing.size)
        assertTrue(missing.single().message.contains("Malformed"))
        assertTrue(inspection.limits.any { it.contains("Malformed") })
    }

    @Test
    fun `an unknown deliverable kind never verifies a text deliverable`() {
        file("cours.html", "<html lang=\"fr\"><h1>c</h1></html>")
        report(
            "a11y-report.json",
            """
            { "deliverables": [ { "kind": "FutureFormat", "findings": [], "limits": [] } ] }
            """.trimIndent(),
        )

        val inspection = ProducerReportInspector.inspect(dir, materialFiles())

        assertEquals(1, inspection.findings.count { it.rule == A11yRules.PRODUCER_REPORT_INCOMPLETE })
    }

    @Test
    fun `an unknown rule is preserved and an unknown severity degrades to warning`() {
        file("cours.html", "<html lang=\"fr\"><h1>c</h1></html>")
        report(
            "a11y-report.json",
            """
            {
              "deliverables": [
                {
                  "kind": "HTML",
                  "findings": [
                    { "rule": "producer-future-rule", "severity": "BLOCKER", "location": "cours.html", "message": "future" }
                  ],
                  "limits": []
                }
              ]
            }
            """.trimIndent(),
        )

        val inspection = ProducerReportInspector.inspect(dir, materialFiles())

        val finding = inspection.findings.single { it.rule == "producer-future-rule" }
        assertEquals(A11ySeverity.WARNING, finding.severity)
    }
}

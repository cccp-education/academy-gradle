package education.cccp.academy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-15 / GBL-011-5 — functional TestKit guarantees for
 * `auditAcademyAcceptance`.
 *
 * The gate reads the two audit reports through real Gradle inputs. Four
 * contracts are locked end-to-end: opt-in, advisory by default, strict on an
 * error, and a missing layer is a declared gap rather than a crash.
 */
class AcademyAcceptanceFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private fun writeBuild(content: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"consumer-sample\"\n")
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    private fun report(
        relative: String,
        content: String,
    ): File =
        File(projectDir, relative).apply {
            parentFile.mkdirs()
            writeText(content)
        }

    private fun buildScript(
        enabled: Boolean,
        strict: Boolean = false,
    ): String =
        """
        plugins {
            id("education.cccp.academy")
        }

        academyInstaller {
            acceptanceEnabled.set($enabled)
            acceptanceStrict.set($strict)
            a11yReportFile.set(layout.buildDirectory.file("academy/a11y/material.json"))
            moodleA11yReportFile.set(layout.buildDirectory.file("academy/a11y/moodle.json"))
        }
        """.trimIndent()

    private fun reportText(): String {
        val report = File(projectDir, "build/academy/a11y/acceptance-report.json")
        assertTrue(report.isFile, "expected the acceptance report to be written")
        return report.readText()
    }

    private fun runner(vararg args: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments(*args)
            .withPluginClasspath()
            .forwardOutput()

    private val verified = """{ "readiness": "VERIFIED" }"""

    @Test
    fun `the acceptance is opt-in and skipped by default`() {
        writeBuild(buildScript(enabled = false))

        val result = runner("auditAcademyAcceptance").build()

        assertEquals(TaskOutcome.SKIPPED, result.task(":auditAcademyAcceptance")?.outcome)
        assertFalse(
            File(projectDir, "build/academy/a11y/acceptance-report.json").exists(),
            "a skipped acceptance writes no report",
        )
    }

    @Test
    fun `two verified audits are accepted`() {
        report("build/academy/a11y/material.json", verified)
        report("build/academy/a11y/moodle.json", verified)
        writeBuild(buildScript(enabled = true))

        val result = runner("auditAcademyAcceptance").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditAcademyAcceptance")?.outcome)
        val acceptance = reportText()
        assertTrue(acceptance.contains("\"readiness\": \"VERIFIED\""))
        assertTrue(acceptance.contains("\"layer\": \"material\""))
        assertTrue(acceptance.contains("\"layer\": \"moodle\""))
    }

    @Test
    fun `a missing layer is a declared gap, not a crash`() {
        report("build/academy/a11y/material.json", verified)
        writeBuild(buildScript(enabled = true))

        val result = runner("auditAcademyAcceptance").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditAcademyAcceptance")?.outcome)
        val acceptance = reportText()
        assertTrue(acceptance.contains("\"readiness\": \"DEGRADED\""))
        assertTrue(acceptance.contains("moodle audit report not provided"))
    }

    @Test
    fun `strict mode fails on an error finding`() {
        report("build/academy/a11y/material.json", verified)
        report(
            "build/academy/a11y/moodle.json",
            """
            {
              "readiness": "DEGRADED",
              "findings": [
                { "deliverable": "MOODLE", "rule": "moodle-lang-missing", "severity": "ERROR", "location": "x", "message": "no lang" }
              ]
            }
            """.trimIndent(),
        )
        writeBuild(buildScript(enabled = true, strict = true))

        val result = runner("auditAcademyAcceptance").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":auditAcademyAcceptance")?.outcome)
        assertTrue(result.output.contains("strict mode"))
    }

    @Test
    fun `no report at all is a missing acceptance`() {
        writeBuild(buildScript(enabled = true, strict = true))

        val result = runner("auditAcademyAcceptance").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditAcademyAcceptance")?.outcome)
        assertTrue(reportText().contains("\"readiness\": \"MISSING\""))
    }
}

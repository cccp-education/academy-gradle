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
 * ACADEMY-15 — functional TestKit guarantees for `auditTrainingAccessibility`.
 *
 * Three contracts are locked through a real Gradle build (not ProjectBuilder):
 *  - the audit is opt-in: without `a11yAuditEnabled` the task is SKIPPED and no
 *    report is written;
 *  - advisory by default: a finding is written to the report without failing;
 *  - strict mode is the only way to fail the build.
 *
 * The report is a public JSON contract, so the assertions read it as text: the
 * black-box test does not depend on the plugin's implementation classes.
 */
class AcademyA11yFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private fun writeBuild(content: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"consumer-sample\"\n")
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    private fun materialVideo() {
        val video = File(projectDir, "material/clip.mp4")
        video.parentFile.mkdirs()
        video.writeText("x")
    }

    private fun runner(vararg args: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments(*args)
            .withPluginClasspath()
            .forwardOutput()

    private fun optedIn(strict: Boolean): String =
        """
        plugins {
            id("education.cccp.academy")
        }

        academyInstaller {
            a11yAuditEnabled.set(true)
            a11yStrict.set($strict)
            materialDir.set(layout.projectDirectory.dir("material"))
        }
        """.trimIndent()

    private fun reportText(): String =
        File(projectDir, "build/academy/a11y/accessibility-report.json")
            .also { assertTrue(it.isFile, "expected the audit report to be written") }
            .readText()

    @Test
    fun `the audit is opt-in and skipped by default`() {
        materialVideo()
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("auditTrainingAccessibility").build()

        assertEquals(TaskOutcome.SKIPPED, result.task(":auditTrainingAccessibility")?.outcome)
        assertFalse(
            File(projectDir, "build/academy/a11y/accessibility-report.json").exists(),
            "a skipped audit writes no report",
        )
    }

    @Test
    fun `an opted-in advisory audit writes a degraded report without failing`() {
        materialVideo()
        writeBuild(optedIn(strict = false))

        val result = runner("auditTrainingAccessibility").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditTrainingAccessibility")?.outcome)
        val report = reportText()
        assertTrue(report.contains("\"readiness\": \"DEGRADED\""), "the report must carry the verdict")
        assertTrue(report.contains("video-subtitles-missing"), "the report must carry the subtitle finding")
    }

    @Test
    fun `a strict audit fails on an error finding`() {
        materialVideo()
        writeBuild(optedIn(strict = true))

        val result = runner("auditTrainingAccessibility").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":auditTrainingAccessibility")?.outcome)
        assertTrue(
            result.output.contains("strict mode"),
            "strict mode must explain the failure and point to the report",
        )
    }

    @Test
    fun `an absent material directory is a missing report, not a failure`() {
        writeBuild(optedIn(strict = true))

        val result = runner("auditTrainingAccessibility").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditTrainingAccessibility")?.outcome)
        assertTrue(reportText().contains("\"readiness\": \"MISSING\""), "absent material is a missing report")
    }
}

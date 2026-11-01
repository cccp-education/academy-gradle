package education.cccp.academy

import education.cccp.academy.a11y.A11yReadiness
import education.cccp.academy.a11y.MaterialA11yReport
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 — the `auditTrainingAccessibility` task (thin wrapper).
 *
 * The task resolves the material directory and writes the pure auditor's report.
 * An absent directory is a `MISSING` report, and strict mode is the only way to
 * turn findings into a build failure.
 */
class AuditTrainingAccessibilityTaskTest {
    @TempDir
    lateinit var dir: File

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) = project.tasks.getByName("auditTrainingAccessibility") as AuditTrainingAccessibilityTask

    @Test
    fun `the plugin registers the audit task`() {
        val project = applyPlugin()

        assertEquals("academy", project.tasks.getByName("auditTrainingAccessibility").group)
    }

    @Test
    fun `the task writes a degraded report for a video without companions`() {
        val project = applyPlugin()
        File(dir, "slide-01.mp4").writeText("x")
        val task =
            task(project).apply {
                a11yAuditEnabled.set(true)
                a11yStrict.set(false)
                materialFiles.from(dir)
                reportFile.set(project.layout.buildDirectory.file("academy/a11y/accessibility-report.json"))
            }

        task.audit()

        val report =
            MaterialA11yReport.fromJson(
                task.reportFile
                    .get()
                    .asFile
                    .readText(),
            )
        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.findings.isNotEmpty())
    }

    @Test
    fun `an absent material directory yields a missing report`() {
        val project = applyPlugin()
        val task =
            task(project).apply {
                a11yAuditEnabled.set(true)
                a11yStrict.set(false)
                materialFiles.from(File(dir, "absent"))
                reportFile.set(project.layout.buildDirectory.file("academy/a11y/accessibility-report.json"))
            }

        task.audit()

        val report =
            MaterialA11yReport.fromJson(
                task.reportFile
                    .get()
                    .asFile
                    .readText(),
            )
        assertEquals(A11yReadiness.MISSING, report.readiness)
    }
}

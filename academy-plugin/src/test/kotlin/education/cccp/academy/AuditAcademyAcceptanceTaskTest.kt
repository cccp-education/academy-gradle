package education.cccp.academy

import education.cccp.academy.a11y.A11yDeliverable
import education.cccp.academy.a11y.A11yFinding
import education.cccp.academy.a11y.A11yReadiness
import education.cccp.academy.a11y.A11yRules
import education.cccp.academy.a11y.A11ySeverity
import education.cccp.academy.a11y.AcademyA11yReport
import education.cccp.academy.a11y.MaterialA11yReport
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-15 / GBL-011-5 — the `auditAcademyAcceptance` task (thin wrapper).
 *
 * It reads the two audit reports (tolerant: an absent or malformed report is a
 * declared gap, never a crash), aggregates the verdict and, in strict mode,
 * fails on an `ERROR` finding.
 */
class AuditAcademyAcceptanceTaskTest {
    @TempDir
    lateinit var dir: File

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) = project.tasks.getByName("auditAcademyAcceptance") as AuditAcademyAcceptanceTask

    private fun writeReport(
        name: String,
        report: MaterialA11yReport,
    ): File =
        File(dir, name).apply {
            parentFile.mkdirs()
            writeText(MaterialA11yReport.toJson(report))
        }

    private fun enabledTask(
        project: Project,
        material: File? = null,
        moodle: File? = null,
        strict: Boolean = false,
    ) = task(project).apply {
        acceptanceEnabled.set(true)
        acceptanceStrict.set(strict)
        material?.let { materialReports.from(it) }
        moodle?.let { moodleReports.from(it) }
        reportFile.set(project.layout.buildDirectory.file("academy/a11y/acceptance-report.json"))
    }

    private fun report(t: AuditAcademyAcceptanceTask): AcademyA11yReport {
        val text =
            t.reportFile
                .get()
                .asFile
                .readText()
        return AcademyA11yReport.fromJson(text)
    }

    @Test
    fun `the plugin registers the acceptance task`() {
        val project = applyPlugin()

        assertEquals("academy", project.tasks.getByName("auditAcademyAcceptance").group)
    }

    @Test
    fun `the acceptance is skipped when not enabled`() {
        val project = applyPlugin()
        val task = task(project).apply { acceptanceEnabled.set(false) }

        task.audit()

        assertFalse(
            task.reportFile
                .get()
                .asFile
                .exists(),
        )
    }

    @Test
    fun `two verified reports are accepted`() {
        val project = applyPlugin()
        val material = writeReport("material.json", MaterialA11yReport(readiness = A11yReadiness.VERIFIED))
        val moodle = writeReport("moodle.json", MaterialA11yReport(readiness = A11yReadiness.VERIFIED))
        val task = enabledTask(project, material, moodle)

        task.audit()

        assertEquals(A11yReadiness.VERIFIED, report(task).readiness)
        assertEquals(2, report(task).layers.size)
    }

    @Test
    fun `a missing report is a declared gap, not a crash`() {
        val project = applyPlugin()
        val moodle = writeReport("moodle.json", MaterialA11yReport(readiness = A11yReadiness.VERIFIED))
        val task = enabledTask(project, material = null, moodle = moodle)

        task.audit()

        val acceptance = report(task)
        assertEquals(A11yReadiness.DEGRADED, acceptance.readiness)
        assertTrue(acceptance.limits.any { it.contains("material") })
    }

    @Test
    fun `a malformed report is a declared gap, not a crash`() {
        val project = applyPlugin()
        val malformed = File(dir, "material.json").apply { writeText("{ not json") }
        val moodle = writeReport("moodle.json", MaterialA11yReport(readiness = A11yReadiness.VERIFIED))
        val task = enabledTask(project, malformed, moodle)

        task.audit()

        val acceptance = report(task)
        assertEquals(A11yReadiness.DEGRADED, acceptance.readiness)
        assertTrue(acceptance.limits.any { it.contains("material") })
    }

    @Test
    fun `strict mode fails on an error finding`() {
        val project = applyPlugin()
        val material = writeReport("material.json", MaterialA11yReport(readiness = A11yReadiness.VERIFIED))
        val moodle =
            writeReport(
                "moodle.json",
                MaterialA11yReport(
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
            )
        val task = enabledTask(project, material, moodle, strict = true)

        val failure = assertFailsWith<GradleException> { task.audit() }

        assertTrue(failure.message.orEmpty().contains("strict mode"))
    }
}

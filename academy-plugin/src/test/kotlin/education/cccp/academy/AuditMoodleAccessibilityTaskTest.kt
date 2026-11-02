package education.cccp.academy

import education.cccp.academy.a11y.A11yReadiness
import education.cccp.academy.a11y.A11yRules
import education.cccp.academy.a11y.MaterialA11yReport
import education.cccp.academy.a11y.MoodlePageFetcher
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
 * ACADEMY-15 (GBL-011-5) — the `auditMoodleAccessibility` task (thin wrapper).
 *
 * The network lives behind the [MoodlePageFetcher] port so the decision and the
 * verdict are testable without a Moodle. The task is opt-in, advisory by
 * default, and an unreachable page is a declared warning, never a crash.
 */
class AuditMoodleAccessibilityTaskTest {
    @TempDir
    lateinit var dir: File

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) = project.tasks.getByName("auditMoodleAccessibility") as AuditMoodleAccessibilityTask

    private fun enabledTask(
        project: Project,
        fetcher: MoodlePageFetcher,
        strict: Boolean = false,
    ) = task(project).apply {
        moodleA11yEnabled.set(true)
        moodleA11yStrict.set(strict)
        moodleA11yBaseUrl.set("http://localhost:8080")
        moodleA11yPaths.set(listOf("/login/index.php"))
        reportFile.set(project.layout.buildDirectory.file("academy/a11y/moodle-accessibility-report.json"))
        this.fetcher.set(fetcher)
    }

    private fun report(t: AuditMoodleAccessibilityTask): MaterialA11yReport {
        val text =
            t.reportFile
                .get()
                .asFile
                .readText()
        return MaterialA11yReport.fromJson(text)
    }

    @Test
    fun `the plugin registers the audit task`() {
        val project = applyPlugin()

        assertEquals("academy", project.tasks.getByName("auditMoodleAccessibility").group)
    }

    @Test
    fun `the audit is skipped when not enabled and writes no report`() {
        val project = applyPlugin()
        val task = task(project).apply { moodleA11yEnabled.set(false) }

        task.audit()

        assertFalse(
            task.reportFile
                .get()
                .asFile
                .exists(),
        )
    }

    @Test
    fun `an accessible page is verified and the url is resolved`() {
        val project = applyPlugin()
        val requested = mutableListOf<String>()
        val fetcher =
            MoodlePageFetcher { url ->
                requested += url
                """<html lang="fr"><body><h1>Connexion</h1></body></html>"""
            }
        val task = enabledTask(project, fetcher)

        task.audit()

        assertEquals(listOf("http://localhost:8080/login/index.php"), requested)
        assertEquals(A11yReadiness.VERIFIED, report(task).readiness)
    }

    @Test
    fun `an unreachable page degrades the report without failing`() {
        val project = applyPlugin()
        val task = enabledTask(project, MoodlePageFetcher { null })

        task.audit()

        val report = report(task)
        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.findings.any { it.rule == A11yRules.MOODLE_PAGE_UNREACHABLE })
    }

    @Test
    fun `strict mode fails on an error finding`() {
        val project = applyPlugin()
        val task =
            enabledTask(
                project,
                MoodlePageFetcher { """<html><body>no lang</body></html>""" },
                strict = true,
            )

        val failure = assertFailsWith<GradleException> { task.audit() }

        assertTrue(failure.message.orEmpty().contains("strict mode"))
    }

    @Test
    fun `advisory mode reports an error without failing`() {
        val project = applyPlugin()
        val task = enabledTask(project, MoodlePageFetcher { """<html><body>no lang</body></html>""" })

        task.audit()

        assertEquals(A11yReadiness.DEGRADED, report(task).readiness)
    }
}

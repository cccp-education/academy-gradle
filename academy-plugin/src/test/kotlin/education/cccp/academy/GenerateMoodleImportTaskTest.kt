package education.cccp.academy

import education.cccp.academy.moodle.MoodleIngestionScriptGenerator
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ACADEMY-11-2 — the `generateMoodleImport` task (D-ACADEMY-11-5/11-6/11-8).
 *
 * It reads the pulled material directory (structure + EPIC K pivots), builds the
 * pure plan and writes `plan.json` + the generic `ingest.sh`. Degraded by
 * default: an empty material directory writes nothing and succeeds, so an
 * installation without material is byte-identical (D-ACADEMY-11-6).
 *
 * Baby-step TDD: this file drove `GenerateMoodleImportTask`, the extension's
 * `moodleImport*` properties and the plugin registration. RED proved by
 * `Unresolved reference`.
 */
class GenerateMoodleImportTaskTest {

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) =
        project.tasks.getByName("generateMoodleImport") as GenerateMoodleImportTask

    private fun materialDir(parent: File): File = File(parent, "material").apply { mkdirs() }

    /** Writes the real producer layout: the artifact and its sibling EPIC K pivot. */
    private fun pivot(dir: File, relative: String, type: String) {
        val artifact = File(dir, relative)
        artifact.parentFile.mkdirs()
        artifact.writeText("== $relative\n")
        File(artifact.parentFile, "metadata.json")
            .writeText("""{ "source": "training", "type": "$type", "version": "1.0" }""")
    }

    @Test
    fun `the plugin registers the moodle import task`() {
        val project = applyPlugin()

        assertNotNull(project.tasks.findByName("generateMoodleImport"))
        assertEquals("academy", project.tasks.getByName("generateMoodleImport").group)
    }

    @Test
    fun `the extension carries no moodle opt-in by default`() {
        val extension = applyPlugin().extensions.getByType(AcademyInstallerExtension::class.java)

        assertFalse(extension.moodleImportEnabled.get(), "the injection must be opt-in (D-ACADEMY-11-8)")
        assertEquals("academy-seed", extension.moodleCourseShortName.get())
    }

    @Test
    fun `an empty material directory builds an empty plan and writes nothing`(@org.junit.jupiter.api.io.TempDir temp: File) {
        val project = applyPlugin()
        val dir = materialDir(temp)
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            moodleImportEnabled.set(true)
            moodleMaterialDir.set(dir)
        }

        val plan = task(project).buildPlan()

        assertTrue(plan.isEmpty, "no material means an empty plan (D-ACADEMY-11-6)")
        assertEquals(0, plan.activityCount)
    }

    @Test
    fun `the plan is built from the material structure and pivots`(@org.junit.jupiter.api.io.TempDir temp: File) {
        val project = applyPlugin()
        val dir = materialDir(temp)
        pivot(dir, "SPG/spg.adoc", "SPG")
        pivot(dir, "SPD/01_accueil/001_bienvenue.adoc", "SPD")
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            moodleImportEnabled.set(true)
            moodleMaterialDir.set(dir)
            moodleCourseShortName.set("formation-fpa")
            moodleCourseFullName.set("Formation FPA")
        }

        val plan = task(project).buildPlan()

        assertEquals("formation-fpa", plan.courseShortName)
        assertEquals("Formation FPA", plan.courseFullName)
        assertEquals(2, plan.activityCount)
    }

    @Test
    fun `an unreadable pivot never breaks the plan - it is skipped`(@org.junit.jupiter.api.io.TempDir temp: File) {
        val project = applyPlugin()
        val dir = materialDir(temp)
        File(dir, "BROKEN").apply { mkdirs() }
        File(File(dir, "BROKEN"), "metadata.json").writeText("{ not json")
        pivot(dir, "SPG/spg.adoc", "SPG")
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            moodleImportEnabled.set(true)
            moodleMaterialDir.set(dir)
        }

        val plan = task(project).buildPlan()

        assertIs<education.cccp.academy.moodle.MoodleImportPlan>(plan)
        assertEquals(1, plan.activityCount, "the readable SPG is present, the broken pivot is skipped")
    }

    @Test
    fun `the task is skipped unless explicitly enabled`() {
        val extension = applyPlugin().extensions.getByType(AcademyInstallerExtension::class.java)

        assertFalse(extension.moodleImportEnabled.get())
    }

    @Test
    fun `the generic script is the same whatever the formation`() {
        val project = applyPlugin()
        val dir = materialDir(File.createTempFile("academy", "test").parentFile)
        pivot(dir, "SPG/spg.adoc", "SPG")
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            moodleImportEnabled.set(true)
            moodleMaterialDir.set(dir)
            moodleCourseShortName.set("formation-fpa")
        }

        val plan = task(project).buildPlan()
        val planJson = MoodleIngestionScriptGenerator.renderPlanJson(plan)
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        assertTrue(planJson.contains("formation-fpa"), "the plan carries the formation")
        assertTrue(script.contains("moosh"), "the applicator drives the native tools")
    }
}

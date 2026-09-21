package education.cccp.academy

import education.cccp.academy.material.MaterialArtifactType
import education.cccp.academy.material.MaterialReadiness
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * ACADEMY-4-2 — the `inspectTrainingMaterial` task resolves its pure
 * requirement from the lazy extension properties, reads the EPIC K pivots of the
 * material directory and computes a verdict (D-ACADEMY-4-8/4-9).
 *
 * A build never fails on legitimately absent material: without a declared
 * requirement the verdict is `Empty` and the task succeeds (D-ACADEMY-4-7).
 */
class InspectTrainingMaterialTaskTest {

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) =
        project.tasks.getByName("inspectTrainingMaterial") as InspectTrainingMaterialTask

    @Test
    fun `the extension carries no requirement by default`() {
        val extension = applyPlugin().extensions.getByType(AcademyInstallerExtension::class.java)

        assertEquals("", extension.materialRemoteUrl.get(), "no requirement is declared by default")
        assertEquals("", extension.materialCurrentVersion.get())
        assertEquals("material", extension.materialDir.get().asFile.name)
        assertEquals(emptyList(), extension.materialExpectedTypes.get())
    }

    @Test
    fun `resolves the requirement from the extension`() {
        val project = applyPlugin()
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            materialRemoteUrl.set("https://github.com/cccp-education/formation-fpa")
            materialCurrentVersion.set("v1.0")
            materialExpectedTypes.set(listOf(MaterialArtifactType.SPG))
        }

        val requirement = task(project).requirement()

        assertEquals("https://github.com/cccp-education/formation-fpa", requirement.remoteUrl)
        assertEquals("v1.0", requirement.currentVersion)
        assertEquals(listOf(MaterialArtifactType.SPG), requirement.expectedTypes)
    }

    @Test
    fun `an absent material directory with no requirement is empty, not a failure`() {
        val project = applyPlugin()
        val task = task(project)
        task.materialDir.set(File(project.projectDir, "does-not-exist"))

        val readiness = task.inspect()

        assertIs<MaterialReadiness.Empty>(readiness)
        assertFalse(task.hasRequirement(), "no requirement declared - the build must not fail")
    }

    @Test
    fun `a declared requirement is detected`() {
        val project = applyPlugin()
        project.extensions.getByType(AcademyInstallerExtension::class.java)
            .materialRemoteUrl.set("https://example.org/training.git")

        assertTrue(task(project).hasRequirement())
    }

    @Test
    fun `expected artifacts alone declare a requirement even without a remote`() {
        val project = applyPlugin()
        project.extensions.getByType(AcademyInstallerExtension::class.java)
            .materialExpectedTypes.set(listOf(MaterialArtifactType.SPG))

        assertTrue(
            task(project).hasRequirement(),
            "naming the artifacts needed is a requirement - an absent material must then be reported",
        )
    }

    @Test
    fun `reads the pivot manifests and reports ready material`() {
        val project = applyPlugin()
        val materialDir = File(project.projectDir, "material").apply { mkdirs() }
        File(materialDir, "spg.metadata.json").writeText(
            """{ "source": "training", "type": "SPG", "version": "1.0" }""",
        )
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            materialRemoteUrl.set("https://example.org/training.git")
            materialCurrentVersion.set("1.0")
            materialExpectedTypes.set(listOf(MaterialArtifactType.SPG))
        }

        val readiness = task(project).inspect()

        val ready = assertIs<MaterialReadiness.Ready>(readiness)
        assertEquals(listOf(MaterialArtifactType.SPG), ready.presentTypes)
    }

    @Test
    fun `an unreadable manifest is ignored rather than throwing`() {
        val project = applyPlugin()
        val materialDir = File(project.projectDir, "material").apply { mkdirs() }
        File(materialDir, "broken.json").writeText("not json")
        project.extensions.getByType(AcademyInstallerExtension::class.java)
            .materialRemoteUrl.set("https://example.org/training.git")

        val readiness = task(project).inspect()

        assertIs<MaterialReadiness.Empty>(readiness, "an unreadable file must not break the inspection")
    }
}

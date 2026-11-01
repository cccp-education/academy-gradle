package education.cccp.academy

import education.cccp.academy.installer.offline.DockerImageRunner
import education.cccp.academy.installer.offline.DockerRunResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-14 — the `buildOfflineBundle` task (thin wrapper).
 *
 * The task is the adapter seam: the Docker process is injected as a
 * [DockerImageRunner], so the orchestration (pull each reference, resolve its
 * digest, save one archive, write the manifest) is exercised without Docker
 * and without touching a machine.
 *
 * Baby-step TDD: this file is written before the task and the domain; RED is
 * proven by `Unresolved reference`.
 */
class BuildOfflineBundleTaskTest {
    private class RecordingDockerRunner : DockerImageRunner {
        val commands = mutableListOf<List<String>>()

        override fun run(command: List<String>): DockerRunResult {
            commands += command
            return when (command.getOrNull(1)) {
                "save" -> {
                    val output = command[command.indexOf("-o") + 1]
                    File(output).apply { parentFile.mkdirs() }.writeText("offline-archive")
                    DockerRunResult(0)
                }
                "inspect" -> {
                    val reference = command.last()
                    DockerRunResult(0, "$reference@sha256:" + "a".repeat(64))
                }
                else -> DockerRunResult(0)
            }
        }
    }

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) = project.tasks.getByName("buildOfflineBundle") as BuildOfflineBundleTask

    @Test
    fun `the plugin registers the offline bundle task`() {
        val project = applyPlugin()

        assertEquals("academy", project.tasks.getByName("buildOfflineBundle").group)
    }

    @Test
    fun `the task pulls every reference, resolves its digest and saves one archive`() {
        val project = applyPlugin()
        val runner = RecordingDockerRunner()
        val task =
            task(project).apply {
                references.set(listOf("postgres:16", "ollama/ollama:latest"))
                bundleFile.set(project.layout.buildDirectory.file("academy/offline/offline-images.tar"))
                manifestFile.set(project.layout.buildDirectory.file("academy/offline/offline-images.json"))
                dockerRunner.set(runner)
            }

        task.buildBundle()

        assertTrue(runner.commands.contains(listOf("docker", "pull", "postgres:16")))
        assertTrue(runner.commands.contains(listOf("docker", "pull", "ollama/ollama:latest")))
        assertTrue(
            runner.commands.any {
                it.getOrNull(1) == "build" &&
                    it.contains("cccp-education/academy-workspace:latest")
            },
        )
        assertTrue(
            runner.commands.any {
                it.getOrNull(1) == "save" &&
                    it.contains("postgres:16") &&
                    it.contains("ollama/ollama:latest") &&
                    it.contains("cccp-education/academy-workspace:latest")
            },
        )
        assertTrue(
            task.bundleFile
                .get()
                .asFile
                .exists(),
        )
        assertTrue(
            task.manifestFile
                .get()
                .asFile
                .exists(),
        )
    }

    @Test
    fun `the manifest records the resolved digests`() {
        val project = applyPlugin()
        val task =
            task(project).apply {
                references.set(listOf("postgres:16"))
                bundleFile.set(project.layout.buildDirectory.file("academy/offline/offline-images.tar"))
                manifestFile.set(project.layout.buildDirectory.file("academy/offline/offline-images.json"))
                dockerRunner.set(RecordingDockerRunner())
            }

        task.buildBundle()

        val manifest =
            task.manifestFile
                .get()
                .asFile
                .readText()
        assertTrue(manifest.contains("sha256:" + "a".repeat(64)))
        assertTrue(manifest.contains("postgres:16"))
        assertTrue(manifest.contains("cccp-education/academy-workspace:latest"))
    }
}

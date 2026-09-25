package education.cccp.academy

import education.cccp.academy.env.DockerStrategy
import education.cccp.academy.env.EnvironmentFacts
import education.cccp.academy.env.EnvironmentProbe
import education.cccp.academy.env.HostOs
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * ACADEMY-12-2 — the `checkContainerEngine` task (D-ACADEMY-12-2).
 *
 * The task is the adapter seam: it probes the host through an injectable
 * [EnvironmentProbe] (I/O), then computes the verdict with the pure
 * [education.cccp.academy.env.DockerStrategyDecider]. Unit tests inject a fake
 * probe, so the decision is exercised without a machine and without executing a
 * single command — the boundary D-ACADEMY-12-6.
 *
 * Baby-step TDD: this file is written before the task and the probe port; RED is
 * proven by `Unresolved reference`.
 */
class CheckContainerEngineTaskTest {

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    private fun task(project: Project) =
        project.tasks.getByName("checkContainerEngine") as CheckContainerEngineTask

    private fun probe(
        os: HostOs,
        engineReady: Boolean = false,
        wsl2Present: Boolean = false,
        admin: Boolean = false,
    ) = EnvironmentProbe {
        EnvironmentFacts(
            os = os,
            dockerEngineReady = engineReady,
            wsl2Present = wsl2Present,
            admin = admin,
        )
    }

    @Test
    fun `the plugin registers the container engine check task`() {
        val project = applyPlugin()

        assertNotNull(project.tasks.findByName("checkContainerEngine"))
        assertEquals("academy", project.tasks.getByName("checkContainerEngine").group)
    }

    @Test
    fun `a reachable engine is reported as already ready`() {
        val project = applyPlugin()
        task(project).environmentProbe.set(probe(HostOs.LINUX, engineReady = true))

        assertIs<DockerStrategy.AlreadyReady>(task(project).strategy())
    }

    @Test
    fun `linux without an engine resolves to the native engine install`() {
        val project = applyPlugin()
        task(project).environmentProbe.set(probe(HostOs.LINUX))

        assertIs<DockerStrategy.InstallDockerEngine>(task(project).strategy())
    }

    @Test
    fun `windows without WSL2 and with admin resolves to the WSL2 bootstrap`() {
        val project = applyPlugin()
        task(project).environmentProbe.set(probe(HostOs.WINDOWS, wsl2Present = false, admin = true))

        assertIs<DockerStrategy.BootstrapWsl2First>(task(project).strategy())
    }

    @Test
    fun `an unprovisionable host is reported as a verdict, never a build failure`() {
        val project = applyPlugin()
        task(project).environmentProbe.set(probe(HostOs.WINDOWS, wsl2Present = false, admin = false))

        assertIs<DockerStrategy.Unsupported>(task(project).strategy())
        task(project).checkContainerEngine()
    }

    @Test
    fun `a provisionable host succeeds and states the strategy`() {
        val project = applyPlugin()
        task(project).environmentProbe.set(probe(HostOs.MACOS))

        task(project).checkContainerEngine()
    }
}

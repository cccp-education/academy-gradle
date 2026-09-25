package education.cccp.academy.env

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * ACADEMY-12-1 — the pure environment decider: a deterministic function of the
 * observed [EnvironmentFacts] to a [DockerStrategy] verdict (D-ACADEMY-12-2/3).
 * No I/O, no Gradle, no command executed: the probe belongs to the task adapter.
 *
 * It never throws on an unprovisionable host — that is a verdict (`Unsupported`),
 * not an error (same sealed-verdict pattern as MaterialReadiness).
 */
class DockerStrategyDeciderTest {

    @Test
    fun `a reachable engine is ready on every OS`() {
        HostOs.entries.forEach { os ->
            val strategy = DockerStrategyDecider.decide(facts(os = os, engineReady = true))

            assertIs<DockerStrategy.AlreadyReady>(strategy)
        }
    }

    @Test
    fun `linux without an engine installs the native Docker Engine`() {
        val strategy = DockerStrategyDecider.decide(facts(os = HostOs.LINUX))

        assertIs<DockerStrategy.InstallDockerEngine>(strategy)
    }

    @Test
    fun `macos without an engine installs Colima`() {
        val strategy = DockerStrategyDecider.decide(facts(os = HostOs.MACOS))

        assertIs<DockerStrategy.InstallColima>(strategy)
    }

    @Test
    fun `windows with WSL2 present installs Docker Desktop`() {
        val strategy = DockerStrategyDecider.decide(
            facts(os = HostOs.WINDOWS, wsl2Present = true),
        )

        assertIs<DockerStrategy.InstallDockerDesktop>(strategy)
    }

    @Test
    fun `windows without WSL2 and with admin bootstraps WSL2 first`() {
        val strategy = DockerStrategyDecider.decide(
            facts(os = HostOs.WINDOWS, wsl2Present = false, admin = true),
        )

        assertIs<DockerStrategy.BootstrapWsl2First>(strategy)
    }

    @Test
    fun `windows without WSL2 and without admin is unsupported`() {
        val strategy = DockerStrategyDecider.decide(
            facts(os = HostOs.WINDOWS, wsl2Present = false, admin = false),
        )

        val unsupported = assertIs<DockerStrategy.Unsupported>(strategy)
        assertTrue(unsupported.reason.isNotBlank(), "an unsupported verdict must explain why")
    }

    @Test
    fun `an unknown OS is unsupported`() {
        val strategy = DockerStrategyDecider.decide(facts(os = HostOs.UNKNOWN))

        assertIs<DockerStrategy.Unsupported>(strategy)
    }

    @Test
    fun `admin and WSL2 facts are irrelevant once the engine is ready`() {
        val strategy = DockerStrategyDecider.decide(
            facts(os = HostOs.WINDOWS, engineReady = true, wsl2Present = false, admin = false),
        )

        assertIs<DockerStrategy.AlreadyReady>(strategy)
    }

    @Test
    fun `the verdict never throws on any combination`() {
        HostOs.entries.forEach { os ->
            listOf(true, false).forEach { engineReady ->
                listOf(true, false).forEach { wsl2 ->
                    listOf(true, false).forEach { admin ->
                        DockerStrategyDecider.decide(
                            facts(os = os, engineReady = engineReady, wsl2Present = wsl2, admin = admin),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `already ready exposes the detected OS`() {
        val ready = assertIs<DockerStrategy.AlreadyReady>(
            DockerStrategyDecider.decide(facts(os = HostOs.LINUX, engineReady = true)),
        )

        assertEquals(HostOs.LINUX, ready.os)
    }

    private fun facts(
        os: HostOs,
        engineReady: Boolean = false,
        wsl2Present: Boolean = false,
        admin: Boolean = false,
    ) = EnvironmentFacts(
        os = os,
        dockerEngineReady = engineReady,
        wsl2Present = wsl2Present,
        admin = admin,
    )
}

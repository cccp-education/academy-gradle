package education.cccp.academy.bdd

import education.cccp.academy.env.DockerStrategy
import education.cccp.academy.env.DockerStrategyDecider
import education.cccp.academy.env.EnvironmentFacts
import education.cccp.academy.env.HostOs
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_env.feature` (ACADEMY-12-1) — pattern S-088
 * (feature-scoped glue). Drives the pure environment domain directly: zero
 * Gradle task invocation, zero probe, zero command executed (D-ACADEMY-12-2).
 *
 * Every step is prefixed with the `environment` vocabulary: the glue path is
 * shared with the installer/opencode/byok/bridge/material/moodle steps, so
 * unprefixed phrases would collide (lesson S-088).
 */
class AcademyEnvSteps : En {

    private var os: HostOs = HostOs.UNKNOWN
    private var engineReady: Boolean = false
    private var wsl2Present: Boolean = false
    private var admin: Boolean = false
    private var strategy: DockerStrategy? = null

    init {
        Given("an environment on {string} where the docker engine is ready") { osName: String ->
            os = hostOs(osName)
            engineReady = true
            wsl2Present = false
            admin = false
            strategy = null
        }

        Given("an environment on {string} where the docker engine is not ready") { osName: String ->
            os = hostOs(osName)
            engineReady = false
            wsl2Present = false
            admin = false
            strategy = null
        }

        And("the environment has WSL2") {
            wsl2Present = true
        }

        And("the environment has no WSL2") {
            wsl2Present = false
        }

        And("the environment runs with administrator rights") {
            admin = true
        }

        And("the environment runs without administrator rights") {
            admin = false
        }

        When("the container engine strategy is decided") {
            strategy = DockerStrategyDecider.decide(
                EnvironmentFacts(
                    os = os,
                    dockerEngineReady = engineReady,
                    wsl2Present = wsl2Present,
                    admin = admin,
                ),
            )
        }

        Then("the strategy is already ready") {
            assertThat(selected()).isInstanceOf(DockerStrategy.AlreadyReady::class.java)
        }

        Then("the strategy installs the native docker engine") {
            assertThat(selected()).isInstanceOf(DockerStrategy.InstallDockerEngine::class.java)
        }

        Then("the strategy installs colima") {
            assertThat(selected()).isInstanceOf(DockerStrategy.InstallColima::class.java)
        }

        Then("the strategy installs docker desktop") {
            assertThat(selected()).isInstanceOf(DockerStrategy.InstallDockerDesktop::class.java)
        }

        Then("the strategy bootstraps WSL2 first") {
            assertThat(selected()).isInstanceOf(DockerStrategy.BootstrapWsl2First::class.java)
        }

        Then("the strategy is unsupported with a reason") {
            val unsupported = selected()
            assertThat(unsupported).isInstanceOf(DockerStrategy.Unsupported::class.java)
            assertThat((unsupported as DockerStrategy.Unsupported).reason).isNotBlank()
        }
    }

    private fun selected(): DockerStrategy = checkNotNull(strategy) { "no strategy decided yet" }

    private fun hostOs(name: String): HostOs = when (name) {
        "linux" -> HostOs.LINUX
        "windows" -> HostOs.WINDOWS
        "macos" -> HostOs.MACOS
        else -> HostOs.UNKNOWN
    }
}

package education.cccp.academy.bdd

import education.cccp.academy.installer.InstallerFile
import education.cccp.academy.installer.InstallerPlatform
import education.cccp.academy.installer.InstallerScriptGenerator
import education.cccp.academy.installer.TargetOs
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_installer.feature` (ACADEMY-1-2) — pattern S-088
 * (feature-scoped glue). Drives the pure [InstallerScriptGenerator] directly:
 * zero Gradle task invocation, zero I/O, pure domain BDD (pattern codex
 * fine-tuning/validate suites). Cucumber re-instantiates the glue per
 * scenario, so instance fields are naturally reset.
 */
class AcademyInstallerSteps : En {

    private var os: TargetOs? = null
    private var applicationName: String = "academy"
    private var applicationVersion: String = "0.0.1"
    private var javaVersion: String = "25"
    private var gradleVersion: String = "9.7.1"
    private var composeEnabled: Boolean = true
    private var credentialsEnvPrefix: String = "ACADEMY_"
    private var files: List<InstallerFile> = emptyList()

    init {
        Given("a platform targeting {string}") { target: String ->
            os = TargetOs.entries.first { it.name.lowercase() == target }
        }

        And("application name {string}") { name: String -> applicationName = name }
        And("version {string}") { version: String -> applicationVersion = version }
        And("Java major {string}") { major: String -> javaVersion = major }
        And("Gradle {string}") { gradle: String -> gradleVersion = gradle }

        And("compose embedding enabled") { composeEnabled = true }
        And("compose embedding disabled") { composeEnabled = false }

        And("credential env prefix {string}") { prefix: String -> credentialsEnvPrefix = prefix }

        When("the installer generator renders the platform") {
            val platform = InstallerPlatform(
                os = checkNotNull(os) { "target platform must be set" },
                applicationName = applicationName,
                applicationVersion = applicationVersion,
                javaVersion = javaVersion,
                gradleVersion = gradleVersion,
                composeEnabled = composeEnabled,
                credentialsEnvPrefix = credentialsEnvPrefix,
            )
            files = InstallerScriptGenerator.render(platform)
        }

        Then("exactly one file {string} is produced") { fileName: String ->
            assertThat(files).hasSize(1)
            assertThat(files.single().relativePath).isEqualTo(fileName)
        }

        And("the script starts with {string}") { prefix: String ->
            assertThat(script()).startsWith(prefix)
        }

        And("the script contains {string}") { fragment: String ->
            assertThat(script()).contains(fragment)
        }

        And("the script does not contain {string}") { fragment: String ->
            assertThat(script()).doesNotContain(fragment)
        }

        And("the script is idempotent") {
            assertThat(script()).contains("already installed")
        }

        And("the script bootstraps Docker") {
            assertThat(script()).contains("docker")
        }

        And("the script uses PowerShell") {
            assertThat(script()).contains("PowerShell")
        }
    }

    private fun script(): String = files.single().content
}
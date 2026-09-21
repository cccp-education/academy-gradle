package education.cccp.academy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-1-2 — functional TestKit guarantees: the installer tasks really run
 * inside a Gradle build (not ProjectBuilder stubs) and write the generated
 * scripts to the expected locations, honouring the extension overrides.
 */
class AcademyInstallerFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `buildAllInstallers generates installers for all three platforms`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)
        val linux = File(projectDir, "build/academy/installers/linux/install.sh")
        assertTrue(linux.isFile, "expected linux/install.sh to be generated")
        assertTrue(linux.readText().contains("academy"), "linux script must embed the default application name")
        assertTrue(File(projectDir, "build/academy/installers/windows/install.bat").isFile)
        assertTrue(File(projectDir, "build/academy/installers/macos/install.sh").isFile)
    }

    @Test
    fun `custom application name and output dir are honoured`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                applicationName.set("my-academy")
                outputDir.set(layout.buildDirectory.dir("dist/installers"))
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)
        val linux = File(projectDir, "build/dist/installers/linux/install.sh")
        assertTrue(linux.isFile, "expected linux/install.sh under the custom output dir")
        assertTrue(linux.readText().contains("my-academy"))
    }

    @Test
    fun `compose scaffold and env are written as real files with the full contract`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val compose = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertTrue(compose.contains("cat > \"\$APP_DIR/docker-compose.yml\" <<'EOF'"), "linux script must write the compose file")
        assertTrue(compose.contains("cat > \"\$APP_DIR/.env\" <<'EOF'"), "linux script must write the .env file")
        assertTrue(compose.contains("networks:"), "compose must declare the dedicated network")
        assertTrue(compose.contains("academy-net"), "compose must name the dedicated network")
        assertTrue(compose.contains("volumes:"), "compose must declare named volumes")
        assertTrue(compose.contains("pg_isready"), "postgres must ship a healthcheck")

        val windows = File(projectDir, "build/academy/installers/windows/install.bat").readText()
        assertTrue(windows.contains("> \"%APP_DIR%\\docker-compose.yml\" ("), "windows script must write the compose file")
        assertTrue(windows.contains("> \"%APP_DIR%\\.env\" ("), "windows script must write the .env file")
        assertTrue(windows.contains("academy-net"), "windows compose must declare the network (parity)")
        assertTrue(windows.contains("pg_isready"), "windows postgres must ship a healthcheck (parity)")
    }

    private fun writeBuild(content: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"consumer-sample\"\n")
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*args)
        .withPluginClasspath()
        .forwardOutput()
}
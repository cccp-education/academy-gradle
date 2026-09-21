package education.cccp.academy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `generated compose references real images and is directly executable`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val scripts = listOf(
            File(projectDir, "build/academy/installers/linux/install.sh").readText(),
            File(projectDir, "build/academy/installers/windows/install.bat").readText(),
        )
        scripts.forEach { script ->
            assertTrue(
                script.contains("erseco/alpine-moodle:v5.2.3"),
                "compose must reference the real latest Moodle image",
            )
            assertFalse(script.contains("moodle:4.5"), "the non-existent placeholder image must be gone")
            assertTrue(script.contains("portainer/portainer-ce"), "portainer must use the maintained CE image")
            assertFalse(
                script.contains("cccp-education/academy-workspace"),
                "the unpublished workspace image must not block compose up",
            )
            assertTrue(script.contains("DB_TYPE"), "moodle must wire PostgreSQL via the real contract")
            assertTrue(script.contains("MOODLE_USERNAME"), "moodle auto-install requires the admin variables")
        }
    }

    @Test
    fun `seed entrypoint and course sql are written as real files with the full contract`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val linux = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertTrue(
            linux.contains("cat > \"\$APP_DIR/seed/seed.sh\" <<'EOF'"),
            "linux script must write the seed entrypoint",
        )
        assertTrue(
            linux.contains("cat > \"\$APP_DIR/seed/seed-course.sql\" <<'EOF'"),
            "linux script must write the seed sql",
        )
        assertTrue(linux.contains("moodle-seed"), "linux compose must declare the seed service")
        assertTrue(linux.contains("until psql"), "linux seed entrypoint must poll the schema")
        assertTrue(linux.contains("WHERE NOT EXISTS"), "linux seed sql must be idempotent")

        val windows = File(projectDir, "build/academy/installers/windows/install.bat").readText()
        assertTrue(
            windows.contains("> \"%APP_DIR%\\seed\\seed.sh\" ("),
            "windows script must write the seed entrypoint",
        )
        assertTrue(
            windows.contains("> \"%APP_DIR%\\seed\\seed-course.sql\" ("),
            "windows script must write the seed sql",
        )
        assertTrue(windows.contains("moodle-seed"), "windows compose must declare the seed service (parity)")
        assertTrue(windows.contains("mdl_course"), "windows seed entrypoint must wait for the schema (parity)")
    }

    @Test
    fun `host bootstrap provisions docker only - no host toolchain is installed`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val scripts = listOf(
            File(projectDir, "build/academy/installers/linux/install.sh").readText(),
            File(projectDir, "build/academy/installers/macos/install.sh").readText(),
            File(projectDir, "build/academy/installers/windows/install.bat").readText(),
        )
        scripts.forEach { script ->
            assertTrue(script.contains("docker"), "every installer must bootstrap Docker")
            assertFalse(script.contains("JAVA_HOME"), "the host must not provision Java (D-ACADEMY-6-10)")
            assertFalse(script.contains("services.gradle.org"), "the host must not download Gradle (D-ACADEMY-6-10)")
        }
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
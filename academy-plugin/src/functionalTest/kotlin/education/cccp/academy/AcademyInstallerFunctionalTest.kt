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
            assertTrue(
                script.contains("cccp-education/academy-workspace"),
                "the workspace image must now be a buildable service (ACADEMY-5)",
            )
            assertTrue(script.contains("Dockerfile"), "the workspace Dockerfile must be written by the installer")
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

    @Test
    fun `workspace image opencode config and learner guide are written as real files`() {
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
        assertTrue(linux.contains("cat > \"\$APP_DIR/project/Dockerfile\" <<'EOF'"), "linux must write the workspace Dockerfile")
        assertTrue(linux.contains("cat > \"\$APP_DIR/project/opencode.json\" <<'EOF'"), "linux must write opencode.json")
        assertTrue(linux.contains("cat > \"\$APP_DIR/project/AGENTS.md\" <<'EOF'"), "linux must write the learner guide")
        assertTrue(linux.contains("FROM gradle:9.7.1-jdk25"), "the Dockerfile must base on the pinned gradle image")

        val windows = File(projectDir, "build/academy/installers/windows/install.bat").readText()
        assertTrue(windows.contains("> \"%APP_DIR%\\project\\Dockerfile\" ("), "windows must write the workspace Dockerfile (parity)")
        assertTrue(windows.contains("> \"%APP_DIR%\\project\\opencode.json\" ("), "windows must write opencode.json (parity)")
        assertTrue(windows.contains("FROM gradle:9.7.1-jdk25"), "windows Dockerfile must base on the pinned gradle image (parity)")
    }

    @Test
    fun `a configured provider writes the byok key variable name without a value (ACADEMY-7)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                openCodeProvider.set(contracts.runtime.LlmProviderKind.GEMINI)
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
            assertTrue(script.contains("GEMINI_API_KEY="), "the .env must document the key variable name")
            assertFalse(script.contains("GEMINI_API_KEY=sk-"), "the key value must never be written (Secrets rule)")
            assertTrue(
                script.contains("\"{env:GEMINI_API_KEY}\""),
                "opencode.json must reference the key through the env syntax",
            )
        }
    }

    @Test
    fun `a custom provider carries its own base url and key variable name (ACADEMY-7)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                openCodeProvider.set(contracts.runtime.LlmProviderKind.CUSTOM)
                openCodeProviderUrl.set("https://api.myprovider.com/v1")
                openCodeApiKeyEnvVar.set("MY_PROVIDER_KEY")
                openCodeModel.set("gpt-oss:120b-cloud")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val linux = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertTrue(linux.contains("\"model\": \"custom/gpt-oss:120b-cloud\""), "the model must carry the custom provider prefix")
        assertTrue(linux.contains("https://api.myprovider.com/v1"), "the custom base url must be rendered")
        assertTrue(linux.contains("{env:MY_PROVIDER_KEY}"), "the custom key must be referenced by env name")
        assertTrue(linux.contains("MY_PROVIDER_KEY="), "the .env must document the custom key variable name")
    }

    @Test
    fun `the webhook bridge task is skipped unless explicitly enabled (ACADEMY-8)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("serveWebhookBridge").build()

        assertEquals(
            TaskOutcome.SKIPPED,
            result.task(":serveWebhookBridge")?.outcome,
            "a build must never open a port without an opt-in (D-ACADEMY-8-7)",
        )
    }

    @Test
    fun `a declared material requirement writes the material guide without a runnable bureau command (ACADEMY-4)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                materialRemoteUrl.set("https://github.com/cccp-education/formation-fpa")
                materialCurrentVersion.set("v1.0")
                materialExpectedTypes.set(listOf(education.cccp.academy.material.MaterialArtifactType.SPG))
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
                script.contains("cat > \"\$APP_DIR/project/MATERIAL.md\" <<'EOF'") ||
                    script.contains("> \"%APP_DIR%\\project\\MATERIAL.md\" ("),
                "the material guide must be written by the installer",
            )
            assertTrue(
                script.contains("https://github.com/cccp-education/formation-fpa"),
                "the guide must carry the declared remote (parity)",
            )
            assertFalse(
                script.contains("pullMaterial"),
                "pullMaterial is BUREAU-3 TODO - it must never be cited as runnable (S-007/S-011)",
            )
        }
    }

    @Test
    fun `the generated scripts provision the container engine per OS (ACADEMY-12)`() {
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
        assertTrue(linux.contains("provisioning the Docker Engine"), "linux must provision the engine")
        assertTrue(linux.contains("download.docker.com"), "linux must use Docker's official repository")
        assertTrue(linux.contains("docker-ce"), "linux must install docker-ce")

        val macos = File(projectDir, "build/academy/installers/macos/install.sh").readText()
        assertTrue(macos.contains("brew install colima docker"), "macos must provision Colima")
        assertTrue(macos.contains("colima start"), "macos must start Colima")

        val windows = File(projectDir, "build/academy/installers/windows/install.bat").readText()
        assertTrue(windows.contains("wsl --status"), "windows must probe WSL2")
        assertTrue(windows.contains("--backend=wsl-2"), "windows must install Docker Desktop on WSL2")
        assertTrue(windows.contains("wsl --install"), "windows without WSL2 must bootstrap it")
    }

    @Test
    fun `checkContainerEngine never fails a bare host - it reports a verdict (ACADEMY-12)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("checkContainerEngine").build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":checkContainerEngine")?.outcome,
            "an unprovisionable host is a verdict, never a throw (D-ACADEMY-12-3, P0 S-014)",
        )
        assertTrue(
            result.output.contains("[academy]"),
            "the task must state the provisioning strategy",
        )
    }

    @Test
    fun `the generated unix scripts pass a real bash syntax check (ACADEMY-12)`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            File("/bin/bash").canExecute() || File("/usr/bin/bash").canExecute(),
            "bash is required to syntax-check the generated installer",
        )
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        listOf("linux", "macos").forEach { os ->
            val script = File(projectDir, "build/academy/installers/$os/install.sh")
            val syntax = ProcessBuilder("bash", "-n", script.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = syntax.inputStream.bufferedReader().readText()
            val exit = syntax.waitFor()
            assertEquals(0, exit, "the $os installer must be valid bash, got:\n$output")
        }
    }

    @Test
    fun `inspectTrainingMaterial reports empty on absent material without failing the build (ACADEMY-4)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("inspectTrainingMaterial").build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":inspectTrainingMaterial")?.outcome,
            "no requirement declared - absent material is a legitimate empty report, never a failure",
        )
        assertTrue(
            result.output.contains("nothing to verify"),
            "the task must report the empty state",
        )
    }

    @Test
    fun `inspectTrainingMaterial fails when a declared requirement is unsatisfied (ACADEMY-4)`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                materialRemoteUrl.set("https://github.com/cccp-education/formation-fpa")
                materialCurrentVersion.set("v1.0")
                materialExpectedTypes.set(listOf(education.cccp.academy.material.MaterialArtifactType.SPG))
            }
            """.trimIndent(),
        )

        val result = runner("inspectTrainingMaterial").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":inspectTrainingMaterial")?.outcome)
        assertTrue(
            result.output.contains("pull the tagged version first"),
            "a declared but absent requirement must fail with an actionable message",
        )
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
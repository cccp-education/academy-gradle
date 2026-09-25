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
 * ACADEMY-11-4 — functional TestKit guarantees for the one-shot
 * `moodle-material` compose service (D-ACADEMY-11-5/11-6).
 *
 * Two contracts are locked end-to-end through a real Gradle build:
 *  - opted in **with** material: the generated `install.sh` declares the
 *    one-shot service sharing the `moodle-html` volume and stages the generic
 *    entrypoint plus the plan-driven applicator;
 *  - degraded by default: without the opt-in, the very same installer carries
 *    no service, no shared volume, no staging directory.
 */
class AcademyMoodleMaterialFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private fun writeBuild(content: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"consumer-sample\"\n")
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*args)
        .withPluginClasspath()
        .forwardOutput()

    /** Writes the real producer layout under `material/`: artifact + sibling pivot. */
    private fun pivot(relative: String, type: String) {
        val artifact = File(projectDir, "material/$relative")
        artifact.parentFile.mkdirs()
        artifact.writeText("== $relative\n")
        File(artifact.parentFile, "metadata.json")
            .writeText("""{ "source": "training", "type": "$type", "version": "1.0" }""")
    }

    @Test
    fun `an opted-in project stages the material service the entrypoint and the plan`() {
        pivot("SPG/spg.adoc", "SPG")
        pivot("SPD/01_accueil/001_bienvenue.adoc", "SPD")
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                moodleImportEnabled.set(true)
                moodleMaterialDir.set(layout.projectDirectory.dir("material"))
                moodleCourseShortName.set("formation-fpa")
                moodleCourseFullName.set("Formation FPA")
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val linux = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertTrue(linux.contains("moodle-material"), "the one-shot service must be staged")
        assertTrue(linux.contains("restart: \"no\""), "the material service must be one-shot")
        assertTrue(linux.contains("moodle-html:/var/www/html"), "the Moodle tree must be shared (S-016)")
        assertTrue(linux.contains("moodle-html:"), "the shared volume must be declared")
        assertTrue(linux.contains("cat > \"\$APP_DIR/moodle/entrypoint.sh\" <<'EOF'"), "the entrypoint must be written")
        assertTrue(linux.contains("cat > \"\$APP_DIR/moodle/plan.json\" <<'EOF'"), "the plan must be written")
        assertTrue(linux.contains("cat > \"\$APP_DIR/moodle/ingest.sh\" <<'EOF'"), "the applicator must be written")
        assertTrue(linux.contains("mkdir -p \"\$APP_DIR/material\""), "the material mount point must be created")
        assertTrue(linux.contains("moosh course-list"), "the entrypoint must wait for the moosh bootstrap")

        val windows = File(projectDir, "build/academy/installers/windows/install.bat").readText()
        assertTrue(windows.contains("moodle-material"), "windows must stage the service (parity)")
        assertTrue(windows.contains("> \"%APP_DIR%\\moodle\\entrypoint.sh\" ("), "windows must write the entrypoint (parity)")
        assertTrue(windows.contains("MATERIAL_DIR"), "windows applicator must read the material directory (parity)")
    }

    @Test
    fun `an opted-in project without material stages only the degraded entrypoint`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                moodleImportEnabled.set(true)
                moodleMaterialDir.set(layout.projectDirectory.dir("material"))
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val linux = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertTrue(linux.contains("moodle-material"), "the service is still declared")
        assertTrue(linux.contains("cat > \"\$APP_DIR/moodle/entrypoint.sh\" <<'EOF'"), "the entrypoint is staged")
        assertTrue(
            !linux.contains("cat > \"\$APP_DIR/moodle/plan.json\" <<'EOF'"),
            "no material means no plan is staged (D-ACADEMY-11-6)",
        )
        assertTrue(linux.contains("no staged material plan - nothing to inject"), "the entrypoint must degrade explicitly")
    }

    @Test
    fun `a project without the opt-in carries no material service at all - byte-identical`() {
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
        assertFalse(linux.contains("moodle-material"), "no opt-in means no service (D-ACADEMY-11-6)")
        assertFalse(linux.contains("moodle-html"), "no opt-in means no shared volume (D-ACADEMY-11-6)")
        assertFalse(linux.contains("\$APP_DIR/moodle"), "no opt-in means no staging directory (D-ACADEMY-11-6)")

        val windows = File(projectDir, "build/academy/installers/windows/install.bat").readText()
        assertFalse(windows.contains("moodle-material"), "windows must be degraded too (parity)")
    }

    @Test
    fun `the generated unix installer with injection stays valid bash`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            File("/bin/bash").canExecute() || File("/usr/bin/bash").canExecute(),
            "bash is required to syntax-check the generated installer",
        )
        pivot("SPG/spg.adoc", "SPG")
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                moodleImportEnabled.set(true)
                moodleMaterialDir.set(layout.projectDirectory.dir("material"))
            }
            """.trimIndent(),
        )

        val result = runner("buildAllInstallers").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":buildAllInstallers")?.outcome)

        val script = File(projectDir, "build/academy/installers/linux/install.sh")
        val syntax = ProcessBuilder("bash", "-n", script.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = syntax.inputStream.bufferedReader().readText()
        val exit = syntax.waitFor()
        assertEquals(0, exit, "the installer with injection must be valid bash, got:\n$output")
    }

    @Test
    fun `changing the material re-renders the staged plan - a plain re-run is never stale`() {
        pivot("SPG/spg.adoc", "SPG")
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }

            academyInstaller {
                moodleImportEnabled.set(true)
                moodleMaterialDir.set(layout.projectDirectory.dir("material"))
            }
            """.trimIndent(),
        )

        assertEquals(TaskOutcome.SUCCESS, runner("generateLinuxInstaller").build().task(":generateLinuxInstaller")?.outcome)
        val first = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertFalse(first.contains("03_avance"), "the initial material has no third module")

        pivot("SPD/03_avance/001_approfondissement.adoc", "SPD")

        // A second plain run (no --rerun-tasks) must observe the new material:
        // the staged plan belongs to the installer output, a stale scaffold is a
        // silent bug (S-007/S-016).
        assertEquals(TaskOutcome.SUCCESS, runner("generateLinuxInstaller").build().task(":generateLinuxInstaller")?.outcome)
        val second = File(projectDir, "build/academy/installers/linux/install.sh").readText()
        assertTrue(second.contains("03_avance"), "a material change must re-render the staged plan, never go stale")
    }
}

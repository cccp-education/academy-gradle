package education.cccp.academy.installer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-14 — the generated installer loads the offline archive when it is
 * present.
 *
 * The bundle is optional: without it the script is byte-identical to the
 * networked path (backward compatibility). With it, the script runs
 * `docker load -i` before the learner starts the compose stack.
 */
class InstallerScriptGeneratorOfflineTest {
    private fun platform(
        os: TargetOs,
        offline: Boolean,
    ) = InstallerPlatform(
        os = os,
        applicationName = "academy",
        applicationVersion = "0.0.1",
        javaVersion = "25",
        gradleVersion = "9.7.1",
        composeEnabled = true,
        credentialsEnvPrefix = "ACADEMY_",
        offlineBundleEnabled = offline,
    )

    private fun render(
        os: TargetOs,
        offline: Boolean,
    ): String = InstallerScriptGenerator.render(platform(os, offline)).single().content

    @Test
    fun `unix installer loads the archive when offline mode is enabled`() {
        val script = render(TargetOs.LINUX, offline = true)

        assertTrue(script.contains("docker load -i"))
    }

    @Test
    fun `unix installer stays unchanged when offline mode is disabled`() {
        val script = render(TargetOs.LINUX, offline = false)

        assertFalse(script.contains("docker load -i"))
    }

    @Test
    fun `windows installer loads the archive when offline mode is enabled`() {
        val script = render(TargetOs.WINDOWS, offline = true)

        assertTrue(script.contains("docker load -i"))
    }

    @Test
    fun `windows installer stays unchanged when offline mode is disabled`() {
        val script = render(TargetOs.WINDOWS, offline = false)

        assertFalse(script.contains("docker load -i"))
    }
}

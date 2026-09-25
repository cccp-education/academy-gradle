package education.cccp.academy.installer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-12-2 — the installer writers **provision** the container engine per
 * OS instead of only checking it (D-ACADEMY-12-1/12-5).
 *
 * Matrix (actée S-014, sources vérifiées) : Linux = native Docker Engine from
 * Docker's official apt repository ; macOS = Colima (`brew install colima
 * docker` then `colima start`) ; Windows = Docker Desktop (`install --user
 * --quiet --accept-license --backend=wsl-2`) when WSL2 is present, otherwise
 * `wsl --install` when admin, otherwise an explicit unsupported message.
 *
 * One `install.sh` source (OS header + common tail), one `install.bat`: the
 * compose/seed/env queue is never duplicated (D-ACADEMY-12-5).
 *
 * Baby-step TDD: this file is written before the generator refactor; RED is
 * proven by the absent provisioning fragments.
 */
class InstallerScriptEngineTest {

    private fun platform(
        os: TargetOs,
        composeEnabled: Boolean = true,
    ) = InstallerPlatform(
        os = os,
        applicationName = "academy",
        applicationVersion = "0.0.1",
        javaVersion = "25",
        gradleVersion = "9.7.1",
        composeEnabled = composeEnabled,
        credentialsEnvPrefix = "ACADEMY_",
    )

    private fun render(os: TargetOs): String =
        InstallerScriptGenerator.render(platform(os)).single().content

    @Test
    fun `linux provisions the native Docker Engine from the official apt repository`() {
        val script = render(TargetOs.LINUX)

        assertTrue(script.contains("download.docker.com"), "linux must use Docker's official repository")
        assertTrue(script.contains("docker-ce "), "linux must install the docker-ce package")
        assertTrue(script.contains("docker-ce-cli"), "linux must install docker-ce-cli")
        assertTrue(script.contains("containerd.io"), "linux must install containerd.io")
        assertTrue(script.contains("docker-buildx-plugin"), "linux must install docker-buildx-plugin")
        assertTrue(script.contains("docker-compose-plugin"), "linux must install docker-compose-plugin")
        assertTrue(script.contains("apt-get"), "linux must provision via the apt package manager")
        assertTrue(script.contains("sudo"), "the native engine install requires sudo")
        assertTrue(script.contains("command -v docker"), "provisioning must be guarded by the engine presence")
        assertFalse(script.contains("colima"), "linux never uses Colima (native engine, D-ACADEMY-12-1)")
        assertFalse(
            script.contains("Docker Desktop Installer"),
            "linux never uses Docker Desktop (native engine, D-ACADEMY-12-1)",
        )
    }

    @Test
    fun `macos provisions Colima and never Docker Desktop`() {
        val script = render(TargetOs.MACOS)
        val bootstrap = script.substringBefore("mkdir -p \"\$APP_DIR\"")

        assertTrue(script.contains("brew install colima docker"), "macos must install Colima and the docker CLI")
        assertTrue(script.contains("colima start"), "macos must start the Colima runtime")
        assertTrue(script.contains("command -v docker"), "provisioning must be guarded by the engine presence")
        assertFalse(script.contains("--cask docker"), "Docker Desktop is superseded by Colima (D-ACADEMY-12-1)")
        assertFalse(bootstrap.contains("apt-get"), "the macos engine bootstrap never uses the Linux package manager")
        assertFalse(bootstrap.contains("sudo"), "the Colima install must not require sudo")
    }

    @Test
    fun `the unix installers share the exact same common tail (one source, D-ACADEMY-12-5)`() {
        val linux = render(TargetOs.LINUX)
        val macos = render(TargetOs.MACOS)

        val tail: (String) -> String = { it.substringAfter("mkdir -p \"\$APP_DIR\"") }
        assertEquals(tail(macos), tail(linux), "the compose/seed/env queue must be shared verbatim, never duplicated")
    }

    @Test
    fun `windows probes admin and WSL2 before provisioning Docker Desktop`() {
        val script = render(TargetOs.WINDOWS)

        assertTrue(script.contains("net session"), "windows must probe administrator rights")
        assertTrue(script.contains("wsl --status"), "windows must probe WSL2 presence")
        assertTrue(script.contains("desktop.docker.com"), "windows must download Docker Desktop from the official host")
        assertTrue(script.contains("--user"), "the per-user mode needs no administrator rights")
        assertTrue(script.contains("--accept-license"), "the licence acceptance is scriptable")
        assertTrue(script.contains("--backend=wsl-2"), "the Linux backend must be WSL2")
        assertTrue(script.contains("wsl --install"), "windows without WSL2 must bootstrap it first")
        assertFalse(
            script.contains("GEMINI_API_KEY=sk-"),
            "no credential value is ever embedded (Secrets rule)",
        )
    }

    @Test
    fun `windows admin is required only when WSL2 is absent`() {
        val script = render(TargetOs.WINDOWS)

        assertTrue(script.contains("set IS_ADMIN=1"), "admin rights are recorded as a fact, not an unconditional gate")
        assertTrue(script.contains("set IS_ADMIN=0"), "the absence of admin rights is a fact, not an immediate abort")
    }
}

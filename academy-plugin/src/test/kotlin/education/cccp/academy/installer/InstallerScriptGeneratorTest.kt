package education.cccp.academy.installer

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-1-1 — pure installer script generator, locked by unit tests before
 * any task wiring.
 *
 * The generator renders a deterministic per-platform installer scaffold
 * (pure function, no I/O): host provisioning (Docker bootstrap, Java Temurin,
 * Gradle), idempotence guard, credential env convention, and the embedded
 * `docker-compose.yml` scaffold (Moodle + PostgreSQL + Ollama + Portainer +
 * opencode workspace) as a
 * heredoc when [InstallerPlatform.composeEnabled] is true.
 */
class InstallerScriptGeneratorTest {

    private fun platform(
        os: TargetOs = TargetOs.LINUX,
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

    @Test
    fun `linux install script is a bash script with pinned toolchain`() {
        val files = InstallerScriptGenerator.render(platform(TargetOs.LINUX))

        val script = files.single()
        assertEquals("install.sh", script.relativePath)
        assertTrue(script.content.startsWith("#!/usr/bin/env bash"))
        assertTrue(script.content.contains("JAVA_HOME"))
        assertTrue(script.content.contains("25"))
        assertTrue(script.content.contains("9.7.1"))
        assertTrue(script.content.contains("academy"))
    }

    @Test
    fun `linux script guards idempotence and bootstraps docker`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("docker"), "linux installer must bootstrap Docker")
        assertTrue(script.contains("already installed"), "installer must be idempotent")
    }

    @Test
    fun `compose scaffold is embedded only when enabled`() {
        val withCompose = InstallerScriptGenerator.render(platform(composeEnabled = true)).single().content
        val withoutCompose = InstallerScriptGenerator.render(platform(composeEnabled = false)).single().content

        assertTrue(withCompose.contains("docker-compose.yml"))
        assertTrue(withCompose.contains("moodle"))
        assertTrue(withCompose.contains("postgres"), "compose scaffold must embed postgres as the Moodle database (pilot: postgres over mariadb)")
        assertTrue(withCompose.contains("POSTGRES_PASSWORD"), "postgres service must configure its password via environment")
        assertFalse(withCompose.contains("mariadb"), "mariadb must be fully replaced by postgres")
        assertTrue(withCompose.contains("ollama"), "compose scaffold must embed the ollama service (D-ACADEMY-6-10)")
        assertTrue(withCompose.contains("ollama/ollama"), "ollama service must use the official ollama image")
        assertTrue(withCompose.contains("11434"), "ollama service must expose the model port 11434")
        assertTrue(withCompose.contains("portainer"), "compose scaffold must embed the portainer service replacing the Docker Desktop GUI (pilot decision)")
        assertTrue(withCompose.contains("portainer/portainer"), "portainer must use the official portainer image")
        assertTrue(withCompose.contains("9000"), "portainer must expose its web UI on port 9000")
        assertTrue(withCompose.contains("/var/run/docker.sock"), "portainer must mount the host docker socket")
        assertFalse(withoutCompose.contains("docker-compose.yml"))
    }

    @Test
    fun `credentials never hold values - only the env prefix convention`() {
        val script = InstallerScriptGenerator.render(
            platform(credentialsPrefixShallBe = "MY_ACADEMY_"),
        ).single().content

        assertTrue(script.contains("MY_ACADEMY_"))
        assertFalse(script.contains("SECRET="))
        assertFalse(script.contains("TOKEN="))
    }

    @Test
    fun `windows installer is a batch script with admin check and setx env`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().content

        assertEquals("install.bat", InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().relativePath)
        assertTrue(script.startsWith("@echo off"))
        assertTrue(script.contains("net session"), "windows installer must check admin rights")
        assertTrue(script.contains("setx"), "windows installer must set env vars")
        assertTrue(script.contains("PowerShell"), "windows installer must use PowerShell downloads")
        assertTrue(script.contains("25"))
        assertTrue(script.contains("9.7.1"))
        assertTrue(script.contains("ollama"), "windows compose scaffold must embed the ollama service (parity D-ACADEMY-6-10)")
        assertTrue(script.contains("ollama/ollama"), "windows ollama service must use the official ollama image")
        assertTrue(script.contains("11434"), "windows ollama service must expose the model port 11434")
        assertTrue(script.contains("portainer"), "windows compose scaffold must embed the portainer service replacing Docker Desktop")
        assertTrue(script.contains("portainer/portainer"), "windows portainer must use the official portainer image")
        assertTrue(script.contains("9000"), "windows portainer must expose its web UI on port 9000")
        assertTrue(script.contains("/var/run/docker.sock"), "windows portainer must mount the host docker socket")
        assertTrue(script.contains("postgres"), "windows compose scaffold must embed postgres as the Moodle database (parity)")
        assertTrue(script.contains("POSTGRES_PASSWORD"), "windows postgres service must configure its password via environment")
        assertFalse(script.contains("mariadb"), "windows scaffold must not keep mariadb after the postgres switch")
    }

    @Test
    fun `macos installer uses homebrew docker desktop without sudo`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.MACOS)).single().content

        assertEquals("install.sh", InstallerScriptGenerator.render(platform(TargetOs.MACOS)).single().relativePath)
        assertTrue(script.startsWith("#!/usr/bin/env bash"))
        assertTrue(script.contains("brew install --cask docker"))
        assertTrue(script.contains("uname -m"), "macos installer must detect the architecture")
        assertFalse(script.contains("sudo"), "macos installer must not require sudo")
    }

    @Test
    fun `platform invariants reject blank identity and non numeric java version`() {
        assertThrows<IllegalArgumentException> {
            platform().copy(applicationName = "  ")
        }
        assertThrows<IllegalArgumentException> {
            platform().copy(javaVersion = "latest")
        }
        assertThrows<IllegalArgumentException> {
            platform().copy(gradleVersion = "")
        }
    }

    private fun platform(os: TargetOs = TargetOs.LINUX, composeEnabled: Boolean = true, credentialsPrefixShallBe: String = "ACADEMY_") =
        InstallerPlatform(
            os = os,
            applicationName = "academy",
            applicationVersion = "0.0.1",
            javaVersion = "25",
            gradleVersion = "9.7.1",
            composeEnabled = composeEnabled,
            credentialsEnvPrefix = credentialsPrefixShallBe,
        )
}
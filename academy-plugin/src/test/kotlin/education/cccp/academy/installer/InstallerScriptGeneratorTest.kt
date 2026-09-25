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
        credentialsEnvPrefix: String = "ACADEMY_",
    ) = InstallerPlatform(
        os = os,
        applicationName = "academy",
        applicationVersion = "0.0.1",
        javaVersion = "25",
        gradleVersion = "9.7.1",
        composeEnabled = composeEnabled,
        credentialsEnvPrefix = credentialsEnvPrefix,
    )

    @Test
    fun `linux install script is a bash script with pinned toolchain`() {
        val files = InstallerScriptGenerator.render(platform(TargetOs.LINUX))

        val script = files.single()
        assertEquals("install.sh", script.relativePath)
        assertTrue(script.content.startsWith("#!/usr/bin/env bash"))
        assertTrue(script.content.contains("JAVA_VERSION=\"25\""))
        assertTrue(script.content.contains("GRADLE_VERSION=\"9.7.1\""))
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
        assertTrue(withCompose.contains("portainer/portainer-ce"), "portainer must use the maintained portainer-ce image")
        assertTrue(withCompose.contains("9000"), "portainer must expose its web UI on port 9000")
        assertTrue(withCompose.contains("/var/run/docker.sock"), "portainer must mount the host docker socket")
        assertFalse(withoutCompose.contains("docker-compose.yml"))
    }

    @Test
    fun `compose scaffold declares a dedicated network named volumes and a documented env file`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("networks:"), "compose must declare a dedicated network")
        assertTrue(script.contains("academy-net"), "compose must name the dedicated network")
        assertTrue(script.contains("volumes:"), "compose must declare named volumes")
        assertTrue(script.contains("postgres-data:"), "postgres data must be a named volume")
        assertTrue(script.contains("ollama-models:"), "ollama models must be a named volume")
        assertTrue(script.contains("portainer-data:"), "portainer data must be a named volume")
        assertTrue(script.contains("pg_isready"), "postgres service must ship a healthcheck")
        assertTrue(script.contains(".env"), "compose must document the .env file")
        assertTrue(script.contains("POSTGRES_PASSWORD="), ".env must document POSTGRES_PASSWORD without a value")
        assertTrue(script.contains("DB_TYPE"), "moodle service must be wired to postgres")
        assertTrue(script.contains("pgsql"), "moodle database type must be pgsql")
    }

    @Test
    fun `windows compose scaffold mirrors the complete linux contract`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().content

        assertTrue(script.contains("networks:"), "windows compose must declare the dedicated network (parity)")
        assertTrue(script.contains("academy-net"), "windows compose must name the dedicated network (parity)")
        assertTrue(script.contains("postgres-data:"), "windows postgres data must be a named volume (parity)")
        assertTrue(script.contains("ollama-models:"), "windows ollama models must be a named volume (parity)")
        assertTrue(script.contains("portainer-data:"), "windows portainer data must be a named volume (parity)")
        assertTrue(script.contains("pg_isready"), "windows postgres service must ship a healthcheck (parity)")
        assertTrue(script.contains(".env"), "windows compose must document the .env file (parity)")
        assertTrue(script.contains("POSTGRES_PASSWORD="), "windows .env must document POSTGRES_PASSWORD without a value (parity)")
        assertTrue(script.contains("DB_TYPE"), "windows moodle service must be wired to postgres (parity)")
        assertTrue(script.contains("pgsql"), "windows moodle database type must be pgsql (parity)")
    }

    @Test
    fun `compose declares a one-shot seed service applying the course sql`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("moodle-seed"), "compose must declare the one-shot seed service")
        assertTrue(script.contains("restart: \"no\""), "seed service must be one-shot")
        assertTrue(script.contains("entrypoint"), "seed service must override the container entrypoint")
        assertTrue(script.contains("./seed:/seed"), "seed directory must be mounted into the seed container")
        assertTrue(script.contains("seed/seed.sh"), "seed service must run the idempotent entrypoint")
        assertTrue(script.contains("seed/seed-course.sql"), "seed service must apply the course sql")
    }

    @Test
    fun `seed entrypoint waits for the moodle schema then applies the course seed`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("set -eu"), "seed entrypoint must fail fast")
        assertTrue(script.contains("psql"), "seed entrypoint must use psql")
        assertTrue(script.contains("mdl_course"), "seed entrypoint must wait for the Moodle schema")
        assertTrue(script.contains("until"), "seed entrypoint must poll instead of racing Moodle")
        assertTrue(script.contains("PGPASSWORD"), "seed entrypoint must read the runtime password from the environment")
        assertTrue(script.contains("seed-course.sql"), "seed entrypoint must apply the sql file")
    }

    @Test
    fun `course seed sql is data-only and idempotent`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("mdl_course"), "seed sql must target the Moodle course table")
        assertTrue(script.contains("INSERT INTO"), "seed sql must insert the minimal course")
        assertTrue(script.contains("WHERE NOT EXISTS"), "seed sql must be idempotent")
        assertTrue(script.contains("academy-seed"), "seed sql must use a stable course idnumber")
        listOf("DROP ", "DELETE ", "TRUNCATE ", "ALTER ", "UPDATE ").forEach { destructive ->
            assertFalse(script.contains(destructive), "seed sql must be data-only, found: $destructive")
        }
    }

    @Test
    fun `windows installer writes the seed files with cmd-safe escaping`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().content

        assertTrue(script.contains("moodle-seed"), "windows compose must declare the seed service (parity)")
        assertTrue(script.contains("restart: \"no\""), "windows seed service must be one-shot (parity)")
        assertTrue(script.contains("seed\\seed.sh"), "windows must write the seed entrypoint")
        assertTrue(script.contains("seed\\seed-course.sql"), "windows must write the seed sql")
        assertTrue(script.contains("2^>^&1"), "windows seed lines must escape cmd redirection metacharacters")
        assertTrue(script.contains("mdl_course"), "windows seed must wait for the Moodle schema (parity)")
        assertTrue(script.contains("WHERE NOT EXISTS"), "windows seed sql must be idempotent (parity)")
    }

    @Test
    fun `moodle service uses the real latest image with the actual environment contract`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(
            script.contains("erseco/alpine-moodle:v5.2.3"),
            "moodle must use a real, maintained image (the placeholder moodle:4.5 does not exist)",
        )
        assertFalse(script.contains("moodle:4.5"), "the non-existent placeholder image must be gone")
        listOf("DB_TYPE", "DB_HOST", "DB_NAME", "DB_USER", "DB_PASS").forEach { variable ->
            assertTrue(script.contains(variable), "moodle must wire PostgreSQL via $variable")
        }
        listOf("MOODLE_USERNAME", "MOODLE_PASSWORD", "MOODLE_SITENAME").forEach { variable ->
            assertTrue(script.contains(variable), "moodle auto-install requires $variable")
        }
        assertFalse(
            script.contains("MOODLE_DATABASE_TYPE"),
            "the fake MOODLE_DATABASE_* contract must be replaced by the real DB_* contract",
        )
    }

    @Test
    fun `portainer service uses the maintained community edition image`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("portainer/portainer-ce"), "portainer must use the maintained portainer-ce image")
        assertFalse(
            script.contains("image: portainer/portainer\n"),
            "the obsolete portainer/portainer image must be gone",
        )
    }

    @Test
    fun `portainer ships no healthcheck because its distroless image has no shell`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertFalse(
            script.contains("wget"),
            "portainer-ce is distroless (no wget/sh) - a wget healthcheck is always unhealthy",
        )
    }

    @Test
    fun `workspace service is provisioned with the generated image and the opencode agent (ACADEMY-5)`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(
            script.contains("cccp-education/academy-workspace"),
            "the workspace image must now be a real, generable service (ACADEMY-5)",
        )
        assertTrue(script.contains("build:"), "the workspace image must be built from the generated Dockerfile")
        assertTrue(script.contains("./project:/workspace"), "the learner project must be mounted into the workspace")
        assertTrue(script.contains("Dockerfile"), "the generated Dockerfile must be written by the installer")
        assertTrue(script.contains("opencode.json"), "opencode.json must now be generated (ACADEMY-5)")
        assertTrue(script.contains("AGENTS.md"), "AGENTS.md (opencode anchor) must now be generated (ACADEMY-5)")
    }

    @Test
    fun `env documents the moodle runtime variables without values`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        listOf("POSTGRES_USER=", "MOODLE_USERNAME=", "MOODLE_PASSWORD=", "MOODLE_SITENAME=").forEach { variable ->
            assertTrue(script.contains(variable), ".env must document $variable without a value")
        }
    }

    @Test
    fun `credentials never hold values - only the env prefix convention`() {
        val script = InstallerScriptGenerator.render(
            platform(credentialsEnvPrefix = "MY_ACADEMY_"),
        ).single().content

        assertTrue(script.contains("MY_ACADEMY_"))
        assertFalse(script.contains("SECRET="))
        assertFalse(script.contains("TOKEN="))
    }

    @Test
    fun `windows installer is a batch script with admin check and the docker bootstrap`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().content

        assertEquals("install.bat", InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().relativePath)
        assertTrue(script.startsWith("@echo off"))
        assertTrue(script.contains("net session"), "windows installer must check admin rights")
        assertTrue(script.contains("Get-Command docker"), "windows installer must check the Docker engine")
        assertTrue(script.contains("PowerShell"), "windows installer must use PowerShell")
        assertTrue(script.contains("25"))
        assertTrue(script.contains("9.7.1"))
        assertTrue(script.contains("ollama"), "windows compose scaffold must embed the ollama service (parity D-ACADEMY-6-10)")
        assertTrue(script.contains("ollama/ollama"), "windows ollama service must use the official ollama image")
        assertTrue(script.contains("11434"), "windows ollama service must expose the model port 11434")
        assertTrue(script.contains("portainer"), "windows compose scaffold must embed the portainer service replacing Docker Desktop")
        assertTrue(script.contains("portainer/portainer-ce"), "windows portainer must use the maintained portainer-ce image (parity)")
        assertTrue(script.contains("9000"), "windows portainer must expose its web UI on port 9000")
        assertTrue(script.contains("/var/run/docker.sock"), "windows portainer must mount the host docker socket")
        assertTrue(script.contains("postgres"), "windows compose scaffold must embed postgres as the Moodle database (parity)")
        assertTrue(script.contains("POSTGRES_PASSWORD"), "windows postgres service must configure its password via environment")
        assertFalse(script.contains("mariadb"), "windows scaffold must not keep mariadb after the postgres switch")
    }

    @Test
    fun `macos installer provisions colima without sudo`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.MACOS)).single().content

        assertEquals("install.sh", InstallerScriptGenerator.render(platform(TargetOs.MACOS)).single().relativePath)
        assertTrue(script.startsWith("#!/usr/bin/env bash"))
        assertTrue(script.contains("brew install colima docker"), "macos must provision Colima (D-ACADEMY-12-1)")
        assertTrue(script.contains("colima start"), "macos must start the Colima runtime")
        assertTrue(script.contains("uname -m"), "macos installer must detect the architecture")
        assertFalse(script.contains("sudo"), "macos installer must not require sudo")
    }

    @Test
    fun `linux host bootstrap provisions docker only - the toolchain lives in the workspace image`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("command -v docker"), "linux installer must check the Docker engine")
        assertFalse(script.contains("JAVA_HOME"), "the host must not provision Java (D-ACADEMY-6-10)")
        assertFalse(script.contains("temurin"), "the host must not download Temurin (D-ACADEMY-6-10)")
        assertFalse(script.contains("adoptium"), "the host must not download Adoptium (D-ACADEMY-6-10)")
        assertFalse(script.contains("services.gradle.org"), "the host must not download Gradle (D-ACADEMY-6-10)")
        assertTrue(
            script.contains("FROM gradle:9.7.1-jdk25"),
            "the toolchain lives in the workspace image, never on the host (D-ACADEMY-6-10)",
        )
    }

    @Test
    fun `windows host bootstrap provisions docker only and keeps the admin guard`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().content

        assertTrue(script.contains("net session"), "windows installer must keep the admin check")
        assertTrue(script.contains("Get-Command docker"), "windows installer must check the Docker engine")
        assertFalse(script.contains("adoptium"), "windows must not download Temurin (D-ACADEMY-6-10)")
        assertFalse(script.contains("services.gradle.org"), "windows must not download Gradle (D-ACADEMY-6-10)")
        assertFalse(script.contains("setx JAVA_HOME"), "windows must not set JAVA_HOME on the host (D-ACADEMY-6-10)")
    }

    @Test
    fun `macos host bootstrap provisions colima only without sudo`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.MACOS)).single().content

        assertTrue(script.contains("brew install colima docker"), "macos installer must bootstrap Colima")
        assertTrue(script.contains("colima start"), "macos installer must start Colima")
        assertFalse(script.contains("brew install openjdk"), "macos must not provision Java (D-ACADEMY-6-10)")
        assertFalse(script.contains("brew install gradle"), "macos must not provision Gradle (D-ACADEMY-6-10)")
        assertFalse(script.contains("sudo"), "macos installer must not require sudo")
    }

    @Test
    fun `installers provision the engine or abort with an actionable message`() {
        val linux = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content
        val windows = InstallerScriptGenerator.render(platform(TargetOs.WINDOWS)).single().content
        val macos = InstallerScriptGenerator.render(platform(TargetOs.MACOS)).single().content

        assertTrue(linux.contains("provisioning the Docker Engine"), "linux must provision the engine (D-ACADEMY-12-1)")
        assertTrue(linux.contains("download.docker.com"), "linux must use Docker's official repository")
        assertTrue(macos.contains("Homebrew is required"), "macos must explain Homebrew is required for Colima")
        assertTrue(macos.contains("exit 1"), "macos must abort non-zero when Homebrew is missing")
        assertTrue(
            windows.contains("needs administrator rights"),
            "windows without WSL2 and without admin must explain the WSL2 requirement (D-ACADEMY-12-4)",
        )
        assertTrue(windows.contains("exit /b 1"), "windows must abort non-zero when it cannot provision")
    }

    @Test
    fun `idempotence marker is versioned so a version bump re-provisions`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("INSTALL_ROOT"), "linux installer must define its install root")
        assertTrue(script.contains(".installed-\$APP_VERSION"), "the idempotence marker must be versioned")
        assertTrue(script.contains("already installed"), "a second run must short-circuit as a no-op")
    }

    @Test
    fun `compose scaffold is staged under the application directory`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("APP_DIR=\"\$INSTALL_ROOT/\$APP_NAME\""), "the app dir must be derived from the install root")
        assertTrue(script.contains("mkdir -p \"\$APP_DIR\""), "the app dir must be created before writing")
        assertTrue(script.contains("\$APP_DIR/docker-compose.yml"), "compose must land in the app dir")
        assertTrue(script.contains("\$APP_DIR/.env"), ".env must land in the app dir")
        assertTrue(script.contains("\$APP_DIR/seed/seed.sh"), "seed entrypoint must land in the app seed dir")
    }

    @Test
    fun `opencode configuration is generated with the workspace service (ACADEMY-5)`() {
        val script = InstallerScriptGenerator.render(platform(TargetOs.LINUX)).single().content

        assertTrue(script.contains("opencode.json"), "opencode.json must be generated with the workspace service")
        assertTrue(script.contains("AGENTS.md"), "the learner guide AGENTS.md must be generated")
        assertTrue(script.contains("\$APP_DIR/project/opencode.json"), "opencode config must land in the mounted learner project")
        assertFalse(script.contains("TODO ACADEMY-5"), "the deferred-workspace TODO is resolved by ACADEMY-5")
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
}
package education.cccp.academy.opencode

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * ACADEMY-5-1 — the workspace image definition. The base is the verified
 * `gradle:9.7.1-jdk25` (Ubuntu 26.04, Java 25 + Gradle 9.7.1 already pinned by
 * D-ACADEMY-6-10); `zsh` and `opencode` are installed on top. The official
 * `ghcr.io/anomalyco/opencode` image is Alpine-minimal and cannot run Gradle,
 * so a generated Dockerfile is required (audit S-009).
 */
class WorkspaceDockerfileGeneratorTest {

    private fun platform() = OpenCodeConfig(
        providerUrl = "http://ollama:11434/v1",
        model = "gpt-oss:120b-cloud",
    )

    @Test
    fun `bases the workspace on the pinned gradle image`() {
        val dockerfile = WorkspaceDockerfileGenerator.render(platform())

        assertTrue(dockerfile.contains("FROM gradle:9.7.1-jdk25"), "must base on the pinned gradle image")
    }

    @Test
    fun `installs zsh and opencode and provides a non root learner user`() {
        val dockerfile = WorkspaceDockerfileGenerator.render(platform())

        assertTrue(dockerfile.contains("zsh"), "the workspace session is zsh (D-ACADEMY-1)")
        assertTrue(dockerfile.contains("opencode"), "the workspace ships the opencode agent")
        assertTrue(dockerfile.contains("opencode.ai/install"), "opencode must be installed from the official script")
        assertTrue(dockerfile.contains("learner"), "the workspace must run as a non-root learner user")
    }

    @Test
    fun `installs opencode as the learner so the binary is on the learner path`() {
        val dockerfile = WorkspaceDockerfileGenerator.render(platform())

        val installIndex = dockerfile.indexOf("opencode.ai/install")
        val learnerIndex = dockerfile.lastIndexOf("USER learner")
        assertTrue(
            learnerIndex in 0 until installIndex,
            "the opencode install must run AFTER switching to the learner user (a root install lands in /root/.opencode)",
        )
        assertTrue(dockerfile.contains("ENV HOME=/home/learner"), "the install must target the learner home")
    }

    @Test
    fun `declares the project workdir and the compose command`() {
        val dockerfile = WorkspaceDockerfileGenerator.render(platform())

        assertTrue(dockerfile.contains("WORKDIR /workspace"), "the learner project is mounted at /workspace")
        assertTrue(dockerfile.contains("CMD"), "the image must declare its default command")
    }

    @Test
    fun `never embeds a credential`() {
        val dockerfile = WorkspaceDockerfileGenerator.render(platform())

        listOf("API_KEY=", "SECRET=", "TOKEN=", "PASSWORD=").forEach { secret ->
            assertTrue(!dockerfile.contains(secret), "the Dockerfile must never embed $secret")
        }
    }
}

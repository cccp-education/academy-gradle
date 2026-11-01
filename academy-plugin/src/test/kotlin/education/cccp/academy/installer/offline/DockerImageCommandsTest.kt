package education.cccp.academy.installer.offline

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ACADEMY-14 — the Docker argv builders (pure domain).
 *
 * Every command is a plain `List<String>`: the process seam stays in the
 * adapter, so the exact argv is testable without Docker or a machine.
 */
class DockerImageCommandsTest {
    @Test
    fun `pull targets one reference`() {
        assertEquals(
            listOf("docker", "pull", "postgres:16"),
            DockerImageCommands.pull("postgres:16"),
        )
    }

    @Test
    fun `save writes every reference to one archive`() {
        assertEquals(
            listOf("docker", "save", "-o", "/tmp/offline.tar", "postgres:16", "ollama/ollama:latest"),
            DockerImageCommands.save(
                listOf("postgres:16", "ollama/ollama:latest"),
                "/tmp/offline.tar",
            ),
        )
    }

    @Test
    fun `load reads one archive`() {
        assertEquals(
            listOf("docker", "load", "-i", "/tmp/offline.tar"),
            DockerImageCommands.load("/tmp/offline.tar"),
        )
    }

    @Test
    fun `inspect reads the first repo digest of a reference`() {
        assertEquals(
            listOf(
                "docker",
                "inspect",
                "--format",
                "{{index .RepoDigests 0}}",
                "postgres:16",
            ),
            DockerImageCommands.inspectDigest("postgres:16"),
        )
    }
}

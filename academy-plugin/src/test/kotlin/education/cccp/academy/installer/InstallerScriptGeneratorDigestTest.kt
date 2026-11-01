package education.cccp.academy.installer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-14 — the compose scaffold pins image references to their resolved
 * digests when the offline manifest provides them.
 *
 * Without a digest the line is byte-identical to the historical scaffold
 * (backward compatibility); with a digest the reference becomes
 * `repo:tag@sha256:...`. The untagged compose spellings (`ollama/ollama`,
 * `portainer/portainer-ce`) match the `:latest` manifest key.
 */
class InstallerScriptGeneratorDigestTest {
    private fun platform(digests: Map<String, String>) =
        InstallerPlatform(
            os = TargetOs.LINUX,
            applicationName = "academy",
            applicationVersion = "0.0.1",
            javaVersion = "25",
            gradleVersion = "9.7.1",
            composeEnabled = true,
            credentialsEnvPrefix = "ACADEMY_",
            imageDigests = digests,
        )

    private fun render(digests: Map<String, String>): String = InstallerScriptGenerator.render(platform(digests)).single().content

    @Test
    fun `no digest keeps the historical reference unchanged`() {
        val script = render(emptyMap())

        assertTrue(script.contains("image: postgres:16"))
        assertFalse(script.contains("postgres:16@"))
    }

    @Test
    fun `a digest pins the compose reference`() {
        val digest = "sha256:" + "a".repeat(64)

        val script = render(mapOf("postgres:16" to digest))

        assertTrue(script.contains("image: postgres:16@$digest"))
    }

    @Test
    fun `an untagged compose reference matches its latest digest`() {
        val digest = "sha256:" + "b".repeat(64)

        val script = render(mapOf("ollama/ollama:latest" to digest))

        assertTrue(script.contains("image: ollama/ollama@$digest"))
    }
}

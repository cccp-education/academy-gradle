package education.cccp.academy.installer

import contracts.runtime.LlmProviderKind
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * ACADEMY-16 US-4 — the cloud BYOK profile removes the local Ollama runtime from
 * the compose scaffold, while the local profile keeps it (D-LWA-5).
 */
class InstallerScriptGeneratorCloudProfileTest {
    private fun platform(ollamaEnabled: Boolean) =
        InstallerPlatform(
            os = TargetOs.LINUX,
            applicationName = "academy",
            applicationVersion = "0.0.1",
            javaVersion = "25",
            gradleVersion = "9.7.1",
            composeEnabled = true,
            credentialsEnvPrefix = "ACADEMY_",
            openCodeProvider = LlmProviderKind.OLLAMA_CLOUD,
            openCodeApiKeyEnvVar = "OLLAMA_API_KEY",
            ollamaEnabled = ollamaEnabled,
        )

    @Test
    fun `the cloud profile omits the local ollama runtime`() {
        val script = InstallerScriptGenerator.render(platform(ollamaEnabled = false)).single().content

        assertFalse(script.contains("ollama/ollama"), "the cloud profile must not reference the local image")
        assertFalse(script.contains("ollama-models:"), "the cloud profile must not declare the model volume")
        assertFalse(script.contains("OLLAMA_HOST"), "the cloud profile must not wire the local endpoint")
        assertFalse(script.contains("11434"), "the cloud profile must not expose the local port")
        assertContains(script, "OLLAMA_API_KEY=")
    }

    @Test
    fun `the local profile keeps the embedded runtime`() {
        val script = InstallerScriptGenerator.render(platform(ollamaEnabled = true)).single().content

        assertContains(script, "ollama/ollama")
        assertContains(script, "ollama-models:")
        assertContains(script, "OLLAMA_HOST")
    }
}

package education.cccp.academy.installer

import contracts.runtime.LlmProviderKind
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-7-2 — the generated scaffold carries the BYOK configuration: the
 * `.env` documents the **name** of the key variable (value empty, never
 * committed) and the compose transmits it to the `workspace` service. The
 * value itself never appears anywhere (Secrets rule, D-ACADEMY-7-2/7-8).
 */
class InstallerScriptByokTest {

    private fun platform(
        provider: LlmProviderKind = LlmProviderKind.OLLAMA_LOCAL,
        apiKeyEnvVar: String? = null,
        os: TargetOs = TargetOs.LINUX,
    ) = InstallerPlatform(
        os = os,
        applicationName = "academy",
        applicationVersion = "0.0.1",
        javaVersion = "25",
        gradleVersion = "9.7.1",
        composeEnabled = true,
        credentialsEnvPrefix = "ACADEMY_",
        openCodeEnabled = true,
        openCodeModel = "gpt-oss:120b-cloud",
        openCodeProviderUrl = "http://ollama:11434/v1",
        openCodeProvider = provider,
        openCodeApiKeyEnvVar = apiKeyEnvVar,
    )

    @Test
    fun `the embedded local runtime documents no key variable`() {
        val script = InstallerScriptGenerator.render(platform()).single().content

        assertTrue(script.contains("OLLAMA_HOST"), "the ollama endpoint is still documented")
        assertFalse(script.contains("OLLAMA_API_KEY"), "the embedded runtime requires no key")
        assertFalse(script.contains("GEMINI_API_KEY"), "no unrelated key variable must leak in")
    }

    @Test
    fun `an external provider documents the key variable name without a value`() {
        val script = InstallerScriptGenerator.render(
            platform(provider = LlmProviderKind.GEMINI),
        ).single().content

        assertTrue(script.contains("GEMINI_API_KEY="), ".env must document the key variable name")
        assertFalse(
            script.contains("GEMINI_API_KEY=sk-"),
            "the key value must never be embedded (Secrets rule)",
        )
    }

    @Test
    fun `the compose transmits the key variable to the workspace service`() {
        val script = InstallerScriptGenerator.render(
            platform(provider = LlmProviderKind.GEMINI),
        ).single().content

        assertTrue(script.contains("GEMINI_API_KEY"), "the workspace service must receive the key variable")
    }

    @Test
    fun `a custom provider uses the learner supplied variable name`() {
        val script = InstallerScriptGenerator.render(
            platform(provider = LlmProviderKind.CUSTOM, apiKeyEnvVar = "MY_PROVIDER_KEY"),
        ).single().content

        assertTrue(script.contains("MY_PROVIDER_KEY="), ".env must document the custom variable name")
    }

    @Test
    fun `the script never carries a secret-looking value`() {
        LlmProviderKind.entries.forEach { kind ->
            val script = InstallerScriptGenerator.render(
                platform(
                    provider = kind,
                    apiKeyEnvVar = if (kind == LlmProviderKind.CUSTOM) "MY_PROVIDER_KEY" else null,
                ),
            ).single().content

            listOf("sk-", "ghp_", "gho_", "AIza", "Bearer ").forEach { leaked ->
                assertFalse(script.contains(leaked), "$kind must never embed a credential value")
            }
        }
    }

    @Test
    fun `the windows installer mirrors the key variable contract`() {
        val script = InstallerScriptGenerator.render(
            platform(provider = LlmProviderKind.GEMINI, os = TargetOs.WINDOWS),
        ).single().content

        assertTrue(script.contains("GEMINI_API_KEY="), "windows .env must document the key variable (parity)")
        assertTrue(script.contains("GEMINI_API_KEY"), "windows workspace service must receive the key variable (parity)")
        assertFalse(script.contains("GEMINI_API_KEY=sk-"), "windows must never embed a key value (parity)")
    }

    @Test
    fun `the scaffold documents the bridge only when it is enabled`() {
        val off = InstallerScriptGenerator.render(platform()).single().content
        val on = InstallerScriptGenerator.render(
            InstallerPlatform(
                os = TargetOs.LINUX,
                applicationName = "academy",
                applicationVersion = "0.0.1",
                javaVersion = "25",
                gradleVersion = "9.7.1",
                composeEnabled = true,
                credentialsEnvPrefix = "ACADEMY_",
                bridgeEnabled = true,
                bridgeHost = "127.0.0.1",
                bridgePort = 8765,
            ),
        ).single().content

        assertFalse(off.contains("serveWebhookBridge"), "a disabled bridge must not be documented")
        assertFalse(off.contains("8765"), "a disabled bridge must not leak a port")
        assertTrue(on.contains("serveWebhookBridge"), "an enabled bridge must be documented in the guide")
        assertTrue(on.contains("127.0.0.1:8765"), "the enabled bridge must state its address")
        assertTrue(on.contains("/events/moodle"), "the enabled bridge must document its route")
    }

    @Test
    fun `a custom provider without an env var name fails fast at the platform boundary`() {
        assertThrows<IllegalArgumentException> {
            platform(provider = LlmProviderKind.CUSTOM)
        }
    }
}

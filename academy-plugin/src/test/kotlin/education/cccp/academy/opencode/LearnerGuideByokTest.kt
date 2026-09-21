package education.cccp.academy.opencode

import contracts.runtime.LlmProviderKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-7-1 — the learner guide (`AGENTS.md`) renders the **concrete** BYOK
 * configuration: the provider, the model, and the exact name of the environment
 * variable to export. The learner reads what to do, never a credential
 * (D-ACADEMY-7-10).
 */
class LearnerGuideByokTest {

    @Test
    fun `the embedded local runtime states that no key is required`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(providerUrl = "http://ollama:11434/v1", model = "gpt-oss:120b-cloud"),
        )

        assertTrue(guide.contains("ollama"), "the guide must name the provider")
        assertTrue(guide.contains("gpt-oss:120b-cloud"), "the guide must name the model")
        assertTrue(
            guide.contains("no key", ignoreCase = true),
            "the embedded runtime must state that no key is required",
        )
    }

    @Test
    fun `an external provider names the environment variable to export`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(
                providerUrl = "http://ollama:11434/v1",
                model = "gemini-2.5-flash",
                provider = LlmProviderKind.GEMINI,
            ),
        )

        assertTrue(guide.contains("GEMINI_API_KEY"), "the guide must name the env var to export")
        assertTrue(guide.contains("google"), "the guide must name the opencode provider id")
        assertFalse(guide.contains("no key"), "an external provider does require a key")
    }

    @Test
    fun `a custom env var name is rendered as given`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(
                providerUrl = "https://api.myprovider.com/v1",
                model = "gpt-oss:120b-cloud",
                provider = LlmProviderKind.CUSTOM,
                apiKeyEnvVar = "MY_PROVIDER_KEY",
            ),
        )

        assertTrue(guide.contains("MY_PROVIDER_KEY"))
    }

    @Test
    fun `the guide documents the bridge only when it is enabled`() {
        val off = LearnerGuideGenerator.render(
            OpenCodeConfig(providerUrl = "http://ollama:11434/v1", model = "gpt-oss:120b-cloud"),
        )
        val on = LearnerGuideGenerator.render(
            OpenCodeConfig(
                providerUrl = "http://ollama:11434/v1",
                model = "gpt-oss:120b-cloud",
                bridge = BridgeGuide(host = "127.0.0.1", port = 8765),
            ),
        )

        assertFalse(off.contains("serveWebhookBridge"), "a disabled bridge must not be documented")
        assertTrue(on.contains("serveWebhookBridge"), "an enabled bridge must be documented")
        assertTrue(on.contains("/events/moodle"), "the guide must name the event route")
        assertTrue(on.contains("8765"), "the guide must state the port")
    }

    @Test
    fun `the guide never embeds a credential value`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(
                providerUrl = "http://ollama:11434/v1",
                model = "gemini-2.5-flash",
                provider = LlmProviderKind.GEMINI,
            ),
        )

        listOf("apiKey\": \"sk-", "ghp_", "gho_", "AIza").forEach { leaked ->
            assertFalse(guide.contains(leaked), "the guide must never embed a key value")
        }
    }
}

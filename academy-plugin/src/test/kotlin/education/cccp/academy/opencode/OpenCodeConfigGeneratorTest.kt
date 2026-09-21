package education.cccp.academy.opencode

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * ACADEMY-5-1 — the pure `opencode.json` renderer. The output must be valid,
 * deterministic JSON (no serialization library — D-ACADEMY-5-3) and must
 * never embed a credential.
 */
class OpenCodeConfigGeneratorTest {

    private fun config(
        providerUrl: String = "http://ollama:11434/v1",
        model: String = "gpt-oss:120b-cloud",
    ) = OpenCodeConfig(providerUrl = providerUrl, model = model)

    @Test
    fun `renders the opencode schema and the ollama provider`() {
        val json = OpenCodeConfigGenerator.render(config("http://ollama:11434/v1", "gpt-oss:120b-cloud"))

        assertTrue(json.startsWith("{"))
        assertTrue(json.contains("\"\$schema\": \"https://opencode.ai/config.json\""))
        assertTrue(json.contains("\"model\": \"ollama/gpt-oss:120b-cloud\""))
        assertTrue(json.contains("\"ollama\""))
        assertTrue(json.contains("\"@ai-sdk/openai-compatible\""))
        assertTrue(json.contains("\"baseURL\": \"http://ollama:11434/v1\""))
    }

    @Test
    fun `the rendered json is structurally balanced`() {
        val json = OpenCodeConfigGenerator.render(config())

        assertTrue(json.count { it == '{' } == json.count { it == '}' }, "braces must balance")
        assertTrue(json.count { it == '[' } == json.count { it == ']' }, "brackets must balance")
        assertTrue(!json.contains(",}"), "no trailing comma before a closing brace")
        assertTrue(!json.contains(",]"), "no trailing comma before a closing bracket")
    }

    @Test
    fun `rendering is deterministic and idempotent`() {
        val first = OpenCodeConfigGenerator.render(config())
        val second = OpenCodeConfigGenerator.render(config())

        assertTrue(first == second, "the renderer must be a pure function of its input")
    }

    @Test
    fun `the learner model is configurable`() {
        val json = OpenCodeConfigGenerator.render(config("http://localhost:11434/v1", "qwen3.5:397b-cloud"))

        assertTrue(json.contains("\"model\": \"ollama/qwen3.5:397b-cloud\""))
        assertTrue(json.contains("\"baseURL\": \"http://localhost:11434/v1\""))
    }

    @Test
    fun `the config never embeds a credential`() {
        val json = OpenCodeConfigGenerator.render(config())

        listOf("API_KEY", "apiKey", "SECRET", "TOKEN", "PASSWORD").forEach { secret ->
            assertTrue(!json.contains(secret), "opencode.json must never embed $secret (Secrets rule)")
        }
    }

    @Test
    fun `config invariants reject blank fields`() {
        assertFailsWith<IllegalArgumentException> { config(providerUrl = "  ") }
        assertFailsWith<IllegalArgumentException> { config(model = "") }
    }
}

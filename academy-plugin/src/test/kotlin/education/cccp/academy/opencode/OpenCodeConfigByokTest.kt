package education.cccp.academy.opencode

import contracts.runtime.LlmProviderKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-7-1 — the `opencode.json` renderer is now **multi-provider**, driven
 * by the N0 contract [LlmProviderKind] (via
 * [education.cccp.academy.byok.ByokProviderCatalog]).
 *
 * Backward compatibility (D-ACADEMY-7-5): the default provider is the embedded
 * ollama runtime, so a config built without a provider is byte-identical to
 * the ACADEMY-5 output. A key is only ever referenced by its **environment
 * variable name** (`{env:NAME}`, the official opencode syntax) — never a value.
 */
class OpenCodeConfigByokTest {

    private fun config(
        provider: LlmProviderKind = LlmProviderKind.OLLAMA_LOCAL,
        providerUrl: String = "http://ollama:11434/v1",
        model: String = "gpt-oss:120b-cloud",
        apiKeyEnvVar: String? = null,
    ) = OpenCodeConfig(
        providerUrl = providerUrl,
        model = model,
        provider = provider,
        apiKeyEnvVar = apiKeyEnvVar,
    )

    @Test
    fun `the default provider stays the embedded ollama runtime`() {
        val json = OpenCodeConfigGenerator.render(config())

        assertTrue(json.contains("\"model\": \"ollama/gpt-oss:120b-cloud\""))
        assertTrue(json.contains("\"ollama\": {"))
        assertTrue(json.contains("\"@ai-sdk/openai-compatible\""))
        assertTrue(json.contains("\"baseURL\": \"http://ollama:11434/v1\""))
        assertTrue(json.contains("\"name\": \"Academy Ollama (embedded)\""))
    }

    @Test
    fun `ollama cloud renders the official endpoint and references the key by env var`() {
        val json = OpenCodeConfigGenerator.render(config(provider = LlmProviderKind.OLLAMA_CLOUD))

        assertTrue(json.contains("\"model\": \"ollama-cloud/gpt-oss:120b-cloud\""))
        assertTrue(json.contains("\"ollama-cloud\": {"))
        assertTrue(json.contains("\"baseURL\": \"https://ollama.com/v1\""))
        assertTrue(json.contains("\"apiKey\": \"{env:OLLAMA_API_KEY}\""))
    }

    @Test
    fun `gemini renders the google package and the env var reference`() {
        val json = OpenCodeConfigGenerator.render(
            config(provider = LlmProviderKind.GEMINI, model = "gemini-2.5-flash"),
        )

        assertTrue(json.contains("\"model\": \"google/gemini-2.5-flash\""))
        assertTrue(json.contains("\"google\": {"))
        assertTrue(json.contains("\"@ai-sdk/google\""))
        assertTrue(json.contains("\"apiKey\": \"{env:GEMINI_API_KEY}\""))
        assertFalse(json.contains("baseURL"), "gemini uses the official package endpoint, not a custom base url")
    }

    @Test
    fun `huggingface renders the router endpoint and HF_TOKEN`() {
        val json = OpenCodeConfigGenerator.render(
            config(provider = LlmProviderKind.HUGGINGFACE, model = "meta-llama/Llama-3.3-70B-Instruct"),
        )

        assertTrue(json.contains("\"model\": \"huggingface/meta-llama/Llama-3.3-70B-Instruct\""))
        assertTrue(json.contains("\"huggingface\": {"))
        assertTrue(json.contains("\"baseURL\": \"https://router.huggingface.co/v1\""))
        assertTrue(json.contains("\"apiKey\": \"{env:HF_TOKEN}\""))
    }

    @Test
    fun `custom renders the extension base url and env var name`() {
        val json = OpenCodeConfigGenerator.render(
            config(
                provider = LlmProviderKind.CUSTOM,
                providerUrl = "https://api.myprovider.com/v1",
                apiKeyEnvVar = "MY_PROVIDER_KEY",
            ),
        )

        assertTrue(json.contains("\"model\": \"custom/gpt-oss:120b-cloud\""))
        assertTrue(json.contains("\"custom\": {"))
        assertTrue(json.contains("\"baseURL\": \"https://api.myprovider.com/v1\""))
        assertTrue(json.contains("\"apiKey\": \"{env:MY_PROVIDER_KEY}\""))
    }

    @Test
    fun `a custom provider with a blank env var name fails fast`() {
        assertFailsWith<IllegalArgumentException> {
            config(provider = LlmProviderKind.CUSTOM, apiKeyEnvVar = "  ")
        }
    }

    @Test
    fun `custom requires an explicit base url`() {
        assertFailsWith<IllegalArgumentException> {
            OpenCodeConfig(
                providerUrl = "   ",
                model = "gpt-oss:120b-cloud",
                provider = LlmProviderKind.CUSTOM,
                apiKeyEnvVar = "MY_PROVIDER_KEY",
            )
        }
    }

    @Test
    fun `the default output is byte-identical to the ACADEMY-5 rendering`() {
        val expected = """
            |{
            |  "${'$'}schema": "https://opencode.ai/config.json",
            |  "model": "ollama/gpt-oss:120b-cloud",
            |  "provider": {
            |    "ollama": {
            |      "npm": "@ai-sdk/openai-compatible",
            |      "name": "Academy Ollama (embedded)",
            |      "options": {
            |        "baseURL": "http://ollama:11434/v1"
            |      },
            |      "models": {
            |        "gpt-oss:120b-cloud": {
            |          "name": "gpt-oss:120b-cloud"
            |        }
            |      }
            |    }
            |  },
            |  "instructions": [
            |    "AGENTS.md"
            |  ]
            |}
            |""".trimMargin()

        assertEquals(expected, OpenCodeConfigGenerator.render(config()))
    }

    @Test
    fun `the rendered json is structurally balanced for every provider`() {
        LlmProviderKind.entries.forEach { kind ->
            val extra = when (kind) {
                LlmProviderKind.OLLAMA_LOCAL -> null
                LlmProviderKind.OLLAMA_CLOUD -> null
                LlmProviderKind.GEMINI -> null
                LlmProviderKind.HUGGINGFACE -> null
                LlmProviderKind.CUSTOM -> "MY_PROVIDER_KEY"
            }
            val json = OpenCodeConfigGenerator.render(config(provider = kind, apiKeyEnvVar = extra))
            assertEquals(json.count { it == '{' }, json.count { it == '}' }, "braces must balance for $kind")
            assertEquals(json.count { it == '[' }, json.count { it == ']' }, "brackets must balance for $kind")
            assertFalse(json.contains(",}"), "no trailing comma before a closing brace for $kind")
            assertFalse(json.contains(",]"), "no trailing comma before a closing bracket for $kind")
        }
    }

    @Test
    fun `no provider ever embeds a credential value - only the env reference`() {
        LlmProviderKind.entries.forEach { kind ->
            val extra = if (kind == LlmProviderKind.CUSTOM) "MY_PROVIDER_KEY" else null
            val json = OpenCodeConfigGenerator.render(config(provider = kind, apiKeyEnvVar = extra))
            listOf("\"apiKey\": \"sk-", "\"apiKey\": \"ghp_", "\"apiKey\": \"gho_").forEach { leaked ->
                assertFalse(json.contains(leaked), "$kind must never embed a key value (Secrets rule)")
            }
            if (json.contains("\"apiKey\"")) {
                assertTrue(
                    json.contains("\"apiKey\": \"{env:"),
                    "$kind must reference its key through the official {{env:NAME}} syntax",
                )
            }
        }
    }
}

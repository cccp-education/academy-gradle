package education.cccp.academy.byok

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * ACADEMY-7-1 — the immutable opencode provider description. It is the single
 * shape consumed by [education.cccp.academy.opencode.OpenCodeConfigGenerator]:
 * the provider id opencode routes on, the AI-SDK package, a display name, and
 * *names* (never values) of the base URL and of the API-key environment
 * variable.
 */
class ByokProviderSpecTest {

    @Test
    fun `holds the opencode provider identity`() {
        val spec = ByokProviderSpec(
            id = "google",
            npm = "@ai-sdk/google",
            displayName = "Google Gemini",
            apiKeyEnvVar = "GEMINI_API_KEY",
        )

        assertEquals("google", spec.id)
        assertEquals("@ai-sdk/google", spec.npm)
        assertEquals("Google Gemini", spec.displayName)
        assertNull(spec.baseUrl, "gemini uses the official npm endpoint, not a custom base url")
        assertEquals("GEMINI_API_KEY", spec.apiKeyEnvVar)
    }

    @Test
    fun `base url and api key env var default to null`() {
        val spec = ByokProviderSpec(id = "custom", npm = "@ai-sdk/openai-compatible", displayName = "Custom")

        assertNull(spec.baseUrl)
        assertNull(spec.apiKeyEnvVar)
    }

    @Test
    fun `rejects a blank identity`() {
        assertFailsWith<IllegalArgumentException> { spec(id = "  ") }
        assertFailsWith<IllegalArgumentException> { spec(npm = "") }
        assertFailsWith<IllegalArgumentException> { spec(displayName = "  ") }
    }

    @Test
    fun `rejects a blank base url or api key env var when present`() {
        assertFailsWith<IllegalArgumentException> { spec(baseUrl = "  ") }
        assertFailsWith<IllegalArgumentException> { spec(apiKeyEnvVar = "") }
    }

    private fun spec(
        id: String = "ollama",
        npm: String = "@ai-sdk/openai-compatible",
        displayName: String = "Academy Ollama (embedded)",
        baseUrl: String? = null,
        apiKeyEnvVar: String? = null,
    ) = ByokProviderSpec(
        id = id,
        npm = npm,
        displayName = displayName,
        baseUrl = baseUrl,
        apiKeyEnvVar = apiKeyEnvVar,
    )
}

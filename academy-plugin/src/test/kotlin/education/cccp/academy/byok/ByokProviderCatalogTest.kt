package education.cccp.academy.byok

import contracts.runtime.LlmProviderKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ACADEMY-7-1 — the mapping from the N0 contract [LlmProviderKind] to the real
 * opencode provider description.
 *
 * Every id/npm/endpoint here is **verified against models.dev and the opencode
 * provider documentation** (audit S-010), never invented — cf. the S-007
 * `moodle:4.5` lesson (a plausible-but-inexistent reference that passed tests).
 */
class ByokProviderCatalogTest {

    @Test
    fun `maps every contract provider kind`() {
        LlmProviderKind.entries.forEach { kind ->
            val spec = ByokProviderCatalog.specFor(kind)
            assertTrue(spec.id.isNotBlank(), "$kind must map to an opencode provider id")
            assertTrue(spec.npm.startsWith("@ai-sdk/"), "$kind must map to an AI-SDK package")
            assertTrue(spec.displayName.isNotBlank(), "$kind must have a display name")
        }
    }

    @Test
    fun `the embedded ollama runtime is the default and needs no api key`() {
        val spec = ByokProviderCatalog.specFor(LlmProviderKind.OLLAMA_LOCAL)

        assertEquals("ollama", spec.id)
        assertEquals("@ai-sdk/openai-compatible", spec.npm)
        assertNull(spec.apiKeyEnvVar, "the embedded local runtime never needs a key")
        assertNull(spec.baseUrl, "the base url comes from the extension for OLLAMA_LOCAL")
    }

    @Test
    fun `ollama cloud points at the official endpoint and reads OLLAMA_API_KEY`() {
        val spec = ByokProviderCatalog.specFor(LlmProviderKind.OLLAMA_CLOUD)

        assertEquals("ollama-cloud", spec.id)
        assertEquals("@ai-sdk/openai-compatible", spec.npm)
        assertEquals("https://ollama.com/v1", spec.baseUrl)
        assertEquals("OLLAMA_API_KEY", spec.apiKeyEnvVar)
    }

    @Test
    fun `gemini maps to the official google provider package`() {
        val spec = ByokProviderCatalog.specFor(LlmProviderKind.GEMINI)

        assertEquals("google", spec.id)
        assertEquals("@ai-sdk/google", spec.npm)
        assertNull(spec.baseUrl, "gemini uses the official package endpoint")
        assertEquals("GEMINI_API_KEY", spec.apiKeyEnvVar)
    }

    @Test
    fun `huggingface points at the router endpoint and reads HF_TOKEN`() {
        val spec = ByokProviderCatalog.specFor(LlmProviderKind.HUGGINGFACE)

        assertEquals("huggingface", spec.id)
        assertEquals("@ai-sdk/openai-compatible", spec.npm)
        assertEquals("https://router.huggingface.co/v1", spec.baseUrl)
        assertEquals("HF_TOKEN", spec.apiKeyEnvVar)
    }

    @Test
    fun `custom is an openai-compatible provider resolved from the extension`() {
        val spec = ByokProviderCatalog.specFor(LlmProviderKind.CUSTOM)

        assertEquals("custom", spec.id)
        assertEquals("@ai-sdk/openai-compatible", spec.npm)
        assertNull(spec.baseUrl, "the base url comes from the extension for CUSTOM")
        assertNull(spec.apiKeyEnvVar, "the env var name comes from the extension for CUSTOM")
    }

    @Test
    fun `exposes the catalog keyed by provider so opencode ids never drift`() {
        assertEquals(LlmProviderKind.entries.size, ByokProviderCatalog.specs.size)
        ByokProviderCatalog.specs.forEach { (kind, spec) ->
            assertEquals(spec, ByokProviderCatalog.specFor(kind), "specs must be the single source of truth")
        }
    }
}

package education.cccp.academy.workstation

import contracts.runtime.LlmProviderKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ACADEMY-16 US-4 — the workstation profile resolves to a coherent LLM config
 * and compose decision (D-LWA-5): the cloud profile never points at a local
 * runtime that is no longer started.
 */
class WorkstationProfileResolverTest {
    private fun resolve(
        profile: WorkstationProfile,
        provider: LlmProviderKind = LlmProviderKind.OLLAMA_LOCAL,
        url: String = "http://ollama:11434/v1",
        key: String? = null,
    ) = WorkstationProfileResolver.resolve(profile, provider, url, key)

    @Test
    fun `the local profile keeps the embedded runtime and no key`() {
        val resolved = resolve(WorkstationProfile.LOCAL_OLLAMA)

        assertEquals(LlmProviderKind.OLLAMA_LOCAL, resolved.provider)
        assertEquals("http://ollama:11434/v1", resolved.providerUrl)
        assertNull(resolved.apiKeyEnvVar)
        assertTrue(resolved.ollamaEnabled)
    }

    @Test
    fun `the cloud profile removes the local runtime`() {
        val resolved = resolve(WorkstationProfile.CLOUD_BYOK)

        assertFalse(resolved.ollamaEnabled, "the cloud profile must not start Ollama")
    }

    @Test
    fun `the cloud profile with the local provider falls back to verified ollama cloud`() {
        val resolved = resolve(WorkstationProfile.CLOUD_BYOK, provider = LlmProviderKind.OLLAMA_LOCAL)

        assertEquals(LlmProviderKind.OLLAMA_CLOUD, resolved.provider)
        assertEquals("https://ollama.com/v1", resolved.providerUrl)
        assertEquals("OLLAMA_API_KEY", resolved.apiKeyEnvVar)
        assertFalse(resolved.ollamaEnabled)
    }

    @Test
    fun `the cloud profile keeps an explicitly configured provider and key name`() {
        val resolved =
            resolve(
                WorkstationProfile.CLOUD_BYOK,
                provider = LlmProviderKind.GEMINI,
                key = "GEMINI_API_KEY",
            )

        assertEquals(LlmProviderKind.GEMINI, resolved.provider)
        assertEquals("GEMINI_API_KEY", resolved.apiKeyEnvVar)
        assertFalse(resolved.ollamaEnabled)
    }

    @Test
    fun `the cloud profile keeps a custom provider endpoint`() {
        val resolved =
            resolve(
                WorkstationProfile.CLOUD_BYOK,
                provider = LlmProviderKind.CUSTOM,
                url = "https://api.example.test/v1",
                key = "CUSTOM_API_KEY",
            )

        assertEquals(LlmProviderKind.CUSTOM, resolved.provider)
        assertEquals("https://api.example.test/v1", resolved.providerUrl)
        assertEquals("CUSTOM_API_KEY", resolved.apiKeyEnvVar)
    }
}

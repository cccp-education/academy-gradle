package education.cccp.academy.bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * ACADEMY-8-1 — the resolved bridge configuration. Local-only by construction,
 * never a credential (BYOK stays in the environment).
 */
class BridgeServerConfigTest {

    @Test
    fun `holds the bind address and the descriptive provider`() {
        val config = config()

        assertEquals("127.0.0.1", config.host)
        assertEquals(8765, config.port)
        assertEquals("ollama", config.providerId)
        assertEquals("gpt-oss:120b-cloud", config.model)
    }

    @Test
    fun `rejects an out of range port`() {
        assertFailsWith<IllegalArgumentException> { config(port = 0) }
        assertFailsWith<IllegalArgumentException> { config(port = 70000) }
    }

    @Test
    fun `rejects a blank identity`() {
        assertFailsWith<IllegalArgumentException> { config(host = "  ") }
        assertFailsWith<IllegalArgumentException> { config(providerId = "") }
        assertFailsWith<IllegalArgumentException> { config(model = "  ") }
    }

    private fun config(
        host: String = "127.0.0.1",
        port: Int = 8765,
        providerId: String = "ollama",
        model: String = "gpt-oss:120b-cloud",
    ) = BridgeServerConfig(host = host, port = port, providerId = providerId, model = model)
}

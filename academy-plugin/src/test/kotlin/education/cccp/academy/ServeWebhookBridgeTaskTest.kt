package education.cccp.academy

import education.cccp.academy.bridge.BridgeServerConfig
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * ACADEMY-8-2 — the bridge task resolves its pure configuration from the lazy
 * extension properties, and is guarded so a build never opens a port without an
 * explicit opt-in (D-ACADEMY-8-7/8).
 */
class ServeWebhookBridgeTaskTest {

    private fun applyPlugin(): Project {
        val project = ProjectBuilder.builder().build()
        project.version = "0.0.1"
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    @Test
    fun `resolves the bridge configuration from the extension`() {
        val project = applyPlugin()
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            bridgeHost.set("127.0.0.1")
            bridgePort.set(9001)
        }

        val task = project.tasks.getByName("serveWebhookBridge") as ServeWebhookBridgeTask
        val config: BridgeServerConfig = task.config()

        assertEquals("127.0.0.1", config.host)
        assertEquals(9001, config.port)
        assertEquals("ollama", config.providerId, "the health probe reports the resolved provider id")
        assertEquals("gpt-oss:120b-cloud", config.model)
    }

    @Test
    fun `the health probe follows the configured provider`() {
        val project = applyPlugin()
        project.extensions.getByType(AcademyInstallerExtension::class.java).apply {
            openCodeProvider.set(contracts.runtime.LlmProviderKind.GEMINI)
            openCodeModel.set("gemini-2.5-flash")
        }

        val config = (project.tasks.getByName("serveWebhookBridge") as ServeWebhookBridgeTask).config()

        assertEquals("google", config.providerId, "gemini maps to the google opencode provider")
        assertEquals("gemini-2.5-flash", config.model)
    }

    @Test
    fun `the bridge is disabled by default`() {
        val task = applyPlugin().tasks.getByName("serveWebhookBridge") as ServeWebhookBridgeTask

        assertFalse(task.bridgeEnabled.get(), "a build must not open a port by default")
    }
}

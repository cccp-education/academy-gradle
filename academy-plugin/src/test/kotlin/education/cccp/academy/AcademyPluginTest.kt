package education.cccp.academy

import contracts.runtime.LlmProviderKind
import education.cccp.academy.installer.TargetOs
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ACADEMY-1-0 — the typed `academyInstaller` extension (Property-based) and
 * the four installer tasks (ACADEMY-1-1 registration surface).
 *
 * Baby-step TDD: this test drove the creation of [AcademyInstallerExtension],
 * [TargetOs] and the [AcademyPlugin] task registration. It locks the contract
 * a consuming project sees: defaults present, lazily overridable, four tasks
 * available on apply.
 */
class AcademyPluginTest {

    private fun applyPlugin(version: String = "0.0.1"): Project {
        val project = ProjectBuilder.builder().build()
        project.version = version
        project.pluginManager.apply("education.cccp.academy")
        return project
    }

    @Test
    fun `plugin registers the academyInstaller extension with defaults`() {
        val extension: AcademyInstallerExtension =
            applyPlugin().extensions.getByType(AcademyInstallerExtension::class.java)

        assertEquals("academy", extension.applicationName.get())
        assertEquals("0.0.1", extension.applicationVersion.get())
        assertEquals("25", extension.javaVersion.get())
        assertEquals("9.7.1", extension.gradleVersion.get())
        assertEquals(
            listOf(TargetOs.LINUX, TargetOs.WINDOWS, TargetOs.MACOS),
            extension.targets.get(),
        )
        assertTrue(extension.composeEnabled.get())
        assertEquals("ACADEMY_", extension.credentialsEnvPrefix.get())
        assertTrue(extension.openCodeEnabled.get())
        assertEquals("gpt-oss:120b-cloud", extension.openCodeModel.get())
        assertEquals("http://ollama:11434/v1", extension.openCodeProviderUrl.get())
        assertEquals(LlmProviderKind.OLLAMA_LOCAL, extension.openCodeProvider.get())
        assertTrue(extension.openCodeApiKeyEnvVar.get().isBlank(), "no key variable by default")
        assertFalse(extension.bridgeEnabled.get(), "the bridge must be opt-in (no port opened by default)")
        assertEquals("127.0.0.1", extension.bridgeHost.get(), "the bridge binds locally by default")
        assertEquals(8765, extension.bridgePort.get())
    }

    @Test
    fun `the bridge task is registered and guarded by an opt-in`() {
        val task = applyPlugin().tasks.findByName("serveWebhookBridge")

        assertNotNull(task, "the bridge task must be registered (ACADEMY-8-2)")
        assertEquals("academy", task.group)
    }

    @Test
    fun `plugin registers the three platform generators and the aggregate`() {
        val project = applyPlugin()

        assertNotNull(project.tasks.findByName("generateLinuxInstaller"))
        assertNotNull(project.tasks.findByName("generateWindowsInstaller"))
        assertNotNull(project.tasks.findByName("generateMacInstaller"))
        assertNotNull(project.tasks.findByName("buildAllInstallers"))
    }

    @Test
    fun `extension properties are lazily overridable per project`() {
        val extension: AcademyInstallerExtension =
            applyPlugin().extensions.getByType(AcademyInstallerExtension::class.java)

        extension.applicationName.set("my-academy")
        extension.gradleVersion.set("8.14")
        extension.targets.set(listOf(TargetOs.LINUX))

        assertEquals("my-academy", extension.applicationName.get())
        assertEquals("8.14", extension.gradleVersion.get())
        assertEquals(listOf(TargetOs.LINUX), extension.targets.get())
    }
}
package education.cccp.academy.workstation

import education.cccp.academy.opencode.OpenCodePlugin
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * ACADEMY-16 — the learner Gradle project applies only the **enabled** public
 * boroughs (non-blank version), so the generated build never references an
 * artefact that does not exist (the S-007 `moodle:4.5` lesson).
 */
class WorkstationProjectGeneratorTest {
    private fun plugin(
        id: String,
        version: String = "0.0.1",
    ) = OpenCodePlugin(id = id, role = "role", tasks = listOf("generate"), version = version)

    @Test
    fun `applies the enabled plugins in the generated build`() {
        val files =
            WorkstationProjectGenerator.render(
                listOf(
                    plugin("education.cccp.slider", "0.0.20"),
                    plugin("education.cccp.document", "0.0.30"),
                    plugin("education.cccp.capsule", ""),
                ),
            )

        assertEquals(listOf("project/settings.gradle.kts", "project/build.gradle.kts"), files.map { it.relativePath })
        val build = files.first { it.relativePath.endsWith("build.gradle.kts") }.content
        assertContains(build, "id(\"education.cccp.slider\") version \"0.0.20\"")
        assertContains(build, "id(\"education.cccp.document\") version \"0.0.30\"")
        assertFalse(build.contains("education.cccp.capsule"), "a plugin without version is disabled")
    }

    @Test
    fun `resolves plugins from maven central in settings`() {
        val files = WorkstationProjectGenerator.render(listOf(plugin("education.cccp.slider")))

        val settings = files.first { it.relativePath.endsWith("settings.gradle.kts") }.content
        assertContains(settings, "mavenCentral()")
        assertContains(settings, "gradlePluginPortal()")
        assertContains(settings, "rootProject.name = \"academy-workstation\"")
    }

    @Test
    fun `rejects an empty enabled set`() {
        assertFailsWith<IllegalArgumentException> {
            WorkstationProjectGenerator.render(listOf(plugin("education.cccp.slider", "")))
        }
    }
}

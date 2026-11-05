package education.cccp.academy.opencode

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-16 — the public boroughs are exposed as opencode custom tools.
 *
 * The generator is the executable counterpart of the catalog: one export per
 * enabled plugin, a thin Gradle-task wrapper, and no business logic duplicated
 * from the borough (single source of truth).
 */
class OpenCodeToolGeneratorTest {
    private fun plugin(
        id: String,
        role: String = "role",
        tasks: List<String> = listOf("generate"),
        version: String = "0.0.1",
    ) = OpenCodePlugin(id = id, role = role, tasks = tasks, version = version)

    @Test
    fun `renders one export per enabled plugin`() {
        val file =
            OpenCodeToolGenerator.renderAll(
                listOf(
                    plugin("education.cccp.slider", role = "Generate the slides", tasks = listOf("generateDeck")),
                    plugin("education.cccp.document", role = "Publish the documents", tasks = listOf("bookPipeline")),
                ),
            )

        assertEquals(".opencode/tools/cccp.ts", file.relativePath)
        assertContains(file.content, "export const slider = tool({")
        assertContains(file.content, "export const document = tool({")
        assertContains(file.content, "Generate the slides")
        assertContains(file.content, "\"generateDeck\"")
        assertContains(file.content, "gradle ${'$'}{task}")
    }

    @Test
    fun `skips a plugin without a version`() {
        val file =
            OpenCodeToolGenerator.renderAll(
                listOf(
                    plugin("education.cccp.slider"),
                    plugin("education.cccp.capsule", version = ""),
                ),
            )

        assertContains(file.content, "export const slider")
        assertFalse(file.content.contains("export const capsule"), "a plugin without version is not enabled")
    }

    @Test
    fun `never embeds a credential`() {
        val file = OpenCodeToolGenerator.renderAll(listOf(plugin("education.cccp.bakery")))

        assertFalse(file.content.contains("API_KEY"))
        assertFalse(file.content.contains("SECRET"))
        assertFalse(file.content.contains("PASSWORD"))
    }

    @Test
    fun `rejects an empty enabled set`() {
        assertFailsWith<IllegalArgumentException> {
            OpenCodeToolGenerator.renderAll(listOf(plugin("education.cccp.slider", version = "")))
        }
    }

    @Test
    fun `normalizes the export name to a safe identifier`() {
        assertEquals("slider", OpenCodeToolGenerator.exportName("education.cccp.slider"))
        assertEquals("my_plugin", OpenCodeToolGenerator.exportName("education.cccp.my-plugin"))
        assertTrue(OpenCodeToolGenerator.exportName("education.cccp.slider").matches(Regex("[A-Za-z_][A-Za-z0-9_]*")))
    }
}

package education.cccp.academy.opencode

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * ACADEMY-16 US-3 — the certification workflow names the exact enabled tools and
 * tasks, and never pretends the private `training` borough is exposed (D-LWA-4).
 */
class WorkflowGuideGeneratorTest {
    @Test
    fun `names the exact enabled tools and tasks`() {
        val guide = WorkflowGuideGenerator.render()

        assertContains(guide, "cccp_slider")
        assertContains(guide, "cccp_capsule")
        assertContains(guide, "cccp_document")
        assertContains(guide, "cccp_plantuml")
        assertContains(guide, "generateDeck")
        assertContains(guide, "generateCapsuleVideo")
        assertContains(guide, "bookPipeline")
    }

    @Test
    fun `states the SPG and SPD are the candidate own authored work`() {
        val guide = WorkflowGuideGenerator.render()

        assertContains(guide, "your own work")
        assertFalse(guide.contains("generateSPG"), "the private training task must never be documented as runnable")
        assertFalse(guide.contains("generateSPD"), "the private training task must never be documented as runnable")
    }

    @Test
    fun `skips a disabled plugin`() {
        val plugins =
            OpenCodeCatalog.plugins.map { plugin ->
                if (plugin.id == "education.cccp.slider") plugin.copy(version = "") else plugin
            }

        val guide = WorkflowGuideGenerator.render(plugins)

        assertContains(guide, "(not enabled)")
    }
}

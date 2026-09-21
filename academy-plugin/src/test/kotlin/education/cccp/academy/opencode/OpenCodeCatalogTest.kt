package education.cccp.academy.opencode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ACADEMY-5-1 — the opencode exposure catalog locks the guardrail: only
 * `foundry/public` plugins are ever exposed to the learner. Task names are
 * the verified domain entry points of each public borough (audit S-009 —
 * never invented, cf. the S-007 `moodle:4.5` lesson).
 */
class OpenCodeCatalogTest {

    @Test
    fun `every exposed plugin belongs to the education cccp public namespace`() {
        assertTrue(OpenCodeCatalog.plugins.isNotEmpty())
        OpenCodeCatalog.plugins.forEach { plugin ->
            assertTrue(
                plugin.id.startsWith("education.cccp."),
                "exposed plugin must be a public education.cccp plugin, got: ${plugin.id}",
            )
        }
    }

    @Test
    fun `the catalog never exposes a private borough`() {
        val ids = OpenCodeCatalog.plugins.joinToString(" ") { it.id }
        listOf("training", "edster", "workspace", "waiter", "quizz").forEach { privateBorough ->
            assertFalse(ids.contains(privateBorough), "the private borough $privateBorough must not be exposed")
        }
    }

    @Test
    fun `each exposed plugin carries at least one domain task`() {
        OpenCodeCatalog.plugins.forEach { plugin ->
            assertTrue(plugin.tasks.isNotEmpty(), "${plugin.id} must expose at least one formation task")
            plugin.tasks.forEach { task ->
                assertTrue(
                    task.matches(Regex("[a-zA-Z][a-zA-Z0-9]*")),
                    "task name must be a single camelCase Gradle task, got: $task",
                )
            }
        }
    }

    @Test
    fun `the formation entry points of each content borough are exposed`() {
        assertTrue(OpenCodeCatalog.tasksFor("education.cccp.slider")!!.containsAll(listOf("generateDeck", "generateDeckPipeline")))
        assertTrue(OpenCodeCatalog.tasksFor("education.cccp.capsule")!!.contains("generateCapsuleVideo"))
        assertTrue(OpenCodeCatalog.tasksFor("education.cccp.document")!!.contains("bookPipeline"))
        assertTrue(OpenCodeCatalog.tasksFor("education.cccp.plantuml")!!.contains("generatePlantumlDiagrams"))
        assertTrue(OpenCodeCatalog.tasksFor("education.cccp.graphify")!!.contains("collectAndVerify"))
        assertTrue(OpenCodeCatalog.tasksFor("education.cccp.planner")!!.contains("generatePlan"))
    }

    @Test
    fun `tasksFor returns null for an unknown plugin`() {
        assertNull(OpenCodeCatalog.tasksFor("education.cccp.unknown"))
    }

    @Test
    fun `allTasks flattens the catalog deterministically`() {
        val expected = OpenCodeCatalog.plugins.flatMap { it.tasks }
        assertEquals(expected, OpenCodeCatalog.allTasks)
        assertEquals(OpenCodeCatalog.allTasks, OpenCodeCatalog.allTasks, "catalog must be stable across reads")
    }

    @Test
    fun `plugin invariants reject a blank id and an empty task list`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            OpenCodePlugin(id = "  ", role = "slides", tasks = listOf("generateDeck"))
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            OpenCodePlugin(id = "education.cccp.slider", role = "slides", tasks = emptyList())
        }
    }
}

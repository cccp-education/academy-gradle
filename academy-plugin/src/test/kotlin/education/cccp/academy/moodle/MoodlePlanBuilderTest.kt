package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialArtifactType
import education.cccp.academy.material.MaterialManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-11-1 — the pure plan builder (D-ACADEMY-11-3/11-4/11-7).
 *
 * Academy reads the **structure** of the pulled material (directory = module,
 * file = session, EPIC K pivot = metadata) and turns it into a Moodle import
 * plan. It never reads the `.adoc` content (boundary § 3.3): the file is
 * carried as an opaque path reference.
 *
 * Baby-step TDD: this file drove `MoodleMaterial`, the plan model and
 * `MoodlePlanBuilder`. RED proved by `Unresolved reference`.
 */
class MoodlePlanBuilderTest {

    private val spg = material("SPG/spg.adoc", "SPG")
    private val quiz = material("QUIZ/quiz.adoc", "QUIZ")
    private val document = material("DOCUMENT/livret.pdf", "DOCUMENT")

    private fun material(path: String, type: String): MoodleMaterial =
        MoodleMaterial(
            relativePath = path,
            manifest = MaterialManifest.fromJson(
                """{ "source": "training", "type": "$type", "version": "1.0" }""",
            )!!,
        )

    private fun spd(module: String, file: String): MoodleMaterial =
        material("SPD/$module/$file", "SPD")

    @Test
    fun `an spg becomes a course-level label activity`() {
        val plan = MoodlePlanBuilder.build("academy-seed", "Academy", listOf(spg))

        assertEquals("academy-seed", plan.courseShortName)
        assertEquals(MoodleActivityType.LABEL, plan.sections.single().activities.single().type)
        assertEquals("SPG/spg.adoc", plan.sections.single().activities.single().sourcePath)
    }

    @Test
    fun `each spd module becomes a section and each session a page`() {
        val plan = MoodlePlanBuilder.build(
            "academy-seed",
            "Academy",
            listOf(
                spg,
                spd("01_accueil", "001_bienvenue.adoc"),
                spd("01_accueil", "002_objectifs.adoc"),
                spd("02_coeur", "003_algorithmes.adoc"),
            ),
        )

        val modules = plan.sections.filter { it.module != null }
        assertEquals(2, modules.size)
        assertEquals("01_accueil", modules[0].module)
        assertEquals(2, modules[0].activities.size)
        assertEquals("02_coeur", modules[1].module)
        assertTrue(modules[0].activities.all { it.type == MoodleActivityType.PAGE })
    }

    @Test
    fun `a quiz becomes a quiz activity in its own section`() {
        val plan = MoodlePlanBuilder.build("academy-seed", "Academy", listOf(quiz))

        val activity = plan.sections.single().activities.single()
        assertEquals(MoodleActivityType.QUIZ, activity.type)
    }

    @Test
    fun `a document becomes a resource and never an inlined body`() {
        val plan = MoodlePlanBuilder.build("academy-seed", "Academy", listOf(document))

        val activity = plan.sections.single().activities.single()
        assertEquals(MoodleActivityType.RESOURCE, activity.type)
        assertEquals("DOCUMENT/livret.pdf", activity.sourcePath)
    }

    @Test
    fun `the activity title is derived from the file name, never invented`() {
        val plan = MoodlePlanBuilder.build(
            "academy-seed",
            "Academy",
            listOf(spd("01_accueil", "001_bienvenue.adoc")),
        )

        assertEquals("001 bienvenue", plan.sections.single().activities.single().title)
    }

    @Test
    fun `an unknown producer type is skipped, never guessed`() {
        val unknown = MoodleMaterial(
            relativePath = "MYSTERY/thing.adoc",
            manifest = MaterialManifest.fromJson("""{ "source": "x", "type": "SomeFutureThing" }""")!!,
        )

        val plan = MoodlePlanBuilder.build("academy-seed", "Academy", listOf(unknown))

        assertTrue(plan.sections.isEmpty(), "an unknown type contributes no activity (D-ACADEMY-4-6)")
    }

    @Test
    fun `an empty material produces an empty plan - degraded by default`() {
        val plan = MoodlePlanBuilder.build("academy-seed", "Academy", emptyList())

        assertTrue(plan.isEmpty)
        assertTrue(plan.sections.isEmpty())
    }

    @Test
    fun `the plan never reads the adoc body - only the path`() {
        val plan = MoodlePlanBuilder.build("academy-seed", "Academy", listOf(spg))

        assertEquals(MaterialArtifactType.SPG, spg.manifest.type)
        assertTrue(plan.sections.single().activities.single().sourcePath.endsWith(".adoc"))
    }
}

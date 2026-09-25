package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-11-1 — the plan JSON renderer and the applicator script renderer
 * (D-ACADEMY-11-2/11-9).
 *
 * The plan is the **data** academy injects; the applicator **drives the native
 * tools already present in the image** (`moosh`, `moodle-blueprint`, verified
 * on `erseco/alpine-moodle:v5.2.3`, audit S-013). Academy never reimplements
 * the Moodle API, never writes SQL, and never parses the AsciiDoc body.
 *
 * Baby-step TDD: this file drove [MoodleIngestionScriptGenerator.renderApplicator].
 */
class MoodleIngestionScriptGeneratorTest {

    private val plan = MoodlePlanBuilder.build(
        shortName = "academy-seed",
        fullName = "Academy",
        material = listOf(
            material("SPG/spg.adoc", "SPG"),
            material("SPD/01_accueil/001_bienvenue.adoc", "SPD"),
            material("QUIZ/quiz.adoc", "QUIZ"),
        ),
    )

    private fun material(path: String, type: String): MoodleMaterial =
        MoodleMaterial(
            relativePath = path,
            manifest = MaterialManifest.fromJson(
                """{ "source": "training", "type": "$type", "version": "1.0" }""",
            )!!,
        )

    @Test
    fun `the plan json is balanced and carries the course identity`() {
        val json = MoodleIngestionScriptGenerator.renderPlanJson(plan)

        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
        assertTrue(json.contains("\"courseShortName\": \"academy-seed\""))
        assertTrue(json.contains("\"courseFullName\": \"Academy\""))
        assertTrue(!json.contains(",}"))
        assertTrue(!json.contains(",]"))
    }

    @Test
    fun `the plan json carries one entry per activity with its module activity type`() {
        val json = MoodleIngestionScriptGenerator.renderPlanJson(plan)

        assertTrue(json.contains("\"module\": \"01_accueil\""), "the SPD module must be declared: $json")
        assertTrue(json.contains("\"module\": null"), "the course-level section must be declared")
        assertTrue(json.contains("\"type\": \"label\""))
        assertTrue(json.contains("\"type\": \"page\""))
        assertTrue(json.contains("\"type\": \"quiz\""))
        assertTrue(json.contains("\"sourcePath\": \"SPD/01_accueil/001_bienvenue.adoc\""))
    }

    @Test
    fun `the plan json escapes a quote in a title, never breaking the document`() {
        val quoted = MoodlePlanBuilder.build(
            "academy-seed",
            "Academy",
            listOf(
                MoodleMaterial(
                    relativePath = "SPD/mod/a\"b.adoc",
                    manifest = MaterialManifest.fromJson(
                        """{ "source": "training", "type": "SPD" }""",
                    )!!,
                ),
            ),
        )

        val json = MoodleIngestionScriptGenerator.renderPlanJson(quoted)

        assertTrue(json.contains("\\\""), "a quote in a title must be escaped: $json")
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
    }

    @Test
    fun `the applicator drives the native tools of the image, never a hand-written api`() {
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        assertTrue(script.contains("moosh course-create"), "the script must drive moosh (native tool)")
        assertTrue(script.contains("moosh activity-add"), "activities go through moosh")
        assertTrue(script.contains("-r \"\$SHORTNAME\""), "course creation must be idempotent via -r")
        assertTrue(!script.contains("create_course("), "the script must not reimplement the Moodle API")
        assertTrue(!script.contains("add_moduleinfo("), "the script must not reimplement the Moodle API")
        assertTrue(!script.contains("<?php"), "the script must not be a hand-written PHP stack")
        assertTrue(!script.contains("INSERT INTO mdl_"), "the script must never write SQL directly")
        assertTrue(!script.contains("CREATE TABLE"), "the script must never emit DDL")
    }

    @Test
    fun `the applicator resolves the numeric course id before adding activities`() {
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        assertTrue(script.contains("course-list"), "the numeric id comes from the native course list")
        assertTrue(script.contains("COURSEID"), "activity-add needs the numeric course id (verified S-013)")
    }

    @Test
    fun `the applicator names each module section and never inlines the adoc body`() {
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        assertTrue(script.contains("moosh section-config-set"), "sections are named through moosh")
        assertTrue(script.contains("01_accueil"), "the module name becomes the section name")
        assertTrue(
            script.contains("cat \"\$SRC\""),
            "the content must be read Moodle-side, never embedded by academy",
        )
    }

    @Test
    fun `the applicator fills the right body field per activity type`() {
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        // A page stores its body in `content`, a label in `intro` (verified S-013).
        assertTrue(script.contains("--content=\$CONTENT --contentformat=1"), "a page needs content: $script")
        assertTrue(script.contains("--intro=\$CONTENT --introformat=1"), "a label needs intro")
    }

    @Test
    fun `the applicator carries no credential`() {
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        listOf("PASSWORD=", "SECRET=", "TOKEN=", "API_KEY=").forEach { secret ->
            assertTrue(!script.contains(secret), "the script must never embed $secret")
        }
    }

    @Test
    fun `the applicator shell-quotes a title and stays balanced`() {
        val quoted = MoodlePlanBuilder.build(
            "academy-seed",
            "Academy",
            listOf(
                MoodleMaterial(
                    relativePath = "SPD/mod/a b.adoc",
                    manifest = MaterialManifest.fromJson(
                        """{ "source": "training", "type": "SPD" }""",
                    )!!,
                ),
            ),
        )

        val script = MoodleIngestionScriptGenerator.renderApplicator(quoted)

        assertTrue(script.contains("\"a b\""), "a title with a space is quoted: $script")
    }

    @Test
    fun `the applicator declares the course identity exactly once-quoted`() {
        val script = MoodleIngestionScriptGenerator.renderApplicator(plan)

        // Regression (dogfooding S-013): a double `""value""` broke the shell.
        assertTrue(script.contains("SHORTNAME=\"academy-seed\""), "the shortname must be single-quoted: $script")
        assertTrue(!script.contains("\"\"academy-seed\"\""), "a double-quoted assignment is invalid shell")
        assertTrue(script.contains("FULLNAME=\"Academy\""))
        assertTrue(script.contains("FORMAT=\"topics\""))
    }

    @Test
    fun `the entrypoint guards against replaying an unchanged plan - activities must never duplicate`() {
        val entrypoint = MoodleIngestionScriptGenerator.renderEntrypoint()

        // Regression (dogfooding S-016): `moosh activity-add` does NOT dedupe,
        // so re-running the same plan duplicated every page and label. The guard
        // mirrors the image's own `moodle-blueprint` canonicalHash + `.done`.
        assertTrue(entrypoint.contains("sha256sum"), "the replay guard must key on the plan content: $entrypoint")
        assertTrue(entrypoint.contains(".academy-applied-plan"), "the guard marker must be written by the entrypoint")
        assertTrue(entrypoint.contains("already applied"), "a replay must short-circuit as a no-op")
        assertTrue(entrypoint.contains("/ingest/ingest.sh"), "the entrypoint must delegate to the staged applicator")
        assertTrue(
            entrypoint.contains("no staged material plan - nothing to inject"),
            "the degraded path must stay explicit (D-ACADEMY-11-6)",
        )
        assertTrue(
            entrypoint.contains("/var/www/html/.academy-applied-plan"),
            "the marker must live in the writable shared Moodle volume, never the read-only /ingest bind (S-016)",
        )
    }
}

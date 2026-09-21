package education.cccp.academy.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ACADEMY-4-1 — the manifest is the EPIC K pivot (`metadata.json`) as academy
 * reads it (D-ACADEMY-4-4/4-5). The reader is tolerant: a field added by a
 * producer never breaks the consumer, and a malformed file yields `null` rather
 * than throwing — the verdict, not the parser, decides.
 */
class MaterialManifestTest {

    @Test
    fun `reads the pivot fields`() {
        val manifest = MaterialManifest.fromJson(
            """
            {
              "source": "training",
              "type": "SPG",
              "sessions": 7,
              "generatedAt": "2026-09-21T10:00:00Z",
              "model": "gemma4:31b-cloud",
              "version": "1.0",
              "dependencies": ["codebase"]
            }
            """.trimIndent(),
        )

        val read = checkNotNull(manifest)
        assertEquals("training", read.source)
        assertEquals(MaterialArtifactType.SPG, read.type)
        assertEquals("1.0", read.version)
        assertEquals("gemma4:31b-cloud", read.model)
        assertEquals(listOf("codebase"), read.dependencies)
    }

    @Test
    fun `ignores an unknown field added by a producer`() {
        val manifest = MaterialManifest.fromJson(
            """
            {
              "source": "training",
              "type": "SPD",
              "version": "1.1",
              "futureField": { "anything": true }
            }
            """.trimIndent(),
        )

        val read = checkNotNull(manifest)
        assertEquals(MaterialArtifactType.SPD, read.type)
        assertEquals("1.1", read.version)
    }

    @Test
    fun `an unknown type keeps the raw label and resolves no enum`() {
        val manifest = MaterialManifest.fromJson(
            """{ "source": "x", "type": "SomeFutureThing", "version": "1.0" }""",
        )

        val read = checkNotNull(manifest)
        assertNull(read.type, "an unknown producer type must never be guessed")
        assertEquals("SomeFutureThing", read.rawType)
    }

    @Test
    fun `a malformed json yields null instead of throwing`() {
        assertNull(MaterialManifest.fromJson("not json at all"))
        assertNull(MaterialManifest.fromJson("{ \"source\": }"))
    }

    @Test
    fun `a missing version is tolerated as null`() {
        val manifest = MaterialManifest.fromJson("""{ "source": "training", "type": "SPG" }""")

        val read = checkNotNull(manifest)
        assertEquals(null, read.version)
    }

    @Test
    fun `an optional model and dependencies default to empty`() {
        val manifest = MaterialManifest.fromJson("""{ "source": "training", "type": "SPG" }""")

        val read = checkNotNull(manifest)
        assertEquals("", read.model)
        assertTrue(read.dependencies.isEmpty())
    }
}

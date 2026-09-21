package education.cccp.academy.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ACADEMY-4-1 — the artifact vocabulary is the activated deliverable list
 * (vision § 9.1/9.2), mapped 1:1 from the EPIC K pivot `type` field. An unknown
 * producer type is never guessed: it resolves to `null` and is ignored by the
 * verdict (D-ACADEMY-4-6).
 */
class MaterialArtifactTypeTest {

    @Test
    fun `resolves the activated deliverable types case-insensitively`() {
        assertEquals(MaterialArtifactType.SPG, MaterialArtifactType.fromPivotType("SPG"))
        assertEquals(MaterialArtifactType.SPD, MaterialArtifactType.fromPivotType("spd"))
        assertEquals(MaterialArtifactType.SLIDES, MaterialArtifactType.fromPivotType("slides"))
        assertEquals(MaterialArtifactType.DOCUMENT, MaterialArtifactType.fromPivotType("Document"))
        assertEquals(MaterialArtifactType.CAPSULE, MaterialArtifactType.fromPivotType("capsule"))
        assertEquals(MaterialArtifactType.QUIZ, MaterialArtifactType.fromPivotType("quiz"))
    }

    @Test
    fun `an unknown producer type is never guessed`() {
        assertNull(MaterialArtifactType.fromPivotType("Unknown"))
        assertNull(MaterialArtifactType.fromPivotType("PDF"), "PDF is a render, not the pivot type")
        assertNull(MaterialArtifactType.fromPivotType(""))
    }

    @Test
    fun `every activated deliverable of the vision is modelled`() {
        assertEquals(
            listOf("SPG", "SPD", "SLIDES", "DOCUMENT", "CAPSULE", "QUIZ"),
            MaterialArtifactType.entries.map { it.pivotType },
        )
    }

    @Test
    fun `the pivot type of every entry is non-blank`() {
        MaterialArtifactType.entries.forEach { type ->
            assertTrue(type.pivotType.isNotBlank(), "$type must carry a non-blank pivot type")
        }
    }
}

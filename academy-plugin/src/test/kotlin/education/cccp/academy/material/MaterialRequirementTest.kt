package education.cccp.academy.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * ACADEMY-4-1 — the material requirement is academy's *reading* of the N0
 * `MaterialUpdateContract` (D-ACADEMY-4-1/4-2): a declared remote, an expected
 * version and the artifact types the learner needs. It is a value, never a
 * transport — academy does not pull (the bureau owns git, vision § 3.2).
 */
class MaterialRequirementTest {

    @Test
    fun `holds the remote and the expected artifacts`() {
        val requirement = MaterialRequirement(
            remoteUrl = "https://github.com/cccp-education/formation-fpa",
            currentVersion = "v1.0",
            expectedTypes = listOf(MaterialArtifactType.SPG, MaterialArtifactType.SLIDES),
        )

        assertEquals("https://github.com/cccp-education/formation-fpa", requirement.remoteUrl)
        assertEquals("v1.0", requirement.currentVersion)
        assertEquals(listOf(MaterialArtifactType.SPG, MaterialArtifactType.SLIDES), requirement.expectedTypes)
    }

    @Test
    fun `the version is optional - an unversioned requirement is valid`() {
        val requirement = MaterialRequirement(remoteUrl = "https://example.org/training.git")

        assertEquals(null, requirement.currentVersion)
    }

    @Test
    fun `no artifact type is expected by default`() {
        val requirement = MaterialRequirement(remoteUrl = "https://example.org/training.git")

        assertEquals(emptyList(), requirement.expectedTypes)
    }

    @Test
    fun `rejects a blank remote`() {
        assertFailsWith<IllegalArgumentException> { MaterialRequirement(remoteUrl = "  ") }
    }

    @Test
    fun `rejects a blank version when present`() {
        assertFailsWith<IllegalArgumentException> {
            MaterialRequirement(remoteUrl = "https://example.org/training.git", currentVersion = "  ")
        }
    }
}

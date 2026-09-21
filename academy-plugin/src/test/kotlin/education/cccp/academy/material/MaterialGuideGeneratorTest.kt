package education.cccp.academy.material

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-4-1/4-2 — the material guide declares the **requirement** and the
 * **frozen mechanism** (git pull of the tagged version), never a runnable
 * command that does not exist (D-ACADEMY-4-10): `bureau-gradle pullMaterial` is
 * still BUREAU-3 🔴 À FAIRE (verified S-012). This is the S-007/S-011 lesson
 * applied — cite the mechanism, not an absent binary.
 */
class MaterialGuideGeneratorTest {

    private val requirement = MaterialRequirement(
        remoteUrl = "https://github.com/cccp-education/formation-fpa",
        currentVersion = "v1.0",
        expectedTypes = listOf(MaterialArtifactType.SPG, MaterialArtifactType.SLIDES),
    )

    @Test
    fun `declares the remote and the expected version`() {
        val guide = MaterialGuideGenerator.render(requirement)

        assertTrue(guide.contains("https://github.com/cccp-education/formation-fpa"), "the guide must name the remote")
        assertTrue(guide.contains("v1.0"), "the guide must state the expected version")
    }

    @Test
    fun `states the frozen mechanism as a pull`() {
        val guide = MaterialGuideGenerator.render(requirement)

        assertTrue(guide.contains("git"), "the mechanism is git (frozen, vision S-011)")
        assertTrue(guide.contains("pull"), "the learner pulls - the creator never pushes")
        assertTrue(guide.contains("tag"), "the learner pulls a tagged version")
    }

    @Test
    fun `never cites a command that does not exist yet`() {
        val guide = MaterialGuideGenerator.render(requirement)

        assertFalse(
            guide.contains("pullMaterial"),
            "pullMaterial is not implemented (BUREAU-3 TODO) - it must never be cited as runnable",
        )
        assertFalse(
            guide.contains("./gradlew pullMaterial"),
            "no invented gradle task may be documented",
        )
    }

    @Test
    fun `lists the expected artifact types`() {
        val guide = MaterialGuideGenerator.render(requirement)

        assertTrue(guide.contains("SPG"), "the expected types must be visible to the learner")
        assertTrue(guide.contains("SLIDES"))
        assertTrue(
            guide.contains("capability", ignoreCase = true) || guide.contains("expected", ignoreCase = true),
            "the guide must frame the types as the declared expectation",
        )
    }

    @Test
    fun `an unversioned requirement does not invent a version`() {
        val guide = MaterialGuideGenerator.render(
            MaterialRequirement(remoteUrl = "https://example.org/training.git"),
        )

        assertTrue(guide.contains("https://example.org/training.git"))
        assertFalse(guide.contains("v1.0"))
    }

    @Test
    fun `the guide never carries a credential`() {
        val guide = MaterialGuideGenerator.render(requirement)

        listOf("sk-", "ghp_", "gho_", "AIza", "Bearer ").forEach { leaked ->
            assertFalse(guide.contains(leaked), "the guide must never embed a credential")
        }
    }
}

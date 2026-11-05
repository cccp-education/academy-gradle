package education.cccp.academy.workstation

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * ACADEMY-16 US-1 — the learner workstation manifest is the verifiable identity
 * of the distributed artefact (D-LWA-8): relative paths, SHA-256 digests,
 * deterministic ordering, and no duplicate or escaping path.
 */
class LearnerWorkstationManifestTest {
    private fun file(
        path: String,
        content: String = "content",
    ) = WorkstationFile.of(path, content.toByteArray())

    @Test
    fun `renders a deterministic json with the workstation identity`() {
        val manifest =
            LearnerWorkstationManifest.of(
                workstationVersion = "0.0.3+pilot-0.0.1",
                academyVersion = "0.0.3",
                moduleId = "fpa-pilot",
                moduleVersion = "0.0.1",
                profile = WorkstationProfile.CLOUD_BYOK,
                files = listOf(file("installers/linux/install.sh"), file("README-apprenant.md")),
            )

        val json = manifest.toJson()

        assertContains(json, "\"workstationVersion\": \"0.0.3+pilot-0.0.1\"")
        assertContains(json, "\"academyVersion\": \"0.0.3\"")
        assertContains(json, "\"moduleId\": \"fpa-pilot\"")
        assertContains(json, "\"profile\": \"CLOUD_BYOK\"")
        assertTrue(
            json.indexOf("README-apprenant.md") < json.indexOf("installers/linux/install.sh"),
            "files must be sorted by path in the rendered json",
        )
    }

    @Test
    fun `computes the known sha256 of the content`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            WorkstationFile.sha256("abc".toByteArray()),
        )
    }

    @Test
    fun `records the content size`() {
        val entry = WorkstationFile.of("README.md", "hello".toByteArray())

        assertEquals(5L, entry.size)
        assertEquals("README.md", entry.path)
    }

    @Test
    fun `rejects a blank version`() {
        assertFailsWith<IllegalArgumentException> {
            LearnerWorkstationManifest.of(
                workstationVersion = " ",
                academyVersion = "0.0.3",
                moduleId = "fpa",
                moduleVersion = "0.0.1",
                profile = WorkstationProfile.CLOUD_BYOK,
                files = listOf(file("a.txt")),
            )
        }
    }

    @Test
    fun `rejects an empty file list`() {
        assertFailsWith<IllegalArgumentException> {
            LearnerWorkstationManifest.of(
                workstationVersion = "0.0.3",
                academyVersion = "0.0.3",
                moduleId = "fpa",
                moduleVersion = "0.0.1",
                profile = WorkstationProfile.CLOUD_BYOK,
                files = emptyList(),
            )
        }
    }

    @Test
    fun `rejects duplicate file paths`() {
        assertFailsWith<IllegalArgumentException> {
            LearnerWorkstationManifest(
                workstationVersion = "0.0.3",
                academyVersion = "0.0.3",
                moduleId = "fpa",
                moduleVersion = "0.0.1",
                profile = WorkstationProfile.CLOUD_BYOK,
                files = listOf(file("a.txt"), file("a.txt")),
            )
        }
    }

    @Test
    fun `rejects an absolute or escaping path`() {
        assertFailsWith<IllegalArgumentException> { file("/etc/passwd") }
        assertFailsWith<IllegalArgumentException> { file("../../secret") }
        assertFailsWith<IllegalArgumentException> { file("windows\\path") }
    }

    @Test
    fun `rejects a malformed digest`() {
        assertFailsWith<IllegalArgumentException> {
            WorkstationFile(path = "a.txt", sha256 = "not-a-digest", size = 1)
        }
    }
}

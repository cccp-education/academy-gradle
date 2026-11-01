package education.cccp.academy.installer.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * ACADEMY-14 — parsing `docker inspect --format '{{index .RepoDigests 0}}'`.
 *
 * The command returns `repo@sha256:<64 hex>`; a tool that prints a warning on
 * the same line must not poison the manifest, so the digest is extracted by
 * pattern rather than by splitting the whole line.
 */
class ImageDigestTest {
    @Test
    fun `a repo digest line yields the digest`() {
        val line = "postgres@sha256:" + "a".repeat(64)

        assertEquals("sha256:" + "a".repeat(64), ImageDigest.parse(line))
    }

    @Test
    fun `a bare digest is accepted`() {
        assertEquals("sha256:" + "b".repeat(64), ImageDigest.parse("sha256:" + "b".repeat(64)))
    }

    @Test
    fun `a malformed line yields null`() {
        assertNull(ImageDigest.parse("no digest here"))
        assertNull(ImageDigest.parse("sha256:abc"))
    }
}

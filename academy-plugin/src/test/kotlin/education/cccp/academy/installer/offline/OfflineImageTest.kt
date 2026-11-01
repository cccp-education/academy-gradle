package education.cccp.academy.installer.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * ACADEMY-14 — offline image reference (pure domain).
 *
 * A reference is the tag Docker knows (`repo:tag`); the optional digest pins
 * the exact manifest so an offline bundle cannot silently drift. The pinned
 * form is what the compose file will consume once digest pinning is wired.
 */
class OfflineImageTest {
    @Test
    fun `a reference without a digest is kept as the tag`() {
        val image = OfflineImage("postgres:16")

        assertEquals("postgres:16", image.pinnedReference)
    }

    @Test
    fun `a reference with a digest is pinned`() {
        val image = OfflineImage("postgres:16", "sha256:abc")

        assertEquals("postgres:16@sha256:abc", image.pinnedReference)
    }

    @Test
    fun `a blank digest is ignored`() {
        val image = OfflineImage("postgres:16", " ")

        assertEquals("postgres:16", image.pinnedReference)
    }

    @Test
    fun `a blank reference is rejected`() {
        assertFailsWith<IllegalArgumentException> { OfflineImage(" ") }
    }
}

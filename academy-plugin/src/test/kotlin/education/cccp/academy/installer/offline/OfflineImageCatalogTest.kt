package education.cccp.academy.installer.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-14 — the default image catalog of the academy box.
 *
 * The compose scaffold pulls four services; the workspace image is built from
 * a pinned base. All five must be in the bundle, or the "offline" claim is
 * false on a fresh host.
 */
class OfflineImageCatalogTest {
    @Test
    fun `the catalog covers every image the scaffold needs`() {
        val defaults = OfflineImageCatalog.defaults

        assertTrue(defaults.contains("erseco/alpine-moodle:v5.2.3"), "moodle")
        assertTrue(defaults.contains("postgres:16"), "postgres + seed")
        assertTrue(defaults.contains("ollama/ollama:latest"), "ollama runtime")
        assertTrue(defaults.contains("portainer/portainer-ce:latest"), "portainer")
        assertTrue(defaults.contains("gradle:9.7.1-jdk25"), "workspace base image")
    }

    @Test
    fun `the catalog is unique and non blank`() {
        val defaults = OfflineImageCatalog.defaults

        assertEquals(defaults.size, defaults.distinct().size)
        assertTrue(defaults.none { it.isBlank() })
    }
}

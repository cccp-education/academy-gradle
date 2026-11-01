package education.cccp.academy.installer.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-14 — the bundle manifest (JSON, versioned).
 *
 * The manifest is the proof of what the tarball contains: each image tag and
 * its resolved digest. It is written next to the archive and read by the
 * installer/consumer to diagnose a stale bundle.
 */
class OfflineBundleManifestTest {
    private val images =
        listOf(
            OfflineImage("postgres:16", "sha256:" + "a".repeat(64)),
            OfflineImage("ollama/ollama:latest"),
        )

    @Test
    fun `a manifest round-trips through json`() {
        val manifest = OfflineBundleManifest(version = 1, images = images)

        val json = OfflineBundleManifest.toJson(manifest)
        val parsed = OfflineBundleManifest.fromJson(json)

        assertEquals(manifest, parsed)
    }

    @Test
    fun `the manifest carries the resolved digests`() {
        val json = OfflineBundleManifest.toJson(OfflineBundleManifest(1, images))

        assertTrue(json.contains("sha256:" + "a".repeat(64)))
        assertTrue(json.contains("postgres:16"))
        assertTrue(json.contains("ollama/ollama:latest"))
    }
}

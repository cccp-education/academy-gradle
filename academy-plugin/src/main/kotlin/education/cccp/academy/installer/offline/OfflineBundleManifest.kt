package education.cccp.academy.installer.offline

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ACADEMY-14 — the proof of what the offline archive contains.
 *
 * Written next to the archive (`offline-images.json`) so a consumer can
 * diagnose a stale bundle without loading gigabytes: each image tag and the
 * digest resolved at build time.
 *
 * @property version manifest schema version (bump on breaking change)
 * @property images the archived images, in pull order
 */
@Serializable
data class OfflineBundleManifest(
    val version: Int = 1,
    val images: List<OfflineImage> = emptyList(),
) {
    companion object {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        /** Serializes the manifest deterministically (pretty, stable key order). */
        fun toJson(manifest: OfflineBundleManifest): String = json.encodeToString(manifest)

        /** Parses a manifest, tolerating unknown keys for forward compatibility. */
        fun fromJson(text: String): OfflineBundleManifest = json.decodeFromString(text)
    }
}

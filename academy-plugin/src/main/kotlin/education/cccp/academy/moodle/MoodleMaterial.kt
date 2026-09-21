package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialManifest

/**
 * One artifact of the pulled training material, as academy consumes it for
 * Moodle injection (ACADEMY-11-1, D-ACADEMY-11-4).
 *
 * Academy reads the **structure**, never the content: [relativePath] is the
 * artifact's position in the material tree (an opaque reference — the AsciiDoc
 * body is never read, boundary § 3.3), and [manifest] is the EPIC K
 * `metadata.json` pivot already parsed by the material domain (S-012).
 *
 * @property relativePath artifact path relative to the material root, using
 *   `/` separators (never blank)
 * @property manifest the parsed EPIC K pivot carrying the producer type
 */
data class MoodleMaterial(
    val relativePath: String,
    val manifest: MaterialManifest,
) {
    init {
        require(relativePath.isNotBlank()) { "relativePath must not be blank" }
    }

    /** The file name without its directory, e.g. `001_bienvenue.adoc`. */
    val fileName: String get() = relativePath.substringAfterLast('/')

    /** The artifact type the producer declared, or `null` when unknown. */
    val artifactType get() = manifest.type

    /**
     * The directory segment naming the module, for `SPD` material laid out as
     * `SPD/<module>/<session>.adoc` (training's convention over configuration,
     * `FormationMetadataExtractor`). `null` when the path carries no module.
     */
    val module: String?
        get() {
            val segments = relativePath.split('/')
            if (segments.size < 3) return null
            return segments[segments.size - 2].takeIf { it.isNotBlank() }
        }
}

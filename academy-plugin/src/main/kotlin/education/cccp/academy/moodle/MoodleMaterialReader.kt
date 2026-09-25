package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialManifest
import java.io.File

/**
 * Reads the pulled material tree into [MoodleMaterial] entries (ACADEMY-11).
 *
 * Structural read **only** — the artifact body is never opened (boundary
 * § 3.3): a `metadata.json` pivot (EPIC K) is parsed for its producer type and
 * paired with its sibling artifact path, relative to the material root. A pivot
 * that cannot be parsed is skipped: the reader never decides, the plan does
 * (patron `MaterialInspector`).
 *
 * Shared by `generateMoodleImport` (inspection/dogfooding) and `buildAllInstallers`
 * (the installer stages a self-contained injection), so the read rule lives once.
 */
object MoodleMaterialReader {

    /** The EPIC K pivot file every producing borough writes next to its output. */
    const val PIVOT_FILE_NAME = "metadata.json"

    /**
     * Reads every pivot under [root] and pairs it with the alphabetically first
     * sibling artifact (the only file the producer convention associates with a
     * pivot). Returns an empty list when [root] does not exist.
     */
    fun read(root: File): List<MoodleMaterial> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.name == PIVOT_FILE_NAME }
            .mapNotNull { pivotFile ->
                val manifest = MaterialManifest.fromJson(pivotFile.readText()) ?: return@mapNotNull null
                val artifactDir = pivotFile.parentFile
                val artifact = (artifactDir.listFiles() ?: emptyArray())
                    .filter { it.isFile && it.name != PIVOT_FILE_NAME }
                    .minByOrNull { it.name }
                    ?: return@mapNotNull null
                val relative = artifact.relativeTo(root).invariantSeparatorsPath
                MoodleMaterial(relativePath = relative, manifest = manifest)
            }
            .toList()
    }
}

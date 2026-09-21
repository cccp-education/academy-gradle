package education.cccp.academy.material

/**
 * The pure material inspector (ACADEMY-4-1, D-ACADEMY-4-3/4-7) — a deterministic
 * function from the declared [MaterialRequirement] and the read
 * [MaterialManifest]s to a [MaterialReadiness] verdict. No I/O, no Gradle, no
 * git: unit-testable in isolation.
 *
 * Rules, in order:
 *  - no manifest            -> [MaterialReadiness.Empty] (material not pulled yet)
 *  - an expected type absent -> [MaterialReadiness.Incomplete] (missing types)
 *  - a present type carries a version diverging from the required one
 *                           -> [MaterialReadiness.Incomplete] (outdated)
 *  - otherwise              -> [MaterialReadiness.Ready]
 *
 * An artifact whose producer type is unknown never satisfies a requirement
 * (D-ACADEMY-4-6). An artifact without a version never counts as outdated — the
 * requirement only constrains versions that exist.
 */
object MaterialInspector {

    /** Computes the verdict for [requirement] against [manifests]. */
    fun inspect(
        requirement: MaterialRequirement,
        manifests: List<MaterialManifest>,
    ): MaterialReadiness {
        if (manifests.isEmpty()) return MaterialReadiness.Empty

        val byType = manifests.mapNotNull { manifest ->
            manifest.type?.let { it to manifest }
        }.toMap()

        val missing = requirement.expectedTypes.filterNot { byType.containsKey(it) }

        val outdated = requirement.currentVersion?.let { required ->
            byType.values.any { it.version != null && it.version != required }
        } ?: false

        return if (missing.isEmpty() && !outdated) {
            MaterialReadiness.Ready(presentTypes = byType.keys.toList())
        } else {
            MaterialReadiness.Incomplete(missingTypes = missing, outdated = outdated)
        }
    }
}

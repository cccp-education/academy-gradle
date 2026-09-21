package education.cccp.academy.material

/**
 * The verdict of [MaterialInspector] (ACADEMY-4-1, D-ACADEMY-4-7) — a sealed
 * result instead of a thrown exception.
 *
 * The academy resource is generated **before** the learner pulls the material,
 * so absent material is a legitimate state, never an error: [Empty] is a
 * successful report. A `Ready`/`Incomplete` split lets the task decide whether
 * to fail, without the domain knowing anything about Gradle.
 */
sealed interface MaterialReadiness {

    /** At least one manifest is present and every declared expectation is met. */
    data class Ready(val presentTypes: List<MaterialArtifactType>) : MaterialReadiness

    /**
     * Material is present but an expectation is not met.
     *
     * @property missingTypes expected types no manifest provides
     * @property outdated true when a present artifact carries a version
     *   diverging from the required one
     */
    data class Incomplete(
        val missingTypes: List<MaterialArtifactType>,
        val outdated: Boolean,
    ) : MaterialReadiness

    /** No manifest at all — the material has not been pulled yet. */
    data object Empty : MaterialReadiness
}

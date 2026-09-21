package education.cccp.academy.material

/**
 * Academy's **reading** of the N0 `contracts.runtime.MaterialUpdateContract`
 * (ACADEMY-4-1, D-ACADEMY-4-1/4-2): a declared remote, the expected material
 * version and the artifact types the learner needs.
 *
 * This is a **value, not a transport**: academy never executes git. The vision
 * § 3.2 fixes one owner per verb — the *bureau* syncs the material (git pull),
 * *academy* consumes it pedagogically. The N0 contract stays on the classpath
 * (declared dependency since ACADEMY-7); this type only carries the requirement
 * into the pure domain so the verdict is unit-testable without Gradle or a
 * repository.
 *
 * @property remoteUrl where the versioned material lives (never blank)
 * @property currentVersion the expected version/tag, or `null` when the learner
 *   accepts any version
 * @property expectedTypes the artifact types that must be present; empty means
 *   "material presence only, no type expectation"
 */
data class MaterialRequirement(
    val remoteUrl: String,
    val currentVersion: String? = null,
    val expectedTypes: List<MaterialArtifactType> = emptyList(),
) {
    init {
        require(remoteUrl.isNotBlank()) { "remoteUrl must not be blank" }
        require(currentVersion == null || currentVersion.isNotBlank()) {
            "currentVersion must not be blank when present"
        }
    }
}

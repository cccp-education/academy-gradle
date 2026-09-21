package education.cccp.academy.material

/**
 * The artifact vocabulary academy understands when reading versioned training
 * material (ACADEMY-4-1) — the activated deliverable list of the formation
 * pipeline (VISION_PIPELINE_FORMATION § 9.1/9.2: SPG, SPD, slides, document,
 * capsules, quiz).
 *
 * The [pivotType] is the exact label producers write in the EPIC K
 * `metadata.json` pivot (`TAXONOMIE_WORKSPACE.adoc` § Format Pivot). Mapping is
 * 1:1 and **an unknown producer type is never guessed** — it resolves to `null`
 * and is ignored by the verdict (D-ACADEMY-4-6), so a new borough type never
 * silently satisfies a requirement.
 *
 * @property pivotType the normalized label written by producers
 */
enum class MaterialArtifactType(val pivotType: String) {
    SPG("SPG"),
    SPD("SPD"),
    SLIDES("SLIDES"),
    DOCUMENT("DOCUMENT"),
    CAPSULE("CAPSULE"),
    QUIZ("QUIZ"),
    ;

    companion object {
        /** The type matching [raw] case-insensitively, or `null` when unknown. */
        fun fromPivotType(raw: String): MaterialArtifactType? =
            entries.firstOrNull { it.pivotType.equals(raw.trim(), ignoreCase = true) }
    }
}

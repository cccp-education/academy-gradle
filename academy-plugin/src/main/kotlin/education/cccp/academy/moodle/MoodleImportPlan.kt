package education.cccp.academy.moodle

/**
 * The Moodle import plan (ACADEMY-11-1) — the **data structure** academy
 * injects (D-ACADEMY-11-2/11-9).
 *
 * This is the stable contract of the ingestion: the generic CLI script
 * ([MoodleIngestionScriptGenerator]) is identical for every formation — only
 * this plan varies with the material. It carries no executable logic, which is
 * what makes it inspectable, diffable and dogfoodable without a running Moodle.
 *
 * @property courseShortName the Moodle course shortname to target, idempotency
 *   key of the imported course (never blank)
 * @property courseFullName the human-readable course name (never blank)
 * @property courseFormat the Moodle course format, `topics` by default
 * @property sections the ordered sections and their activities
 */
data class MoodleImportPlan(
    val courseShortName: String,
    val courseFullName: String,
    val courseFormat: String = "topics",
    val sections: List<MoodleSection> = emptyList(),
) {
    init {
        require(courseShortName.isNotBlank()) { "courseShortName must not be blank" }
        require(courseFullName.isNotBlank()) { "courseFullName must not be blank" }
        require(courseFormat.isNotBlank()) { "courseFormat must not be blank" }
    }

    /** True when the plan carries no activity — nothing to inject (D-ACADEMY-11-6). */
    val isEmpty: Boolean get() = sections.all { it.activities.isEmpty() }

    /** Total number of activities across every section (deterministic count). */
    val activityCount: Int get() = sections.sumOf { it.activities.size }
}

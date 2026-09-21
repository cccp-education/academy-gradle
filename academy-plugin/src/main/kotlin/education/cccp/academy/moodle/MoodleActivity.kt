package education.cccp.academy.moodle

/**
 * One activity the injection creates in Moodle (ACADEMY-11-1).
 *
 * The [sourcePath] is a **reference**, never a body: academy transports the
 * pulled artifact (boundary § 3.3 — it does not parse the AsciiDoc). The
 * generic CLI script the plan feeds resolves the path against the mounted
 * material directory at application time (D-ACADEMY-11-9).
 *
 * @property type the real Moodle activity the artifact becomes
 * @property title activity title shown in the course, derived from the file
 *   name (never invented from content)
 * @property sourcePath artifact path relative to the material root
 */
data class MoodleActivity(
    val type: MoodleActivityType,
    val title: String,
    val sourcePath: String,
) {
    init {
        require(title.isNotBlank()) { "activity title must not be blank" }
        require(sourcePath.isNotBlank()) { "activity sourcePath must not be blank" }
    }
}

/**
 * One course section of the import plan (ACADEMY-11-1).
 *
 * [module] is the training module name for `SPD` material (one section per
 * module, training's convention); it is `null` for course-level sections that
 * gather artifacts without a module (the SPG label, the quiz), which Moodle
 * receives as section 0.
 *
 * @property module training module name, or `null` for a course-level section
 * @property activities activities to create in this section, in order
 */
data class MoodleSection(
    val module: String?,
    val activities: List<MoodleActivity>,
) {
    init {
        require(module == null || module.isNotBlank()) { "module must not be blank when present" }
    }
}

package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialArtifactType

/**
 * The Moodle activity vocabulary academy targets when injecting material
 * (ACADEMY-11-1, D-ACADEMY-11-7).
 *
 * The [moduleName] is the **real** Moodle module directory name, verified on
 * the shipping image `erseco/alpine-moodle:v5.2.3` (`public/mod`, audit
 * S-013) — never an invented label. Academy generates a plan for the official
 * `create_course()` / `add_moduleinfo()` PHP API (D-ACADEMY-11-2), so it must
 * speak Moodle's own vocabulary, not its own.
 *
 * @property moduleName the Moodle module directory / activity type name
 */
enum class MoodleActivityType(val moduleName: String) {
    LABEL("label"),
    PAGE("page"),
    QUIZ("quiz"),
    RESOURCE("resource"),
    URL("url"),
    ;

    companion object {
        /** The activity matching [moduleName] exactly, or `null` when unknown. */
        fun fromModuleName(raw: String): MoodleActivityType? =
            entries.firstOrNull { it.moduleName.equals(raw.trim(), ignoreCase = true) }
    }
}

/**
 * Total mapping from the material vocabulary [MaterialArtifactType] to a real
 * Moodle activity (ACADEMY-11-1, D-ACADEMY-11-7).
 *
 * The mapping is **total over the known material types** (SPG, SPD, SLIDES,
 * DOCUMENT, CAPSULE, QUIZ): every type academy knows how to inspect, it also
 * knows how to present. A producer type unknown to [MaterialArtifactType]
 * never reaches here — it is skipped upstream (D-ACADEMY-4-6).
 *
 * Rationale per type:
 *  - `SPG` — the global scenario becomes a course-level presentation label
 *  - `SPD` — a detailed scenario is the session **page** content
 *  - `QUIZ` — an evaluation becomes a Moodle quiz
 *  - `DOCUMENT` — the learner document (PDF/EPUB) is a downloadable resource
 *  - `SLIDES` / `CAPSULE` — an external artifact the learner opens, so a link
 */
object MoodleActivityMapping {

    /** The Moodle activity that presents [type]. */
    fun forMaterial(type: MaterialArtifactType): MoodleActivityType = when (type) {
        MaterialArtifactType.SPG -> MoodleActivityType.LABEL
        MaterialArtifactType.SPD -> MoodleActivityType.PAGE
        MaterialArtifactType.QUIZ -> MoodleActivityType.QUIZ
        MaterialArtifactType.DOCUMENT -> MoodleActivityType.RESOURCE
        MaterialArtifactType.SLIDES,
        MaterialArtifactType.CAPSULE,
        -> MoodleActivityType.URL
    }
}

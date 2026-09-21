package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialArtifactType

/**
 * The pure plan builder (ACADEMY-11-1, D-ACADEMY-11-3/11-4/11-7) — a
 * deterministic function from the pulled material to the Moodle import plan.
 * No I/O, no Gradle, no Moodle, no PHP: unit-testable in isolation.
 *
 * Rules:
 *  - an artifact whose producer type is unknown to [MaterialArtifactType] is
 *    **skipped**, never guessed (D-ACADEMY-4-6) — it contributes no activity
 *  - `SPG` and `QUIZ` gather into the **course-level section** (`module = null`)
 *  - `SPD` artifacts group **one section per module** (training's convention:
 *    the parent directory names the module)
 *  - every other known type lands in the course-level section as well, so a
 *    document or a slide deck is never lost
 *  - the title is derived from the **file name**, never from content:
 *    `001_bienvenue.adoc` becomes `001 bienvenue`
 *
 * @see MoodleImportPlan
 */
object MoodlePlanBuilder {

    /** Builds the plan for [shortName]/[fullName] from the [material] list. */
    fun build(
        shortName: String,
        fullName: String,
        material: List<MoodleMaterial>,
    ): MoodleImportPlan {
        val courseLevel = mutableListOf<MoodleActivity>()
        val modules = linkedMapOf<String, MutableList<MoodleActivity>>()

        for (artifact in material) {
            val type = artifact.artifactType ?: continue
            val activity = MoodleActivity(
                type = MoodleActivityMapping.forMaterial(type),
                title = titleOf(artifact),
                sourcePath = artifact.relativePath,
            )
            if (type == MaterialArtifactType.SPD) {
                val module = artifact.module ?: COURSE_LEVEL_MODULE
                modules.getOrPut(module) { mutableListOf() } += activity
            } else {
                courseLevel += activity
            }
        }

        val sections = buildList {
            if (courseLevel.isNotEmpty()) add(MoodleSection(module = null, activities = courseLevel))
            modules.forEach { (module, activities) ->
                add(MoodleSection(module = module, activities = activities))
            }
        }

        return MoodleImportPlan(
            courseShortName = shortName,
            courseFullName = fullName,
            sections = sections,
        )
    }

    /** `001_bienvenue.adoc` -> `001 bienvenue` (extension dropped, underscores spaced). */
    private fun titleOf(artifact: MoodleMaterial): String {
        val base = artifact.fileName.substringBeforeLast('.')
        return base.replace('_', ' ').trim().ifEmpty { artifact.fileName }
    }

    /** Module bucket for an `SPD` artifact whose path carries no module directory. */
    private const val COURSE_LEVEL_MODULE = "course"
}

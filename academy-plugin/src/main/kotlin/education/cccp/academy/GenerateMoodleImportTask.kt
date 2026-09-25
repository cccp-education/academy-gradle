package education.cccp.academy

import education.cccp.academy.moodle.MoodleImportPlan
import education.cccp.academy.moodle.MoodleIngestionScriptGenerator
import education.cccp.academy.moodle.MoodleMaterialReader
import education.cccp.academy.moodle.MoodlePlanBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Generates the Moodle material injection artifacts (ACADEMY-11-2,
 * D-ACADEMY-11-5/11-6/11-9) — thin-wrapper task (pattern codex
 * `CollectPageProvenanceTask`): every Gradle resolution lives here, the plan is
 * built by the pure [MoodlePlanBuilder].
 *
 * It reads the **structure** of the pulled material (directory = module, file =
 * session, EPIC K pivot = metadata) and writes into `outputDir`:
 *
 *  - `plan.json` — the **data** academy injects, built by
 *    [MoodleIngestionScriptGenerator.renderPlanJson];
 *  - `ingest.sh` — the **generic** Moodle CLI applicator, identical for every
 *    formation (D-ACADEMY-11-9).
 *
 * Academy never reads the AsciiDoc body (boundary § 3.3): the artifact is
 * carried as a path reference and read Moodle-side by the script. Degraded by
 * default (D-ACADEMY-11-6): an empty material directory writes nothing.
 */
@DisableCachingByDefault(because = "Reads the learner material directory, which lives outside the build cache (ACADEMY-11-2)")
abstract class GenerateMoodleImportTask : DefaultTask() {

    /** Opt-in switch — mirrors `academyInstaller.moodleImportEnabled`. */
    @get:Input
    abstract val moodleImportEnabled: Property<Boolean>

    /** Directory holding the pulled material and its EPIC K pivots. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    @get:Optional
    abstract val materialDir: DirectoryProperty

    /** Shortname of the target Moodle course (idempotency key). */
    @get:Input
    abstract val courseShortName: Property<String>

    /** Human-readable full name of the target course. */
    @get:Input
    abstract val courseFullName: Property<String>

    /** Output directory for `plan.json` and `ingest.sh`. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val plan = buildPlan()
        if (plan.isEmpty) {
            logger.lifecycle("[academy] no training material to inject - nothing to generate (D-ACADEMY-11-6)")
            return
        }

        val dir = outputDir.get().asFile.apply { mkdirs() }
        val planFile = File(dir, "plan.json").apply { writeText(MoodleIngestionScriptGenerator.renderPlanJson(plan)) }
        val scriptFile = File(dir, "ingest.sh").apply { writeText(MoodleIngestionScriptGenerator.renderApplicator(plan)) }
        logger.lifecycle(
            "[academy] moodle import generated - ${plan.activityCount} activities across " +
                "${plan.sections.size} sections -> ${planFile.absolutePath}, ${scriptFile.absolutePath}",
        )
    }

    /**
     * Builds the pure plan from the material directory (D-ACADEMY-11-3). An
     * absent directory yields an empty plan — the resource is generated before
     * the learner pulls, so absent material is a legitimate state, never an
     * error (patron D-ACADEMY-4-7).
     */
    fun buildPlan(): MoodleImportPlan {
        val dir = materialDir.orNull?.asFile
        val material = if (dir != null) MoodleMaterialReader.read(dir) else emptyList()
        return MoodlePlanBuilder.build(
            shortName = courseShortName.get(),
            fullName = courseFullName.get(),
            material = material,
        )
    }
}

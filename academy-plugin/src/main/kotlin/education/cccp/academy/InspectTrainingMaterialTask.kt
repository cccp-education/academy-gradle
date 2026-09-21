package education.cccp.academy

import education.cccp.academy.material.MaterialArtifactType
import education.cccp.academy.material.MaterialInspector
import education.cccp.academy.material.MaterialManifest
import education.cccp.academy.material.MaterialReadiness
import education.cccp.academy.material.MaterialRequirement
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Verifies the training material present in the learner workspace against the
 * declared requirement (ACADEMY-4-2) — thin-wrapper task (pattern codex
 * `CollectPageProvenanceTask`): all Gradle resolution lives here, the verdict is
 * computed by the pure [MaterialInspector].
 *
 * The requirement is whatever the N0 `MaterialUpdateContract` means for academy
 * (D-ACADEMY-4-1/4-2). The transport (git pull) belongs to the **bureau** —
 * academy never executes git.
 *
 * Failure policy (D-ACADEMY-4-9): the resource is generated *before* the learner
 * pulls, so absent material without a declared requirement is a legitimate
 * `Empty` report and the task succeeds. It fails only when a requirement is
 * declared and the material does not satisfy it.
 */
@DisableCachingByDefault(because = "Reads the learner material directory, which lives outside the build cache (ACADEMY-4-2)")
abstract class InspectTrainingMaterialTask : DefaultTask() {

    /** Declared remote of the versioned material — blank means "no requirement". */
    @get:Input
    abstract val materialRemoteUrl: Property<String>

    /** Expected material version, blank when the learner accepts any version. */
    @get:Optional
    @get:Input
    abstract val materialCurrentVersion: Property<String>

    /** Artifact types the requirement expects. */
    @get:Input
    abstract val materialExpectedTypes: ListProperty<MaterialArtifactType>

    /** Directory holding the pulled material and its EPIC K pivots. */
    @get:Internal
    abstract val materialDir: DirectoryProperty

    init {
        group = "academy"
        description = "Verifies the pulled training material against the declared requirement (read-only)"
    }

    @TaskAction
    fun inspectMaterial() {
        val requirement = requirement()
        val readiness = inspect()
        when (readiness) {
            is MaterialReadiness.Empty -> {
                if (hasRequirement()) {
                    throw GradleException(
                        "[academy] material declared for ${requirement.remoteUrl} but none found in " +
                            "${materialDir.get().asFile.absolutePath} - pull the tagged version first",
                    )
                }
                logger.lifecycle("[academy] no training material requirement declared and none present - nothing to verify")
            }
            is MaterialReadiness.Incomplete -> throw GradleException(
                "[academy] training material is incomplete - " + describe(requirement, readiness),
            )
            is MaterialReadiness.Ready -> logger.lifecycle(
                "[academy] training material satisfies the requirement - " +
                    "present: ${readiness.presentTypes.joinToString(", ") { it.pivotType }}",
            )
        }
    }

    /**
     * True when the extension declares a material expectation (D-ACADEMY-4-9) —
     * either a remote to pull from, or expected artifact types. A consumer that
     * names the artifacts it needs has declared a requirement even before it
     * pins the remote, so the verification must not silently ignore it.
     */
    fun hasRequirement(): Boolean =
        materialRemoteUrl.get().isNotBlank() || materialExpectedTypes.get().isNotEmpty()

    /** The resolved, pure requirement — testable without running the task. */
    fun requirement(): MaterialRequirement = MaterialRequirement(
        remoteUrl = materialRemoteUrl.get().ifBlank { "unset" },
        currentVersion = materialCurrentVersion.get().ifBlank { null },
        expectedTypes = materialExpectedTypes.get(),
    )

    /**
     * Reads every `*.json` manifest under [materialDir] and computes the verdict.
     * Unreadable files are skipped — the parser never decides, the verdict does.
     */
    fun inspect(): MaterialReadiness {
        val dir = materialDir.get().asFile
        val manifests: List<MaterialManifest> =
            if (dir.isDirectory) {
                dir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".json") }
                    .mapNotNull { MaterialManifest.fromJson(it.readText()) }
                    .toList()
            } else {
                emptyList()
            }
        return MaterialInspector.inspect(requirement(), manifests)
    }

    private fun describe(requirement: MaterialRequirement, readiness: MaterialReadiness.Incomplete): String =
        buildList {
            if (readiness.missingTypes.isNotEmpty()) {
                add("missing ${readiness.missingTypes.joinToString(", ") { it.pivotType }}")
            }
            if (readiness.outdated) {
                add("outdated (expected version ${requirement.currentVersion})")
            }
        }.joinToString("; ")
}

package education.cccp.academy

import education.cccp.academy.installer.InstallerPlatform
import education.cccp.academy.installer.InstallerScriptGenerator
import education.cccp.academy.installer.TargetOs
import education.cccp.academy.material.MaterialRequirement
import education.cccp.academy.moodle.MoodleImportPlan
import education.cccp.academy.moodle.MoodleMaterialReader
import education.cccp.academy.moodle.MoodlePlanBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Thin-wrapper task (pattern codex `CollectPageProvenanceTask`): renders the
 * installer for one [targetOs] via the pure [InstallerScriptGenerator] and
 * writes the scripts into `outputDir/<os.dirName>/`.
 *
 * All Gradle-specific resolution (lazy properties, provider plumbing) lives
 * here; the rendering logic stays in the domain, unit-testable without Gradle.
 */
@DisableCachingByDefault(because = "Script scaffolds re-render each run; up-to-date checks are enough (ACADEMY-1-1)")
abstract class GenerateInstallerTask : DefaultTask() {

    /** Target platform produced by this task instance (LINUX/WINDOWS/MACOS). */
    @get:Input
    abstract val targetOs: Property<TargetOs>

    /** The `academyInstaller` extension — read through [Internal] to keep wiring lazy. */
    @get:Internal
    abstract val installerExtension: Property<AcademyInstallerExtension>

    /**
     * The pulled material tree staged into the installer when injection is on
     * (ACADEMY-11-4). Declared as a **tolerant** input (pattern CDX-CONTEXT-
     * HARDENING S-221): a plain re-run must re-render a changed plan, never go
     * stale, yet an absent directory stays a legitimate state (D-ACADEMY-11-6).
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val moodleMaterialFiles: ConfigurableFileCollection

    /** Base output directory; the platform subdirectory is `<dirName>` on top. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val extension = installerExtension.get()
        val platform = InstallerPlatform(
            os = targetOs.get(),
            applicationName = extension.applicationName.get(),
            applicationVersion = extension.applicationVersion.get(),
            javaVersion = extension.javaVersion.get(),
            gradleVersion = extension.gradleVersion.get(),
            composeEnabled = extension.composeEnabled.get(),
            credentialsEnvPrefix = extension.credentialsEnvPrefix.get(),
            openCodeEnabled = extension.openCodeEnabled.get(),
            openCodeModel = extension.openCodeModel.get(),
            openCodeProviderUrl = extension.openCodeProviderUrl.get(),
            openCodeProvider = extension.openCodeProvider.get(),
            openCodeApiKeyEnvVar = extension.openCodeApiKeyEnvVar.get().takeIf { it.isNotBlank() },
            bridgeEnabled = extension.bridgeEnabled.get(),
            bridgeHost = extension.bridgeHost.get(),
            bridgePort = extension.bridgePort.get(),
            material = materialRequirement(extension),
            moodleMaterialEnabled = extension.moodleImportEnabled.get(),
            moodlePlan = moodlePlan(extension),
        )

        val platformDir = File(outputDir.get().asFile, platform.os.dirName).apply { mkdirs() }
        InstallerScriptGenerator.render(platform).forEach { file ->
            val target = File(platformDir, file.relativePath)
            target.writeText(file.content)
            logger.lifecycle("[academy] generated ${platform.os.name.lowercase()} installer -> ${target.absolutePath}")
        }
    }

    /**
     * The material plan the installer stages for the one-shot `moodle-material`
     * service (ACADEMY-11-4), or `null` when injection is disabled. Reuses the
     * exact read + build rules of `generateMoodleImport` (single source), so the
     * installer never re-derives the structure.
     */
    private fun moodlePlan(extension: AcademyInstallerExtension): MoodleImportPlan? {
        if (!extension.moodleImportEnabled.get()) return null
        val material = extension.moodleMaterialDir.orNull?.asFile
        val artifacts = if (material != null) MoodleMaterialReader.read(material) else emptyList()
        return MoodlePlanBuilder.build(
            shortName = extension.moodleCourseShortName.get(),
            fullName = extension.moodleCourseFullName.get(),
            material = artifacts,
        )
    }

    /**
     * The material requirement declared on the extension, or `null` when none is
     * set (ACADEMY-4) — the project simply consumes no versioned material and no
     * `MATERIAL.md` is written (D-ACADEMY-4-10).
     */
    private fun materialRequirement(extension: AcademyInstallerExtension): MaterialRequirement? {
        val remote = extension.materialRemoteUrl.get()
        if (remote.isBlank()) return null
        return MaterialRequirement(
            remoteUrl = remote,
            currentVersion = extension.materialCurrentVersion.get().ifBlank { null },
            expectedTypes = extension.materialExpectedTypes.get(),
        )
    }
}
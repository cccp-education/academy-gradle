package education.cccp.academy

import education.cccp.academy.installer.InstallerPlatform
import education.cccp.academy.installer.InstallerScriptGenerator
import education.cccp.academy.installer.TargetOs
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
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
        )

        val platformDir = File(outputDir.get().asFile, platform.os.dirName).apply { mkdirs() }
        InstallerScriptGenerator.render(platform).forEach { file ->
            val target = File(platformDir, file.relativePath)
            target.writeText(file.content)
            logger.lifecycle("[academy] generated ${platform.os.name.lowercase()} installer -> ${target.absolutePath}")
        }
    }
}
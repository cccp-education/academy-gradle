package education.cccp.academy

import education.cccp.academy.installer.TargetOs
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Academy Gradle Plugin — learner experience as a Gradle plugin
 * (Bagdad borough, DAG N2).
 *
 * Applies the typed `academyInstaller { }` extension (ACADEMY-1-0) and
 * registers the installer generator tasks (ACADEMY-1-1):
 * `generateLinuxInstaller`, `generateWindowsInstaller`,
 * `generateMacInstaller` and the `buildAllInstallers` aggregate.
 */
class AcademyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "academyInstaller",
            AcademyInstallerExtension::class.java,
        )
        wireDefaults(project, extension)
        registerInstallerTasks(project, extension)
    }

    private fun wireDefaults(project: Project, extension: AcademyInstallerExtension) {
        extension.applicationName.convention("academy")
        extension.applicationVersion.convention(project.version.toString())
        extension.javaVersion.convention("25")
        extension.gradleVersion.convention("9.7.1")
        extension.targets.convention(listOf(TargetOs.LINUX, TargetOs.WINDOWS, TargetOs.MACOS))
        extension.outputDir.convention(project.layout.buildDirectory.dir("academy/installers"))
        extension.composeEnabled.convention(true)
        extension.credentialsEnvPrefix.convention("ACADEMY_")
    }

    private fun registerInstallerTasks(project: Project, extension: AcademyInstallerExtension) {
        registerPlatformTask(project, extension, "generateLinuxInstaller", TargetOs.LINUX)
        registerPlatformTask(project, extension, "generateWindowsInstaller", TargetOs.WINDOWS)
        registerPlatformTask(project, extension, "generateMacInstaller", TargetOs.MACOS)

        project.tasks.register("buildAllInstallers") {
            it.group = "academy"
            it.description = "Generates every installer target (Linux, Windows, macOS) into the academy output directory"
            it.dependsOn("generateLinuxInstaller", "generateWindowsInstaller", "generateMacInstaller")
        }
    }

    private fun registerPlatformTask(
        project: Project,
        extension: AcademyInstallerExtension,
        name: String,
        os: TargetOs,
    ) {
        project.tasks.register(name, GenerateInstallerTask::class.java) { task ->
            task.group = "academy"
            task.description = "Generates the ${os.name.lowercase()} installer into the academy output directory"
            task.targetOs.set(os)
            task.installerExtension.set(extension)
            task.outputDir.set(extension.outputDir)
        }
    }
}
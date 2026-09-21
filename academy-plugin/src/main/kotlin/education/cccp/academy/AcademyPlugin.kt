package education.cccp.academy

import contracts.runtime.LlmProviderKind
import education.cccp.academy.bridge.ByokBridgeSupport
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
        registerBridgeTask(project, extension)
        registerMaterialTask(project, extension)
        registerMoodleImportTask(project, extension)
    }

    /**
     * Registers the Moodle material injection task (ACADEMY-11-2) — opt-in and
     * degraded: without material it writes nothing (D-ACADEMY-11-6). The task
     * only *generates* the plan and the generic CLI script; the one-shot
     * `moodle-material` service applies them after Moodle created its schema.
     */
    private fun registerMoodleImportTask(project: Project, extension: AcademyInstallerExtension) {
        project.tasks.register("generateMoodleImport", GenerateMoodleImportTask::class.java) { task ->
            task.group = "academy"
            task.description = "Generates the Moodle material injection plan + generic CLI script (ACADEMY-11)"
            task.moodleImportEnabled.set(extension.moodleImportEnabled)
            task.materialDir.set(extension.moodleMaterialDir)
            task.courseShortName.set(extension.moodleCourseShortName)
            task.courseFullName.set(extension.moodleCourseFullName)
            task.outputDir.set(project.layout.buildDirectory.dir("academy/moodle"))
            task.onlyIf { extension.moodleImportEnabled.get() }
        }
    }

    /**
     * Registers the material inspection task (ACADEMY-4-2) — read-only, and
     * never destructive: it verifies what the learner pulled, academy does not
     * pull himself (the bureau owns the transport, D-ACADEMY-4-1).
     */
    private fun registerMaterialTask(project: Project, extension: AcademyInstallerExtension) {
        project.tasks.register("inspectTrainingMaterial", InspectTrainingMaterialTask::class.java) { task ->
            task.materialRemoteUrl.set(extension.materialRemoteUrl)
            task.materialCurrentVersion.set(extension.materialCurrentVersion)
            task.materialExpectedTypes.set(extension.materialExpectedTypes)
            task.materialDir.set(extension.materialDir)
        }
    }

    private fun registerBridgeTask(project: Project, extension: AcademyInstallerExtension) {
        project.tasks.register("serveWebhookBridge", ServeWebhookBridgeTask::class.java) { task ->
            task.bridgeEnabled.set(extension.bridgeEnabled)
            task.bridgeHost.set(extension.bridgeHost)
            task.bridgePort.set(extension.bridgePort)
            task.providerId.set(extension.openCodeProvider.map { ByokBridgeSupport.providerIdFor(it) })
            task.model.set(extension.openCodeModel)
        }
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
        extension.openCodeEnabled.convention(true)
        extension.openCodeModel.convention("gpt-oss:120b-cloud")
        extension.openCodeProviderUrl.convention("http://ollama:11434/v1")
        extension.openCodeProvider.convention(LlmProviderKind.OLLAMA_LOCAL)
        extension.openCodeApiKeyEnvVar.convention("")
        extension.bridgeEnabled.convention(false)
        extension.bridgeHost.convention("127.0.0.1")
        extension.bridgePort.convention(8765)
        extension.materialRemoteUrl.convention("")
        extension.materialCurrentVersion.convention("")
        extension.materialDir.convention(project.layout.projectDirectory.dir("material"))
        extension.materialExpectedTypes.convention(emptyList())
        extension.moodleImportEnabled.convention(false)
        extension.moodleMaterialDir.convention(project.layout.projectDirectory.dir("material"))
        extension.moodleCourseShortName.convention("academy-seed")
        extension.moodleCourseFullName.convention("Academy - Experimentation Track")
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
package education.cccp.academy

import contracts.runtime.LlmProviderKind
import education.cccp.academy.bridge.ByokBridgeSupport
import education.cccp.academy.installer.HostEnvironmentProbe
import education.cccp.academy.installer.TargetOs
import education.cccp.academy.installer.offline.OfflineImageCatalog
import education.cccp.academy.installer.offline.ProcessDockerImageRunner
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
        val extension =
            project.extensions.create(
                "academyInstaller",
                AcademyInstallerExtension::class.java,
            )
        wireDefaults(project, extension)
        registerInstallerTasks(project, extension)
        registerContainerEngineTask(project)
        registerBridgeTask(project, extension)
        registerMaterialTask(project, extension)
        registerMoodleImportTask(project, extension)
        registerOfflineBundleTask(project, extension)
        registerA11yTask(project, extension)
        registerMoodleA11yTask(project, extension)
        registerAcceptanceTask(project, extension)
        registerWorkstationTask(project, extension)
    }

    /**
     * Registers the learner workstation assembler (ACADEMY-16 US-2) — builds the
     * distributable zip that talaria delivers after payment (D-LWA-2).
     */
    private fun registerWorkstationTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("buildLearnerWorkstation", BuildLearnerWorkstationTask::class.java) { task ->
            task.group = "academy"
            task.description = "Builds the distributable learner workstation zip (installers + project + material + README + manifest)"
            task.installersDir.set(extension.outputDir)
            task.materialFiles.from(extension.materialDir)
            task.offlineFiles.from(extension.offlineBundleFile, extension.offlineManifestFile)
            task.applicationName.set(extension.applicationName)
            task.applicationVersion.set(extension.applicationVersion)
            task.academyVersion.set(
                AcademyPlugin::class.java.getPackage()?.implementationVersion ?: "0.0.0-dev",
            )
            task.moduleId.set(extension.workstationModuleId)
            task.moduleVersion.set(extension.workstationModuleVersion)
            task.profile.set(
                project.provider {
                    if (extension.openCodeProvider.get() == LlmProviderKind.OLLAMA_LOCAL) {
                        education.cccp.academy.workstation.WorkstationProfile.LOCAL_OLLAMA
                    } else {
                        education.cccp.academy.workstation.WorkstationProfile.CLOUD_BYOK
                    }
                },
            )
            task.openCodeModel.set(extension.openCodeModel)
            task.openCodeProviderUrl.set(extension.openCodeProviderUrl)
            task.openCodeApiKeyEnvVar.set(extension.openCodeApiKeyEnvVar)
            task.outputDir.set(extension.workstationOutputDir)
            task.dependsOn("buildAllInstallers")
        }
    }

    /**
     * Registers the box acceptance gate (ACADEMY-15 / GBL-011-5) — opt-in and
     * advisory by default. It aggregates the material and Moodle reports; an
     * absent or malformed report is a declared gap, and only `acceptanceStrict`
     * turns an `ERROR` finding into a build failure.
     */
    private fun registerAcceptanceTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("auditAcademyAcceptance", AuditAcademyAcceptanceTask::class.java) { task ->
            task.group = "academy"
            task.description = "Aggregates the material and Moodle audits into the box acceptance report (ACADEMY-15)"
            task.acceptanceEnabled.set(extension.acceptanceEnabled)
            task.acceptanceStrict.set(extension.acceptanceStrict)
            task.materialReports.from(extension.a11yReportFile)
            task.moodleReports.from(extension.moodleA11yReportFile)
            task.reportFile.set(extension.acceptanceReportFile)
            task.onlyIf { extension.acceptanceEnabled.get() }
        }
    }

    /**
     * Registers the Moodle accessibility audit (ACADEMY-15 / GBL-011-5) — opt-in
     * and advisory by default. The task fetches the configured pages over HTTP
     * and writes the report; only `moodleA11yStrict` turns an `ERROR` finding into
     * a build failure.
     */
    private fun registerMoodleA11yTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("auditMoodleAccessibility", AuditMoodleAccessibilityTask::class.java) { task ->
            task.group = "academy"
            task.description = "Audits the accessibility of the running Moodle box (ACADEMY-15 / GBL-011-5)"
            task.moodleA11yEnabled.set(extension.moodleA11yEnabled)
            task.moodleA11yStrict.set(extension.moodleA11yStrict)
            task.moodleA11yBaseUrl.set(extension.moodleA11yBaseUrl)
            task.moodleA11yPaths.set(extension.moodleA11yPaths)
            task.reportFile.set(extension.moodleA11yReportFile)
            task.onlyIf { extension.moodleA11yEnabled.get() }
        }
    }

    /**
     * Registers the material accessibility audit (ACADEMY-15) — opt-in and
     * advisory by default. The report is always written when the task runs; only
     * `a11yStrict` turns an `ERROR` finding into a build failure.
     */
    private fun registerA11yTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("auditTrainingAccessibility", AuditTrainingAccessibilityTask::class.java) { task ->
            task.group = "academy"
            task.description = "Audits the accessibility of the pulled training material (ACADEMY-15)"
            task.a11yAuditEnabled.set(extension.a11yAuditEnabled)
            task.a11yStrict.set(extension.a11yStrict)
            task.materialFiles.from(extension.materialDir)
            task.reportFile.set(extension.a11yReportFile)
            task.onlyIf { extension.a11yAuditEnabled.get() }
        }
    }

    /**
     * Registers the offline bundle builder (ACADEMY-14) — opt-in and never
     * cached: it pulls gigabytes and writes one archive plus its manifest. The
     * Docker process lives behind the injected runner, so the task is testable
     * with a fake (D-ACADEMY-14-2).
     */
    private fun registerOfflineBundleTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("buildOfflineBundle", BuildOfflineBundleTask::class.java) { task ->
            task.group = "academy"
            task.description = "Builds the offline container image archive + manifest of the academy box (ACADEMY-14)"
            task.references.set(
                extension.offlineImages.map { images ->
                    if (images.isEmpty()) OfflineImageCatalog.defaults else images
                },
            )
            task.bundleFile.set(extension.offlineBundleFile)
            task.manifestFile.set(extension.offlineManifestFile)
            task.dockerRunner.convention(ProcessDockerImageRunner())
            task.workspaceImage.convention(OfflineImageCatalog.WORKSPACE_IMAGE)
            task.onlyIf { extension.offlineBundleEnabled.get() }
        }
    }

    /**
     * Registers the container engine check task (ACADEMY-12-2) — the build-time
     * adapter of the pure `academy.env` decider. The real host probe is the
     * default; a test overrides the property with a fake (D-ACADEMY-12-6).
     */
    private fun registerContainerEngineTask(project: Project) {
        project.tasks.register("checkContainerEngine", CheckContainerEngineTask::class.java) { task ->
            task.environmentProbe.convention(HostEnvironmentProbe())
        }
        project.pluginManager.withPlugin("base") {
            project.tasks.named("check") { it.dependsOn("checkContainerEngine") }
        }
    }

    /**
     * Registers the Moodle material injection task (ACADEMY-11-2) — opt-in and
     * degraded: without material it writes nothing (D-ACADEMY-11-6). The task
     * only *generates* the plan and the generic CLI script; the one-shot
     * `moodle-material` service applies them after Moodle created its schema.
     */
    private fun registerMoodleImportTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
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
    private fun registerMaterialTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("inspectTrainingMaterial", InspectTrainingMaterialTask::class.java) { task ->
            task.materialRemoteUrl.set(extension.materialRemoteUrl)
            task.materialCurrentVersion.set(extension.materialCurrentVersion)
            task.materialExpectedTypes.set(extension.materialExpectedTypes)
            task.materialDir.set(extension.materialDir)
        }
    }

    private fun registerBridgeTask(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
        project.tasks.register("serveWebhookBridge", ServeWebhookBridgeTask::class.java) { task ->
            task.bridgeEnabled.set(extension.bridgeEnabled)
            task.bridgeHost.set(extension.bridgeHost)
            task.bridgePort.set(extension.bridgePort)
            task.providerId.set(extension.openCodeProvider.map { ByokBridgeSupport.providerIdFor(it) })
            task.model.set(extension.openCodeModel)
        }
    }

    private fun wireDefaults(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
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
        extension.offlineBundleEnabled.convention(false)
        extension.offlineImages.convention(emptyList())
        extension.offlineBundleFile.convention(
            project.layout.buildDirectory.file("academy/offline/offline-images.tar"),
        )
        extension.offlineManifestFile.convention(
            project.layout.buildDirectory.file("academy/offline/offline-images.json"),
        )
        extension.a11yAuditEnabled.convention(false)
        extension.a11yStrict.convention(false)
        extension.a11yReportFile.convention(
            project.layout.buildDirectory.file("academy/a11y/accessibility-report.json"),
        )
        extension.moodleA11yEnabled.convention(false)
        extension.moodleA11yStrict.convention(false)
        extension.moodleA11yBaseUrl.convention("http://localhost:8080")
        extension.moodleA11yPaths.convention(listOf("/login/index.php"))
        extension.moodleA11yReportFile.convention(
            project.layout.buildDirectory.file("academy/a11y/moodle-accessibility-report.json"),
        )
        extension.acceptanceEnabled.convention(false)
        extension.acceptanceStrict.convention(false)
        extension.acceptanceReportFile.convention(
            project.layout.buildDirectory.file("academy/a11y/acceptance-report.json"),
        )
        extension.workstationModuleId.convention("pilot")
        extension.workstationModuleVersion.convention("0.0.1")
        extension.workstationOutputDir.convention(
            project.layout.buildDirectory.dir("academy/workstation"),
        )
    }

    private fun registerInstallerTasks(
        project: Project,
        extension: AcademyInstallerExtension,
    ) {
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
            // ACADEMY-11-4 — the pulled material is a tolerant input so a changed
            // plan re-renders on a plain re-run (never a stale scaffold).
            task.moodleMaterialFiles.from(extension.moodleMaterialDir)
            // ACADEMY-14 — optional manifest: absent means no digest pinning.
            task.offlineManifestFiles.from(extension.offlineManifestFile)
            // The installer consumes the manifest produced by the bundle task.
            // Declare the dependency explicitly (Gradle validation error found by
            // the offline dogfooding): the bundle self-skips when disabled, so
            // this is harmless on the networked path.
            task.dependsOn("buildOfflineBundle")
        }
    }
}

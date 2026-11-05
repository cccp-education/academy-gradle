package education.cccp.academy

import education.cccp.academy.opencode.LearnerGuideGenerator
import education.cccp.academy.opencode.OpenCodeCatalog
import education.cccp.academy.opencode.OpenCodeConfig
import education.cccp.academy.opencode.OpenCodeConfigGenerator
import education.cccp.academy.opencode.OpenCodeToolGenerator
import education.cccp.academy.opencode.WorkspaceDockerfileGenerator
import education.cccp.academy.workstation.LearnerWorkstationManifest
import education.cccp.academy.workstation.WorkstationFile
import education.cccp.academy.workstation.WorkstationProfile
import education.cccp.academy.workstation.WorkstationProjectGenerator
import education.cccp.academy.workstation.WorkstationReadmeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ACADEMY-16 US-2 — builds the **distributable learner workstation** zip.
 *
 * The workstation is the artefact talaria delivers after payment (D-LWA-2):
 * the installers, the learner Gradle project (with the public boroughs enabled
 * as opencode tools), the module material, the optional offline bundle, the
 * learner README and the verifiable manifest.
 *
 * Every file is produced by the existing pure generators (single source): the
 * installer scaffold, the opencode config, the learner guide, the tool file and
 * the project build scripts are never duplicated here.
 */
@DisableCachingByDefault(because = "Distribution artefact built on demand; its content is versioned by the manifest")
abstract class BuildLearnerWorkstationTask : DefaultTask() {
    /** The installers produced by `buildAllInstallers` (`linux/`, `windows/`, `macos/`). */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val installersDir: DirectoryProperty

    /**
     * The module material to embed (EPIC K `metadata.json` + AsciiDoc). Tolerant
     * input (pattern S-221): absent means the zip carries no material and the
     * learner produces from the guidance only.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val materialFiles: ConfigurableFileCollection

    /** Optional offline image archive + manifest (ACADEMY-14). */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val offlineFiles: ConfigurableFileCollection

    @get:Input
    abstract val applicationName: Property<String>

    @get:Input
    abstract val applicationVersion: Property<String>

    @get:Input
    abstract val academyVersion: Property<String>

    @get:Input
    abstract val moduleId: Property<String>

    @get:Input
    abstract val moduleVersion: Property<String>

    @get:Input
    abstract val profile: Property<WorkstationProfile>

    @get:Input
    abstract val openCodeModel: Property<String>

    @get:Input
    abstract val openCodeProviderUrl: Property<String>

    @get:Input
    abstract val openCodeApiKeyEnvVar: Property<String>

    /** Destination directory; the zip is named `academy-workstation-<version>.zip`. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun build() {
        val output = outputDir.get().asFile
        val staging = File(output, "staging")
        output.mkdirs()
        staging.deleteRecursively()
        staging.mkdirs()
        // Remove a previous zip of another version: a distributed artefact is
        // immutable, but the build directory must not accumulate stale versions.
        output.listFiles { file -> file.isFile && file.name.endsWith(".zip") }?.forEach { it.delete() }

        copyTree(installersDir.get().asFile.toPath(), File(staging, "installers").toPath())
        materialFiles.files.firstOrNull { it.isDirectory }?.let { material ->
            copyTree(material.toPath(), File(staging, "material").toPath())
        }
        offlineFiles.files.filter { it.isFile }.forEach { file ->
            val target = File(staging, "offline/${file.name}")
            target.parentFile.mkdirs()
            Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        val config =
            OpenCodeConfig(
                providerUrl = openCodeProviderUrl.get(),
                model = openCodeModel.get(),
                apiKeyEnvVar = openCodeApiKeyEnvVar.get().takeIf { it.isNotBlank() },
            )
        write(staging, "project/Dockerfile", WorkspaceDockerfileGenerator.render(config))
        write(staging, "project/opencode.json", OpenCodeConfigGenerator.render(config))
        write(staging, "project/AGENTS.md", LearnerGuideGenerator.render(config))
        val toolFile = OpenCodeToolGenerator.renderAll(OpenCodeCatalog.plugins)
        write(staging, "project/${toolFile.relativePath}", toolFile.content)
        WorkstationProjectGenerator.render(OpenCodeCatalog.plugins).forEach { projectFile ->
            write(staging, projectFile.relativePath, projectFile.content)
        }
        write(
            staging,
            "README-apprenant.md",
            WorkstationReadmeGenerator.render(
                applicationName = applicationName.get(),
                moduleId = moduleId.get(),
                moduleVersion = moduleVersion.get(),
                profile = profile.get(),
                plugins = OpenCodeCatalog.plugins,
            ),
        )

        val files = collectFiles(staging)
        val manifest =
            LearnerWorkstationManifest.of(
                workstationVersion = workstationVersion(),
                academyVersion = academyVersion.get(),
                moduleId = moduleId.get(),
                moduleVersion = moduleVersion.get(),
                profile = profile.get(),
                files = files,
            )
        write(staging, "workstation-manifest.json", manifest.toJson())

        val zip = File(output, "academy-workstation-${workstationVersion()}.zip")
        zipTree(staging.toPath(), zip.toPath())
        logger.lifecycle("[academy] learner workstation -> ${zip.absolutePath} (${files.size} files)")
    }

    private fun workstationVersion(): String {
        val version = "${academyVersion.get()}+${moduleId.get()}-${moduleVersion.get()}"
        return version
    }

    private fun write(
        root: File,
        relativePath: String,
        content: String,
    ) {
        val target = File(root, relativePath)
        target.parentFile.mkdirs()
        target.writeText(content)
    }

    /** Every regular file under [root], relative and sorted by path. */
    private fun collectFiles(root: File): List<WorkstationFile> {
        val files = mutableListOf<WorkstationFile>()
        Files.walk(root.toPath()).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .forEach { path ->
                    val relative =
                        root
                            .toPath()
                            .relativize(path)
                            .toString()
                            .replace(File.separatorChar, '/')
                    files += WorkstationFile.of(relative, Files.readAllBytes(path))
                }
        }
        return files.sortedBy { it.path }
    }

    private fun copyTree(
        source: Path,
        target: Path,
    ) {
        if (!Files.isDirectory(source)) return
        Files.walk(source).use { stream ->
            stream.forEach { path ->
                val relative = source.relativize(path)
                val destination = target.resolve(relative.toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination)
                } else {
                    Files.createDirectories(destination.parent)
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    /** Deterministic zip: sorted entries and a fixed timestamp (ZIP epoch). */
    private fun zipTree(
        source: Path,
        zip: Path,
    ) {
        Files.createDirectories(zip.parent)
        ZipOutputStream(Files.newOutputStream(zip)).use { out ->
            val entries =
                Files
                    .walk(source)
                    .use { stream ->
                        stream
                            .filter { Files.isRegularFile(it) }
                            .map { source.relativize(it).toString().replace(File.separatorChar, '/') }
                            .toList()
                            .sorted()
                    }
            entries.forEach { relative ->
                val entry = ZipEntry(relative)
                entry.time = ZIP_EPOCH
                out.putNextEntry(entry)
                Files.copy(source.resolve(relative), out)
                out.closeEntry()
            }
        }
    }

    private companion object {
        /** 1980-01-01T00:00:00Z — the ZIP epoch, used for deterministic entries. */
        const val ZIP_EPOCH: Long = 315532800000L
    }
}

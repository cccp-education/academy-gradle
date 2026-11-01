package education.cccp.academy

import education.cccp.academy.installer.offline.DockerImageCommands
import education.cccp.academy.installer.offline.DockerImageRunner
import education.cccp.academy.installer.offline.ImageDigest
import education.cccp.academy.installer.offline.OfflineBundleManifest
import education.cccp.academy.installer.offline.OfflineImage
import education.cccp.academy.opencode.WorkspaceDockerfileGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * ACADEMY-14 — builds the offline image archive of the academy box.
 *
 * Thin-wrapper task (pattern codex `CollectPageProvenanceTask`): every Docker
 * invocation lives behind the injected [DockerImageRunner] seam, so the
 * orchestration is unit-tested with a fake and the real adapter is the only
 * I/O. Opt-in: the task is skipped unless `academyInstaller.offlineBundleEnabled`
 * is set (a build never pulls gigabytes by default).
 *
 * Outputs:
 *  - the archive (`docker save -o`), loaded by the generated installer with
 *    `docker load -i` when present;
 *  - `offline-images.json`, the manifest (tags + resolved digests).
 */
@DisableCachingByDefault(because = "Pulls and saves container images; never cached (ACADEMY-14)")
abstract class BuildOfflineBundleTask : DefaultTask() {
    /** Image references to pull and archive (defaults are wired by the plugin). */
    @get:Input
    abstract val references: ListProperty<String>

    /** The `docker save` archive. */
    @get:OutputFile
    abstract val bundleFile: RegularFileProperty

    /** The manifest written next to the archive. */
    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    /** Process seam — the default adapter is wired by the plugin. */
    @get:Internal
    abstract val dockerRunner: Property<DockerImageRunner>

    /**
     * ACADEMY-14 — the learner workspace image, **built** (not pulled) from the
     * generated Dockerfile and shipped in the same archive. It is the piece that
     * makes the box offline: the Dockerfile installs `opencode` from a network
     * script, which cannot run on a fresh offline host.
     */
    @get:Internal
    abstract val workspaceImage: Property<String>

    @TaskAction
    fun buildBundle() {
        val runner = dockerRunner.get()
        val images =
            references.get().distinct().map { reference ->
                val pull = runner.run(DockerImageCommands.pull(reference))
                if (pull.exitCode != 0) {
                    throw GradleException(
                        "[academy] docker pull failed for $reference " +
                            "(exit ${pull.exitCode}): ${pull.output.trim()}",
                    )
                }
                val digest = ImageDigest.parse(runner.run(DockerImageCommands.inspectDigest(reference)).output)
                OfflineImage(reference = reference, digest = digest)
            }

        // ACADEMY-14 — the workspace image is built here (online bundle machine)
        // so the archive ships it pre-built; the offline host never runs the
        // Dockerfile's `curl | bash` opencode install.
        val workspaceContext = temporaryDir.apply { mkdirs() }
        File(workspaceContext, "Dockerfile").writeText(WorkspaceDockerfileGenerator.render())
        val build = runner.run(DockerImageCommands.build(workspaceImage.get(), workspaceContext.absolutePath))
        if (build.exitCode != 0) {
            throw GradleException(
                "[academy] docker build failed for ${workspaceImage.get()} " +
                    "(exit ${build.exitCode}): ${build.output.trim()}",
            )
        }
        val workspaceDigest = ImageDigest.parse(runner.run(DockerImageCommands.inspectDigest(workspaceImage.get())).output)
        val allImages = images + OfflineImage(workspaceImage.get(), workspaceDigest)

        val archive = bundleFile.get().asFile
        archive.parentFile.mkdirs()
        val save = runner.run(DockerImageCommands.save(allImages.map { it.reference }, archive.absolutePath))
        if (save.exitCode != 0) {
            throw GradleException(
                "[academy] docker save failed (exit ${save.exitCode}): ${save.output.trim()}",
            )
        }
        if (!archive.exists()) {
            throw GradleException(
                "[academy] docker save reported success but wrote no archive at ${archive.absolutePath}",
            )
        }

        val manifest = OfflineBundleManifest(version = 1, images = allImages)
        manifestFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(OfflineBundleManifest.toJson(manifest))

        logger.lifecycle(
            "[academy] offline bundle: ${allImages.size} images -> ${archive.absolutePath} " +
                "(manifest ${manifestFile.get().asFile.absolutePath})",
        )
    }
}

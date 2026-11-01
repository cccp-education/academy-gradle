package education.cccp.academy.installer.offline

/**
 * ACADEMY-14 — pure Docker argv builders.
 *
 * The process seam stays in [ProcessDockerImageRunner]; this object only
 * describes what to run, so the exact command line is unit-tested without
 * Docker and without a machine.
 */
object DockerImageCommands {
    /** `docker pull <reference>` */
    fun pull(reference: String): List<String> = listOf("docker", "pull", reference)

    /** `docker save -o <archive> <reference>...` */
    fun save(
        references: List<String>,
        outputPath: String,
    ): List<String> = listOf("docker", "save", "-o", outputPath) + references

    /** `docker load -i <archive>` */
    fun load(bundlePath: String): List<String> = listOf("docker", "load", "-i", bundlePath)

    /** `docker build -t <tag> <context>` — ACADEMY-14, pre-built workspace image. */
    fun build(
        tag: String,
        contextPath: String,
    ): List<String> = listOf("docker", "build", "-t", tag, contextPath)

    /**
     * Resolves the first `RepoDigests` entry of a locally pulled image.
     * The output line is `repo@sha256:<64 hex>` (see [ImageDigest]).
     */
    fun inspectDigest(reference: String): List<String> =
        listOf(
            "docker",
            "inspect",
            "--format",
            "{{index .RepoDigests 0}}",
            reference,
        )
}

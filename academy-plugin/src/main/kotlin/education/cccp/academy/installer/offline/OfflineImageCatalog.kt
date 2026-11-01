package education.cccp.academy.installer.offline

/**
 * ACADEMY-14 — the images the academy box needs on a fresh, offline host.
 *
 * Four services are pulled by the compose scaffold; the learner workspace is
 * built from a pinned base (`gradle:9.7.1-jdk25`, D-ACADEMY-6-10). All five are
 * part of the bundle: saving only the services would still require the network
 * when compose builds the workspace image.
 *
 * The compose scaffold declares `ollama/ollama` and `portainer/portainer-ce`
 * without a tag, which Docker resolves to `:latest`; the references are written
 * explicitly so the bundle is deterministic.
 */
object OfflineImageCatalog {
    const val MOODLE: String = "erseco/alpine-moodle:v5.2.3"
    const val POSTGRES: String = "postgres:16"
    const val OLLAMA: String = "ollama/ollama:latest"
    const val PORTAINER: String = "portainer/portainer-ce:latest"
    const val WORKSPACE_BASE: String = "gradle:9.7.1-jdk25"

    /**
     * ACADEMY-14 — the learner workspace image. It is **built** by the bundle
     * (not pulled): `WorkspaceDockerfileGenerator` is rendered to a temporary
     * context, then `docker build` runs on the (online) bundle machine. Shipping
     * the pre-built image is what makes the box offline: the Dockerfile installs
     * `opencode` from a network script, which would fail on a fresh offline host.
     */
    const val WORKSPACE_IMAGE: String = "cccp-education/academy-workspace:latest"

    /** Deterministic, duplicate-free default pull list (the built image is added by the task). */
    val defaults: List<String> = listOf(MOODLE, POSTGRES, OLLAMA, PORTAINER, WORKSPACE_BASE)
}

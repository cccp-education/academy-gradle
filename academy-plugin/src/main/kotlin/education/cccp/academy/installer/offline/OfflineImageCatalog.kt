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

    /** Deterministic, duplicate-free default bundle. */
    val defaults: List<String> = listOf(MOODLE, POSTGRES, OLLAMA, PORTAINER, WORKSPACE_BASE)
}

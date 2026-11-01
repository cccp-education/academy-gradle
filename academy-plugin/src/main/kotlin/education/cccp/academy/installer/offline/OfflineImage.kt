package education.cccp.academy.installer.offline

import kotlinx.serialization.Serializable

/**
 * ACADEMY-14 — one container image of the academy box.
 *
 * @property reference the Docker tag the scaffold pulls (`repo:tag`)
 * @property digest the resolved manifest digest, or `null` when the tool could
 *   not resolve it. The bundle records it so a stale archive is diagnosable; the
 *   compose file pins on [pinnedReference] once digest pinning is wired.
 */
@Serializable
data class OfflineImage(
    val reference: String,
    val digest: String? = null,
) {
    init {
        require(reference.isNotBlank()) { "image reference must not be blank" }
    }

    /** `repo:tag@sha256:...` when a digest was resolved, the tag otherwise. */
    val pinnedReference: String
        get() = if (digest.isNullOrBlank()) reference else "$reference@$digest"
}

package education.cccp.academy.workstation

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * The LLM profile of a learner workstation (ACADEMY-16, D-LWA-5).
 *
 * `CLOUD_BYOK` is the light default: the learner brings a provider key and the
 * compose scaffold does not embed the heavy Ollama runtime. `LOCAL_OLLAMA`
 * keeps the fully local runtime (opt-in).
 */
enum class WorkstationProfile {
    CLOUD_BYOK,
    LOCAL_OLLAMA,
}

/**
 * One file recorded in the workstation manifest (ACADEMY-16, D-LWA-8) — the
 * verifiable identity of a distributed artefact.
 *
 * The path is **relative to the workstation root**, uses `/` separators, and
 * can never escape the root (no absolute path, no `..`). The digest is lower-case
 * SHA-256, so a consumer (talaria) can verify the delivered bytes independently
 * of the archive format.
 *
 * @property path relative path inside the workstation
 * @property sha256 lower-case hex SHA-256 of the file content
 * @property size content length in bytes
 */
@Serializable
data class WorkstationFile(
    val path: String,
    val sha256: String,
    val size: Long,
) {
    init {
        require(path.isNotBlank()) { "path must not be blank" }
        require(!path.startsWith("/")) { "path must be relative, got: $path" }
        require(!path.contains('\\')) { "path must use '/' separators, got: $path" }
        require(!path.split('/').contains("..")) { "path must stay inside the workstation, got: $path" }
        require(sha256.matches(Regex("^[0-9a-f]{64}$"))) { "sha256 must be 64 lower-case hex chars, got: $sha256" }
        require(size >= 0) { "size must be >= 0, got: $size" }
    }

    companion object {
        /** Hashes [content] into a manifest file entry for [path]. */
        fun of(
            path: String,
            content: ByteArray,
        ): WorkstationFile =
            WorkstationFile(
                path = path,
                sha256 = sha256(content),
                size = content.size.toLong(),
            )

        /** Lower-case hex SHA-256 of [content] (pure, no I/O). */
        fun sha256(content: ByteArray): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(content)
                .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}

/**
 * The manifest of one distributable learner workstation (ACADEMY-16, D-LWA-8).
 *
 * It records the identity of the workstation and of the module it carries, the
 * LLM profile, and the exact content of every file — so talaria can generate a
 * signed delivery link and the learner (or support) can verify the archive.
 *
 * **No DRM**: this is a descriptive, verifiable manifest, not an enforcement
 * mechanism (academy is Apache 2.0, D-LWA-8).
 *
 * @property workstationVersion version of the distributed zip (immutable, D-LWA-12)
 * @property academyVersion academy plugin version that produced the zip
 * @property moduleId training module identity carried by the workstation
 * @property moduleVersion version of the carried module
 * @property profile LLM profile (cloud BYOK or local Ollama)
 * @property files every packaged file, sorted by path (deterministic)
 */
@Serializable
data class LearnerWorkstationManifest(
    val workstationVersion: String,
    val academyVersion: String,
    val moduleId: String,
    val moduleVersion: String,
    val profile: WorkstationProfile,
    val files: List<WorkstationFile>,
) {
    init {
        require(workstationVersion.isNotBlank()) { "workstationVersion must not be blank" }
        require(academyVersion.isNotBlank()) { "academyVersion must not be blank" }
        require(moduleId.isNotBlank()) { "moduleId must not be blank" }
        require(moduleVersion.isNotBlank()) { "moduleVersion must not be blank" }
        require(files.isNotEmpty()) { "a workstation must contain at least one file" }
        require(files.map { it.path }.distinct().size == files.size) { "workstation file paths must be unique" }
        require(files.map { it.path } == files.map { it.path }.sorted()) { "workstation files must be sorted by path" }
    }

    /** Deterministic pretty-printed JSON. */
    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json =
            Json {
                prettyPrint = true
                encodeDefaults = true
            }

        /** Builds a manifest, sorting [files] by path so the rendering is deterministic. */
        fun of(
            workstationVersion: String,
            academyVersion: String,
            moduleId: String,
            moduleVersion: String,
            profile: WorkstationProfile,
            files: List<WorkstationFile>,
        ): LearnerWorkstationManifest =
            LearnerWorkstationManifest(
                workstationVersion = workstationVersion,
                academyVersion = academyVersion,
                moduleId = moduleId,
                moduleVersion = moduleVersion,
                profile = profile,
                files = files.sortedBy { it.path },
            )
    }
}

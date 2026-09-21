package education.cccp.academy.material

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One versioned training artifact as academy reads it from the **EPIC K pivot**
 * `metadata.json` (ACADEMY-4-1, D-ACADEMY-4-4/4-5).
 *
 * The pivot is the normed format every producing borough writes next to its
 * AsciiDoc output (`TAXONOMIE_WORKSPACE.adoc` § Format Pivot) — academy reuses
 * it rather than inventing a second manifest format. Reading requires a real
 * decoder, which is why kotlinx-serialization (already an `implementation`
 * dependency since ACADEMY-8) is used for the input side.
 *
 * The [type] is resolved through [MaterialArtifactType]; [rawType] keeps the
 * producer's label verbatim so an unknown type is reported, never guessed.
 *
 * @property source producing borough
 * @property type resolved artifact type, or `null` when the producer label is unknown
 * @property rawType the producer's `type` label, verbatim
 * @property version material version, or `null` when the producer omitted it
 * @property model LLM model that generated the artifact (empty when not applicable)
 * @property dependencies upstream boroughs
 */
data class MaterialManifest(
    val source: String,
    val type: MaterialArtifactType?,
    val rawType: String,
    val version: String?,
    val model: String,
    val dependencies: List<String>,
) {
    companion object {
        /**
         * Tolerant by design (D-ACADEMY-4-4): unknown fields are ignored so a
         * producer can add one without breaking the consumer, and a malformed
         * document yields `null` instead of throwing — the *verdict*, not the
         * parser, decides what a missing material means.
         */
        private val reader = Json { ignoreUnknownKeys = true }

        /** Parses the pivot JSON, or returns `null` when it is not readable. */
        fun fromJson(json: String): MaterialManifest? = try {
            val pivot = reader.decodeFromString(Pivot.serializer(), json)
            MaterialManifest(
                source = pivot.source,
                type = MaterialArtifactType.fromPivotType(pivot.type),
                rawType = pivot.type,
                version = pivot.version,
                model = pivot.model,
                dependencies = pivot.dependencies,
            )
        } catch (_: Exception) {
            null
        }

        /**
         * The EPIC K pivot wire shape, private to the reader: only the fields
         * academy needs are declared, everything else is ignored.
         */
        @Serializable
        private data class Pivot(
            val source: String = "",
            val type: String = "",
            val version: String? = null,
            val model: String = "",
            val dependencies: List<String> = emptyList(),
        )
    }
}

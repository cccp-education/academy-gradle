package education.cccp.academy.installer.offline

/**
 * ACADEMY-14 — extracts a manifest digest from `docker inspect` output.
 *
 * The command returns `repo@sha256:<64 hex>`, but a wrapper may prefix a
 * warning on the same line. The digest is therefore matched by pattern, never
 * by splitting the whole line: a malformed line yields `null` and the bundle
 * keeps the tag instead of recording garbage.
 */
object ImageDigest {
    private val PATTERN = Regex("sha256:[0-9a-f]{64}")

    /** The first well-formed digest of [output], or `null`. */
    fun parse(output: String): String? = PATTERN.find(output)?.value
}

package education.cccp.academy.a11y

import java.io.File

/**
 * ACADEMY-15 — checks the accessibility companions of one video file.
 *
 * The convention is the capsule producer's naming: a video `foo.mp4` is
 * accompanied by `foo.vtt` (or `.srt`/`.ass`), `foo.txt` (transcript) and
 * `foo.audiodesc.txt` (audio description script). The inspector is pure file
 * naming — it never decodes the media, so it is deterministic and fast.
 *
 * Subtitles are an `ERROR` (a deaf learner cannot follow the video); a missing
 * transcript or audio description is a `WARNING` (the former is a duplicate of
 * the subtitles for most uses, the latter is a known declared limitation).
 */
object VideoA11yInspector {
    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov")
    private val SUBTITLE_EXTENSIONS = listOf("vtt", "srt", "ass")
    private val TRANSCRIPT_NAMES = listOf("txt", "md", "transcript.txt")

    /** True when [file] is a video the inspector understands. */
    fun supports(file: File): Boolean = file.extension.lowercase() in VIDEO_EXTENSIONS

    /** Findings for [video], based on its sibling companion files. */
    fun inspect(video: File): List<A11yFinding> {
        val siblings =
            video.parentFile
                ?.listFiles()
                ?.map { it.name }
                ?.toSet()
                .orEmpty()
        val base = video.nameWithoutExtension
        val findings = mutableListOf<A11yFinding>()

        if (SUBTITLE_EXTENSIONS.none { "$base.$it" in siblings }) {
            findings +=
                A11yFinding(
                    deliverable = A11yDeliverable.VIDEO,
                    rule = A11yRules.VIDEO_SUBTITLES_MISSING,
                    severity = A11ySeverity.ERROR,
                    location = video.name,
                    message = "No subtitle companion ($base.vtt/.srt/.ass): a deaf learner cannot follow the video",
                )
        }
        if (TRANSCRIPT_NAMES.none { "$base.$it" in siblings }) {
            findings +=
                A11yFinding(
                    deliverable = A11yDeliverable.VIDEO,
                    rule = A11yRules.VIDEO_TRANSCRIPT_MISSING,
                    severity = A11ySeverity.WARNING,
                    location = video.name,
                    message = "No transcript companion ($base.txt/.md/.transcript.txt)",
                )
        }
        if (siblings.none { it.startsWith("$base.audiodesc.") }) {
            findings +=
                A11yFinding(
                    deliverable = A11yDeliverable.VIDEO,
                    rule = A11yRules.AUDIO_DESCRIPTION_MISSING,
                    severity = A11ySeverity.WARNING,
                    location = video.name,
                    message = "No audio description companion ($base.audiodesc.*): a blind learner misses the visuals",
                )
        }

        return findings
    }
}

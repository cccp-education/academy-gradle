package education.cccp.academy.a11y

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 — the video companion checks.
 *
 * The inspector is pure naming: a video `foo.mp4` is expected to carry
 * `foo.vtt/.srt/.ass`, `foo.txt/.md` and `foo.audiodesc.*`.
 */
class VideoA11yInspectorTest {
    @TempDir
    lateinit var dir: File

    private fun video(): File = File(dir, "slide-01.mp4").apply { writeText("x") }

    @Test
    fun `a video without companions reports the three gaps`() {
        val findings = VideoA11yInspector.inspect(video())

        assertEquals(3, findings.size)
        assertTrue(findings.any { it.rule == A11yRules.VIDEO_SUBTITLES_MISSING })
        assertTrue(findings.any { it.rule == A11yRules.VIDEO_TRANSCRIPT_MISSING })
        assertTrue(findings.any { it.rule == A11yRules.AUDIO_DESCRIPTION_MISSING })
    }

    @Test
    fun `a video with subtitles, transcript and audio description has no finding`() {
        val video = video()
        listOf("slide-01.vtt", "slide-01.txt", "slide-01.audiodesc.txt").forEach {
            File(dir, it).writeText("x")
        }

        assertTrue(VideoA11yInspector.inspect(video).isEmpty())
    }

    @Test
    fun `subtitles alone still leave the transcript and audio description warnings`() {
        val video = video()
        File(dir, "slide-01.srt").writeText("x")

        val findings = VideoA11yInspector.inspect(video)

        assertTrue(findings.none { it.rule == A11yRules.VIDEO_SUBTITLES_MISSING })
        assertTrue(findings.any { it.rule == A11yRules.VIDEO_TRANSCRIPT_MISSING })
        assertTrue(findings.any { it.rule == A11yRules.AUDIO_DESCRIPTION_MISSING })
    }
}

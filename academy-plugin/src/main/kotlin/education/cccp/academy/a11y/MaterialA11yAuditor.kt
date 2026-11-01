package education.cccp.academy.a11y

import java.io.File

/**
 * ACADEMY-15 — audits the pulled training material (pure, no Gradle).
 *
 * What it checks here:
 *  - every video has subtitle, transcript and audio-description companions;
 *  - every text deliverable (HTML/EPUB/PDF) has a producer accessibility report,
 *    or is explicitly reported as unverifiable.
 *
 * What it deliberately does not check (declared in [LIMITS]): the Moodle theme
 * and the `opencode` terminal, which require a browser/session, and PDF tagging,
 * which the producer owns (Asciidoctor PDF is not PDF/UA).
 *
 * Verdict: `MISSING` when there is no material, `DEGRADED` when a finding exists,
 * `VERIFIED` otherwise. The gate never invents a check it did not run.
 */
object MaterialA11yAuditor {
    /** The limits every report carries, so a reader knows what was not measured. */
    val LIMITS =
        listOf(
            "PDF is not PDF/UA; the accessible text deliverable is HTML/EPUB",
            "Moodle theme and opencode terminal audits require a browser/session and are out of scope of this task",
            "Extended audio description (pausing the video) is not implemented",
        )

    /** Audits [materialDir], or reports `MISSING` when it is absent/empty. */
    fun audit(materialDir: File?): MaterialA11yReport {
        if (materialDir == null || !materialDir.isDirectory) {
            return MaterialA11yReport(readiness = A11yReadiness.MISSING, limits = LIMITS)
        }
        val files = materialDir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) {
            return MaterialA11yReport(readiness = A11yReadiness.MISSING, limits = LIMITS)
        }

        val findings = mutableListOf<A11yFinding>()
        files.filter { VideoA11yInspector.supports(it) }.forEach { findings += VideoA11yInspector.inspect(it) }

        val producer = ProducerReportInspector.inspect(materialDir, files)
        findings += producer.findings

        val readiness =
            if (findings.isEmpty()) {
                A11yReadiness.VERIFIED
            } else {
                A11yReadiness.DEGRADED
            }
        return MaterialA11yReport(
            readiness = readiness,
            findings = findings,
            limits = LIMITS + producer.limits,
        )
    }
}

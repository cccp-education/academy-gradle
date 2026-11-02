package education.cccp.academy.a11y

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** ACADEMY-15 — what a producer report contributes to the material audit. */
data class ProducerInspection(
    val findings: List<A11yFinding>,
    val limits: List<String>,
)

/**
 * ACADEMY-15 — reads the accessibility reports produced by the text producer
 * (`document`, GBL-011-3).
 *
 * Contract: JSON files named `a11y-report.json` (or `accessibility-report.json`)
 * under the material directory. Every report contributes — a producer may write
 * one report per artifact directory. The producer owns the checks; academy only
 * transports and aggregates them.
 *
 * A report only verifies the deliverable kinds it declares. A text deliverable
 * (HTML/EPUB/PDF) that no report covers yields a `producer-report-incomplete`
 * warning, and a text deliverable with no usable report yields a
 * `producer-report-missing` warning — never a silent "verified".
 *
 * The reader is tolerant (unknown fields/rules/kinds ignored) and a malformed
 * report is a declared limit, not a crash.
 */
object ProducerReportInspector {
    private val REPORT_NAMES = setOf("a11y-report.json", "accessibility-report.json")
    private val TEXT_EXTENSIONS =
        mapOf(
            "html" to A11yDeliverable.HTML,
            "epub" to A11yDeliverable.EPUB,
            "pdf" to A11yDeliverable.PDF,
        )
    private val json = Json { ignoreUnknownKeys = true }

    /** Reads every producer report of [materialDir] against the [files] it contains. */
    fun inspect(
        materialDir: File,
        files: List<File>,
    ): ProducerInspection {
        val textDeliverables =
            files
                .filter { it.extension.lowercase() in TEXT_EXTENSIONS }
                .map { TEXT_EXTENSIONS.getValue(it.extension.lowercase()) to it }
        val reportFiles = files.filter { it.name.lowercase() in REPORT_NAMES }

        if (reportFiles.isEmpty()) {
            return ProducerInspection(missingFindings(materialDir, textDeliverables, malformed = false), emptyList())
        }

        val parsed = reportFiles.mapNotNull { parse(it) }
        val malformedLimits =
            reportFiles
                .filter { report -> parsed.none { it.source == report } }
                .map { "Malformed producer report: ${it.name}" }

        if (parsed.isEmpty()) {
            return ProducerInspection(
                missingFindings(materialDir, textDeliverables, malformed = true),
                malformedLimits,
            )
        }

        val coveredKinds =
            parsed
                .flatMap { it.report.deliverables }
                .mapNotNull { kindOf(it.kind) }
                .toSet()

        val findings =
            parsed.flatMap { parsedReport ->
                parsedReport.report.deliverables.flatMap { deliverable ->
                    val kind = kindOf(deliverable.kind) ?: return@flatMap emptyList()
                    deliverable.findings.map { finding ->
                        A11yFinding(
                            deliverable = kind,
                            rule = finding.rule.ifBlank { A11yRules.PRODUCER_FINDING },
                            severity = severityOf(finding.severity),
                            location = finding.location,
                            message = finding.message,
                        )
                    }
                }
            } +
                textDeliverables
                    .filter { (kind, _) -> kind !in coveredKinds }
                    .map { (kind, file) ->
                        A11yFinding(
                            deliverable = kind,
                            rule = A11yRules.PRODUCER_REPORT_INCOMPLETE,
                            severity = A11ySeverity.WARNING,
                            location = file.relativeTo(materialDir).path,
                            message = "Producer accessibility report does not cover $kind: the deliverable cannot be verified",
                        )
                    }

        val limits = parsed.flatMap { it.report.deliverables.flatMap { deliverable -> deliverable.limits } } + malformedLimits
        return ProducerInspection(findings, limits)
    }

    private fun missingFindings(
        materialDir: File,
        textDeliverables: List<Pair<A11yDeliverable, File>>,
        malformed: Boolean,
    ): List<A11yFinding> {
        val cause = if (malformed) "Malformed producer accessibility report" else "No producer accessibility report"
        return textDeliverables.map { (deliverable, file) ->
            A11yFinding(
                deliverable = deliverable,
                rule = A11yRules.PRODUCER_REPORT_MISSING,
                severity = A11ySeverity.WARNING,
                location = file.relativeTo(materialDir).path,
                message = "$cause: $deliverable cannot be verified",
            )
        }
    }

    private fun kindOf(raw: String): A11yDeliverable? =
        A11yDeliverable.entries.firstOrNull {
            it.name.equals(raw.trim(), ignoreCase = true)
        }

    private fun parse(file: File): ParsedReport? =
        try {
            ParsedReport(file, json.decodeFromString(ProducerA11yReport.serializer(), file.readText()))
        } catch (_: Exception) {
            null
        }

    private fun severityOf(raw: String): A11ySeverity =
        A11ySeverity.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: A11ySeverity.WARNING

    private data class ParsedReport(
        val source: File,
        val report: ProducerA11yReport,
    )

    @Serializable
    private data class ProducerA11yReport(
        val deliverables: List<ProducerDeliverable> = emptyList(),
    )

    @Serializable
    private data class ProducerDeliverable(
        val kind: String = "",
        val findings: List<ProducerFinding> = emptyList(),
        val limits: List<String> = emptyList(),
    )

    @Serializable
    private data class ProducerFinding(
        val rule: String = "",
        val severity: String = "WARNING",
        val location: String = "",
        val message: String = "",
    )
}

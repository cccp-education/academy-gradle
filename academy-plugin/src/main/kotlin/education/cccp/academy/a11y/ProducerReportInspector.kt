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
 * ACADEMY-15 — reads the accessibility report produced by the text producer
 * (`document`, GBL-011-3).
 *
 * Contract: a JSON file named `a11y-report.json` (or `accessibility-report.json`)
 * under the material directory. The producer owns the checks; academy only
 * transports and aggregates them. Until the producer exists, a text deliverable
 * (HTML/EPUB/PDF) without the report yields a `producer-report-missing` warning —
 * never a silent "verified".
 *
 * The reader is tolerant (unknown fields/rules ignored) and a malformed report is
 * a declared limit, not a crash.
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

    /** Reads the producer report of [materialDir] against the [files] it contains. */
    fun inspect(
        materialDir: File,
        files: List<File>,
    ): ProducerInspection {
        val textDeliverables =
            files
                .filter { it.extension.lowercase() in TEXT_EXTENSIONS }
                .map { TEXT_EXTENSIONS.getValue(it.extension.lowercase()) to it }
        val reportFile = files.firstOrNull { it.name.lowercase() in REPORT_NAMES }

        if (reportFile == null) {
            val findings =
                textDeliverables.map { (deliverable, file) ->
                    A11yFinding(
                        deliverable = deliverable,
                        rule = A11yRules.PRODUCER_REPORT_MISSING,
                        severity = A11ySeverity.WARNING,
                        location = file.relativeTo(materialDir).path,
                        message = "No producer accessibility report: $deliverable cannot be verified",
                    )
                }
            return ProducerInspection(findings, emptyList())
        }

        val report = parse(reportFile)
        if (report == null) {
            val findings =
                textDeliverables.map { (deliverable, file) ->
                    A11yFinding(
                        deliverable = deliverable,
                        rule = A11yRules.PRODUCER_REPORT_MISSING,
                        severity = A11ySeverity.WARNING,
                        location = file.relativeTo(materialDir).path,
                        message = "Malformed producer accessibility report: $deliverable cannot be verified",
                    )
                }
            return ProducerInspection(findings, listOf("Malformed producer report: ${reportFile.name}"))
        }

        val findings =
            report.deliverables.flatMap { deliverable ->
                val kind =
                    A11yDeliverable.entries.firstOrNull {
                        it.name.equals(deliverable.kind.trim(), ignoreCase = true)
                    } ?: return@flatMap emptyList()
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
        return ProducerInspection(findings, report.deliverables.flatMap { it.limits })
    }

    private fun parse(file: File): ProducerA11yReport? =
        try {
            json.decodeFromString(ProducerA11yReport.serializer(), file.readText())
        } catch (_: Exception) {
            null
        }

    private fun severityOf(raw: String): A11ySeverity =
        A11ySeverity.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: A11ySeverity.WARNING

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

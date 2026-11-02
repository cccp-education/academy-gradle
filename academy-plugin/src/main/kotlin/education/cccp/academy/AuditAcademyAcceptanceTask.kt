package education.cccp.academy

import education.cccp.academy.a11y.A11ySeverity
import education.cccp.academy.a11y.AcademyA11yAcceptance
import education.cccp.academy.a11y.AcademyA11yReport
import education.cccp.academy.a11y.MaterialA11yReport
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * ACADEMY-15 / GBL-011-5 — the box acceptance gate.
 *
 * Thin wrapper (pattern `CollectPageProvenanceTask`): the aggregation is the
 * pure [AcademyA11yAcceptance]. It reads the two audit reports
 * (`auditTrainingAccessibility`, `auditMoodleAccessibility`) with tolerant
 * inputs: an absent or malformed report is a declared gap, never a crash.
 * Opt-in (`academyInstaller.acceptanceEnabled`, default `false`) and advisory
 * unless `acceptanceStrict`, which fails on any `ERROR` finding of any layer.
 */
@DisableCachingByDefault(because = "Aggregates report files and carries a strict side effect (ACADEMY-15)")
abstract class AuditAcademyAcceptanceTask : DefaultTask() {
    /** Opt-in switch — mirrors `academyInstaller.acceptanceEnabled`. */
    @get:Input
    abstract val acceptanceEnabled: Property<Boolean>

    /** When true, any `ERROR` finding of any layer fails the build (default: advisory). */
    @get:Input
    abstract val acceptanceStrict: Property<Boolean>

    /** Material audit report(s). Tolerant: absent means the material layer is unverified. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val materialReports: ConfigurableFileCollection

    /** Moodle audit report(s). Tolerant: absent means the Moodle layer is unverified. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val moodleReports: ConfigurableFileCollection

    /** The acceptance report written next to the audits. */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    init {
        group = "academy"
        description = "Aggregates the material and Moodle audits into the box acceptance report (ACADEMY-15 / GBL-011-5)"
    }

    @TaskAction
    fun audit() {
        if (!acceptanceEnabled.get()) return
        val material = parseFirst(materialReports.files)
        val moodle = parseFirst(moodleReports.files)
        val report = AcademyA11yAcceptance.accept(material, moodle)
        reportFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(AcademyA11yReport.toJson(report))

        logger.lifecycle(
            "[academy] a11y acceptance: ${report.readiness} " +
                "(${report.layers.size} layer(s), ${report.findings().size} findings) -> " +
                reportFile.get().asFile.absolutePath,
        )
        report.layers.forEach { layer ->
            logger.lifecycle("[academy] a11y layer ${layer.layer}: ${layer.readiness} (${layer.findings.size} findings)")
        }
        report
            .findings()
            .filter { it.severity == A11ySeverity.ERROR }
            .forEach { finding ->
                logger.warn("[academy] a11y ERROR: ${finding.rule} @ ${finding.location} - ${finding.message}")
            }
        report.limits.forEach { limit -> logger.info("[academy] a11y limit: $limit") }

        val errors = report.findings().count { it.severity == A11ySeverity.ERROR }
        if (acceptanceStrict.get() && errors > 0) {
            throw GradleException(
                "[academy] accessibility acceptance failed in strict mode ($errors errors) - " +
                    "see ${reportFile.get().asFile.absolutePath}",
            )
        }
    }

    private fun parseFirst(files: Set<File>): MaterialA11yReport? =
        files.firstNotNullOfOrNull { file ->
            try {
                MaterialA11yReport.fromJson(file.readText())
            } catch (_: Exception) {
                null
            }
        }

    private fun AcademyA11yReport.findings() = layers.flatMap { it.findings }
}

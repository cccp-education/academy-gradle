package education.cccp.academy

import education.cccp.academy.a11y.A11ySeverity
import education.cccp.academy.a11y.MaterialA11yAuditor
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
 * ACADEMY-15 — audits the accessibility of the pulled training material.
 *
 * Thin-wrapper task (pattern codex `CollectPageProvenanceTask`): the Gradle
 * resolution lives here, the audit is the pure [MaterialA11yAuditor]. Opt-in
 * (`academyInstaller.a11yAuditEnabled`, default `false`) and advisory by default:
 * findings are reported, and only `a11yStrict` turns an `ERROR` into a build
 * failure. An absent material directory is a legitimate `MISSING` report, never
 * a crash (the resource is generated before the learner pulls).
 */
@DisableCachingByDefault(because = "Reads the learner material directory, which lives outside the build cache (ACADEMY-15)")
abstract class AuditTrainingAccessibilityTask : DefaultTask() {
    /** Opt-in switch — mirrors `academyInstaller.a11yAuditEnabled`. */
    @get:Input
    abstract val a11yAuditEnabled: Property<Boolean>

    /** When true, an `ERROR` finding fails the build (default: advisory). */
    @get:Input
    abstract val a11yStrict: Property<Boolean>

    /** Directory holding the pulled material. Tolerant: absent means `MISSING`. */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    abstract val materialFiles: ConfigurableFileCollection

    /** The report written next to the audit. */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    init {
        group = "academy"
        description = "Audits the accessibility of the pulled training material (ACADEMY-15)"
    }

    @TaskAction
    fun audit() {
        if (!a11yAuditEnabled.get()) return
        val materialDir: File? = materialFiles.files.firstOrNull { it.isDirectory }
        val report = MaterialA11yAuditor.audit(materialDir)
        reportFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(MaterialA11yReport.toJson(report))

        logger.lifecycle(
            "[academy] a11y audit: ${report.readiness} (${report.findings.size} findings) -> " +
                reportFile.get().asFile.absolutePath,
        )
        report.findings.forEach { finding ->
            val line = "[academy] a11y ${finding.severity}: ${finding.rule} @ ${finding.location} - ${finding.message}"
            if (finding.severity == A11ySeverity.ERROR) logger.warn(line) else logger.info(line)
        }

        val errors = report.findings.count { it.severity == A11ySeverity.ERROR }
        if (a11yStrict.get() && errors > 0) {
            throw GradleException(
                "[academy] accessibility audit failed in strict mode ($errors errors) - " +
                    "see ${reportFile.get().asFile.absolutePath}",
            )
        }
    }
}

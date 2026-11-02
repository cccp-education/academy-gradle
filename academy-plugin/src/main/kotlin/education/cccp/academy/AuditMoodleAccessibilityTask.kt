package education.cccp.academy

import education.cccp.academy.a11y.A11ySeverity
import education.cccp.academy.a11y.HttpMoodlePageFetcher
import education.cccp.academy.a11y.MaterialA11yReport
import education.cccp.academy.a11y.MoodleA11yAuditor
import education.cccp.academy.a11y.MoodlePage
import education.cccp.academy.a11y.MoodlePageFetcher
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * ACADEMY-15 (GBL-011-5) — audits the accessibility of the running Moodle box.
 *
 * Thin-wrapper task (pattern `CollectPageProvenanceTask`): the network lives
 * behind the injected [MoodlePageFetcher], the verdict is the pure
 * [MoodleA11yAuditor]. Opt-in (`academyInstaller.moodleA11yEnabled`, default
 * `false`) and advisory by default: findings are reported, and only
 * `moodleA11yStrict` turns an `ERROR` into a build failure. A box that is not
 * running yields unreachable-page warnings, never a crash.
 */
@DisableCachingByDefault(because = "Fetches the running Moodle box over HTTP (ACADEMY-15)")
abstract class AuditMoodleAccessibilityTask : DefaultTask() {
    /** Opt-in switch — mirrors `academyInstaller.moodleA11yEnabled`. */
    @get:Input
    abstract val moodleA11yEnabled: Property<Boolean>

    /** When true, an `ERROR` finding fails the build (default: advisory). */
    @get:Input
    abstract val moodleA11yStrict: Property<Boolean>

    /** Base URL of the running Moodle (no trailing slash required). */
    @get:Input
    abstract val moodleA11yBaseUrl: Property<String>

    /** Page paths to audit, relative to the base URL. */
    @get:Input
    abstract val moodleA11yPaths: ListProperty<String>

    /** The report written next to the audit. */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    /** The HTTP port — the only network access of the audit (injectable). */
    @get:Internal
    abstract val fetcher: Property<MoodlePageFetcher>

    init {
        group = "academy"
        description = "Audits the accessibility of the running Moodle box (ACADEMY-15 / GBL-011-5)"
        fetcher.convention(HttpMoodlePageFetcher())
    }

    @TaskAction
    fun audit() {
        if (!moodleA11yEnabled.get()) return
        val baseUrl = moodleA11yBaseUrl.get().trimEnd('/')
        val pages =
            moodleA11yPaths.get().map { path ->
                val url = "$baseUrl/${path.trimStart('/')}"
                val html = fetcher.get().fetch(url)
                MoodlePage(location = url, html = html.orEmpty(), reachable = html != null)
            }
        val report = MoodleA11yAuditor.audit(pages)
        reportFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(MaterialA11yReport.toJson(report))

        logger.lifecycle(
            "[academy] Moodle a11y audit: ${report.readiness} (${report.findings.size} findings) -> " +
                reportFile.get().asFile.absolutePath,
        )
        report.findings.forEach { finding ->
            val line = "[academy] Moodle a11y ${finding.severity}: ${finding.rule} @ ${finding.location} - ${finding.message}"
            if (finding.severity == A11ySeverity.ERROR) logger.warn(line) else logger.info(line)
        }

        val errors = report.findings.count { it.severity == A11ySeverity.ERROR }
        if (moodleA11yStrict.get() && errors > 0) {
            throw GradleException(
                "[academy] Moodle accessibility audit failed in strict mode ($errors errors) - " +
                    "see ${reportFile.get().asFile.absolutePath}",
            )
        }
    }
}

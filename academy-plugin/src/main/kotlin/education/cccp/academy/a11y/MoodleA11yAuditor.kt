package education.cccp.academy.a11y

/**
 * ACADEMY-15 (GBL-011-5) — one Moodle page as observed by the audit.
 *
 * `html` is the body the server returned; an unreachable page carries
 * `reachable = false` and an empty body, so the audit can declare the gap
 * instead of inventing a result.
 */
data class MoodlePage(
    val location: String,
    val html: String = "",
    val reachable: Boolean = true,
)

/**
 * ACADEMY-15 (GBL-011-5) — the Moodle accessibility verdict.
 *
 * The audit aggregates the static findings of every observed page. An
 * unreachable page is a declared `WARNING`, never a crash: the box may not be
 * running when the audit is invoked, and the gate must say so. The report
 * reuses [MaterialA11yReport] (versioned JSON) so every accessibility report of
 * the box shares one contract.
 */
object MoodleA11yAuditor {
    /** What this audit does not measure, always declared in the report. */
    val LIMITS =
        listOf(
            "Static HTML only: no browser, no JavaScript, no focus order and no contrast measurement",
            "Only the configured Moodle paths are audited, not every course page",
            "opencode terminal (TUI) accessibility is not measured by this task",
        )

    /** Audits the observed [pages], or reports `MISSING` when none was observed. */
    fun audit(pages: List<MoodlePage>): MaterialA11yReport {
        if (pages.isEmpty()) {
            return MaterialA11yReport(readiness = A11yReadiness.MISSING, limits = LIMITS)
        }

        val findings =
            pages.flatMap { page ->
                if (page.reachable) {
                    MoodleAccessibilityInspector.inspect(page.html, page.location)
                } else {
                    listOf(
                        A11yFinding(
                            deliverable = A11yDeliverable.MOODLE,
                            rule = A11yRules.MOODLE_PAGE_UNREACHABLE,
                            severity = A11ySeverity.WARNING,
                            location = page.location,
                            message = "the page could not be fetched: its accessibility is unverified",
                        ),
                    )
                }
            }

        return MaterialA11yReport(
            readiness = if (findings.isEmpty()) A11yReadiness.VERIFIED else A11yReadiness.DEGRADED,
            findings = findings,
            limits = LIMITS,
        )
    }
}

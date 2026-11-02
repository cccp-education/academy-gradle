package education.cccp.academy.a11y

/**
 * ACADEMY-15 (GBL-011-5) — the port that fetches one Moodle page.
 *
 * `null` means the page could not be fetched (network, status, timeout): the
 * auditor turns it into a declared `WARNING`, never a crash. The adapter is the
 * only place where the audit touches the network, so the task is testable with
 * a fake.
 */
fun interface MoodlePageFetcher {
    fun fetch(url: String): String?
}

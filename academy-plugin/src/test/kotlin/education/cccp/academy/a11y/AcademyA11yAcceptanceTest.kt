package education.cccp.academy.a11y

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 / GBL-011-5 — the box acceptance verdict.
 *
 * The acceptance aggregates the two machine audits of the box (material and
 * Moodle). A missing layer is a declared gap, never a silent "verified"; the
 * aggregate always declares what it did not measure, including the usage test
 * with a concerned person that no machine audit can replace.
 */
class AcademyA11yAcceptanceTest {
    private val verified = MaterialA11yReport(readiness = A11yReadiness.VERIFIED)

    private fun finding(
        rule: String = "any-rule",
        severity: A11ySeverity = A11ySeverity.WARNING,
    ) = A11yFinding(
        deliverable = A11yDeliverable.MOODLE,
        rule = rule,
        severity = severity,
        location = "http://localhost:8080/login/index.php",
        message = "finding",
    )

    @Test
    fun `no audit report is a missing acceptance`() {
        val report = AcademyA11yAcceptance.accept(material = null, moodle = null)

        assertEquals(A11yReadiness.MISSING, report.readiness)
        assertTrue(report.layers.isEmpty())
        assertTrue(report.limits.isNotEmpty())
    }

    @Test
    fun `two verified audits are verified`() {
        val report = AcademyA11yAcceptance.accept(material = verified, moodle = verified)

        assertEquals(A11yReadiness.VERIFIED, report.readiness)
        assertEquals(listOf("material", "moodle"), report.layers.map { it.layer })
        assertTrue(report.findings().isEmpty())
    }

    @Test
    fun `an error finding degrades the acceptance`() {
        val material = verified.copy(findings = listOf(finding(severity = A11ySeverity.ERROR)))

        val report = AcademyA11yAcceptance.accept(material = material, moodle = verified)

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.findings().any { it.severity == A11ySeverity.ERROR })
    }

    @Test
    fun `a warning finding also degrades the acceptance`() {
        val moodle = verified.copy(findings = listOf(finding(severity = A11ySeverity.WARNING)))

        val report = AcademyA11yAcceptance.accept(material = verified, moodle = moodle)

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
    }

    @Test
    fun `a missing layer is a declared gap, never a silent verified`() {
        val report = AcademyA11yAcceptance.accept(material = verified, moodle = null)

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.limits.any { it.contains("moodle") })
    }

    @Test
    fun `a missing material report is a declared gap`() {
        val report = AcademyA11yAcceptance.accept(material = null, moodle = verified)

        assertEquals(A11yReadiness.DEGRADED, report.readiness)
        assertTrue(report.limits.any { it.contains("material") })
    }

    @Test
    fun `the aggregate declares the usage test and the opencode limit`() {
        val report = AcademyA11yAcceptance.accept(material = verified, moodle = verified)

        assertTrue(report.limits.any { it.contains("concerned person") })
        assertTrue(report.limits.any { it.contains("opencode") })
    }

    @Test
    fun `the aggregate states the opencode limit once`() {
        val audit = verified.copy(limits = listOf("opencode terminal is not audited by this layer"))

        val report = AcademyA11yAcceptance.accept(material = audit, moodle = audit)

        assertEquals(1, report.limits.count { it.contains("opencode") })
    }

    @Test
    fun `the acceptance is deterministic and deduplicates limits`() {
        val twice = verified.copy(limits = listOf("same limit"))

        val first = AcademyA11yAcceptance.accept(material = twice, moodle = twice)
        val second = AcademyA11yAcceptance.accept(material = twice, moodle = twice)

        assertEquals(first, second)
        assertEquals(1, first.limits.count { it == "same limit" })
    }

    private fun AcademyA11yReport.findings(): List<A11yFinding> = layers.flatMap { it.findings }
}

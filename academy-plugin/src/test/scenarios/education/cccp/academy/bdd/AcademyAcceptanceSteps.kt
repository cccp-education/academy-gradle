package education.cccp.academy.bdd

import education.cccp.academy.a11y.A11yDeliverable
import education.cccp.academy.a11y.A11yFinding
import education.cccp.academy.a11y.A11yReadiness
import education.cccp.academy.a11y.A11ySeverity
import education.cccp.academy.a11y.AcademyA11yAcceptance
import education.cccp.academy.a11y.AcademyA11yReport
import education.cccp.academy.a11y.MaterialA11yReport
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_acceptance.feature` (ACADEMY-15 / GBL-011-5) — pattern
 * S-088 (feature-scoped glue). Drives the pure acceptance aggregator directly:
 * zero Gradle task, zero I/O.
 *
 * Every step is prefixed with the `acceptance` vocabulary: the glue path is
 * shared with the other academy step classes, so unprefixed phrases would
 * collide (lesson S-088).
 */
class AcademyAcceptanceSteps : En {
    private var material: MaterialA11yReport? = null
    private var moodle: MaterialA11yReport? = null
    private var report: AcademyA11yReport? = null

    init {
        Given("a verified material audit report") {
            material = MaterialA11yReport(readiness = A11yReadiness.VERIFIED)
            moodle = null
            report = null
        }

        Given("a verified Moodle audit report") {
            moodle = MaterialA11yReport(readiness = A11yReadiness.VERIFIED)
            report = null
        }

        Given("no accessibility audit report") {
            material = null
            moodle = null
            report = null
        }

        Given("the material audit report contains the error rule {string}") { rule: String ->
            val current = checkNotNull(material) { "a material report must be set first" }
            material =
                current.copy(
                    readiness = A11yReadiness.DEGRADED,
                    findings =
                        listOf(
                            A11yFinding(
                                deliverable = A11yDeliverable.VIDEO,
                                rule = rule,
                                severity = A11ySeverity.ERROR,
                                location = "clip.mp4",
                                message = "finding",
                            ),
                        ),
                )
        }

        Given("a verified material audit report declaring the limit {string}") { limit: String ->
            material = MaterialA11yReport(readiness = A11yReadiness.VERIFIED, limits = listOf(limit))
            moodle = null
            report = null
        }

        Given("a verified Moodle audit report declaring the limit {string}") { limit: String ->
            moodle = MaterialA11yReport(readiness = A11yReadiness.VERIFIED, limits = listOf(limit))
            report = null
        }

        When("the box accessibility acceptance is computed") {
            report = AcademyA11yAcceptance.accept(material, moodle)
        }

        Then("the acceptance readiness is {string}") { expected: String ->
            assertThat(audited().readiness).isEqualTo(A11yReadiness.valueOf(expected))
        }

        Then("the acceptance has {int} layers") { count: Int ->
            assertThat(audited().layers).hasSize(count)
        }

        Then("the acceptance contains no finding") {
            assertThat(audited().layers.flatMap { it.findings }).isEmpty()
        }

        Then("the acceptance declares the missing {string} layer") { layer: String ->
            assertThat(audited().limits.any { it.contains(layer) && it.contains("not provided") }).isTrue()
        }

        Then("the acceptance contains the rule {string} with severity {string}") { rule: String, severity: String ->
            assertThat(
                audited()
                    .layers
                    .flatMap { it.findings }
                    .any { it.rule == rule && it.severity.name == severity },
            ).isTrue()
        }

        Then("the acceptance declares the limit {string} once") { limit: String ->
            assertThat(audited().limits.count { it == limit }).isEqualTo(1)
        }

        Then("the acceptance declares the usage test limit") {
            assertThat(audited().limits.any { it.contains("concerned person") }).isTrue()
        }

        Then("the acceptance declares the opencode limit") {
            assertThat(audited().limits.any { it.contains("opencode") }).isTrue()
        }
    }

    private fun audited(): AcademyA11yReport = checkNotNull(report) { "no acceptance computed yet" }
}

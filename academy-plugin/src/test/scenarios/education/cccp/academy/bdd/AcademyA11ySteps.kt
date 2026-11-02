package education.cccp.academy.bdd

import education.cccp.academy.a11y.A11yDeliverable
import education.cccp.academy.a11y.A11yReadiness
import education.cccp.academy.a11y.MaterialA11yAuditor
import education.cccp.academy.a11y.MaterialA11yReport
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import java.io.File

/**
 * Steps for `academy_a11y.feature` (ACADEMY-15) — pattern S-088 (feature-scoped
 * glue). Drives the pure material auditor directly: zero Gradle task invocation,
 * zero media decoding, zero browser.
 *
 * Every step is prefixed with the `accessibility` vocabulary: the glue path is
 * shared with the installer/opencode/byok/bridge/material/moodle/env steps, so
 * unprefixed phrases would collide (lesson S-088). The material lives under a
 * deterministic directory so a scenario starts from a known state.
 */
class AcademyA11ySteps : En {
    private val material = File("build/cucumber-a11y-material")
    private var report: MaterialA11yReport? = null

    init {
        Given("an empty training material for accessibility") {
            reset()
        }

        Given("a training material with the video {string} lacking its accessibility companions") { name: String ->
            reset()
            file(name)
        }

        Given("a training material with the video {string} carrying subtitles, transcript and audio description") { name: String ->
            reset()
            file(name)
            val base = name.substringBeforeLast('.')
            listOf("$base.vtt", "$base.txt", "$base.audiodesc.txt").forEach { file(it) }
        }

        Given("a training material with the text deliverable {string}") { name: String ->
            reset()
            file(name)
        }

        Given("the training material also contains the text deliverable {string}") { name: String ->
            file(name)
        }

        Given("a producer accessibility report covering {string} with no finding") { kind: String ->
            file("a11y-report.json", """{ "deliverables": [ { "kind": "$kind", "findings": [], "limits": [] } ] }""")
        }

        Given("a producer accessibility report covering {string} declaring the finding {string}") { kind: String, rule: String ->
            file(
                "a11y-report.json",
                """
                {
                  "deliverables": [
                    {
                      "kind": "$kind",
                      "findings": [
                        { "rule": "$rule", "severity": "ERROR", "location": "cours.html", "message": "producer finding" }
                      ],
                      "limits": ["PDF not tagged"]
                    }
                  ]
                }
                """.trimIndent(),
            )
        }

        Given("a malformed producer accessibility report") {
            file("a11y-report.json", "{ not json")
        }

        When("the material accessibility is audited") {
            report = MaterialA11yAuditor.audit(material)
        }

        Then("the accessibility readiness is {string}") { expected: String ->
            assertThat(audited().readiness).isEqualTo(A11yReadiness.valueOf(expected))
        }

        Then("the accessibility report contains the rule {string} with severity {string}") { rule: String, severity: String ->
            assertThat(audited().findings.any { it.rule == rule && it.severity.name == severity })
                .describedAs("expected a finding %s/%s in %s", rule, severity, audited().findings)
                .isTrue()
        }

        Then("the accessibility report contains no finding") {
            assertThat(audited().findings).isEmpty()
        }

        Then("the accessibility report names the uncovered deliverable {string}") { deliverable: String ->
            assertThat(
                audited().findings.any {
                    it.rule == "producer-report-incomplete" && it.deliverable == A11yDeliverable.valueOf(deliverable)
                },
            ).isTrue()
        }

        Then("the accessibility report declares the limit {string}") { limit: String ->
            assertThat(audited().limits).contains(limit)
        }

        Then("the accessibility report declares its limits") {
            assertThat(audited().limits).isNotEmpty()
        }

        Then("the accessibility report declares a malformed producer report") {
            assertThat(audited().limits.any { it.contains("Malformed") }).isTrue()
        }
    }

    private fun audited(): MaterialA11yReport = checkNotNull(report) { "no audit ran yet" }

    private fun reset() {
        material.deleteRecursively()
        material.mkdirs()
        report = null
    }

    private fun file(
        relative: String,
        content: String = "x",
    ) {
        File(material, relative).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}

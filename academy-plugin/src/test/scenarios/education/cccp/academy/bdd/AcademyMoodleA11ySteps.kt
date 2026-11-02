package education.cccp.academy.bdd

import education.cccp.academy.a11y.A11yReadiness
import education.cccp.academy.a11y.MaterialA11yReport
import education.cccp.academy.a11y.MoodleA11yAuditor
import education.cccp.academy.a11y.MoodlePage
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_moodle_a11y.feature` (ACADEMY-15 / GBL-011-5) — pattern
 * S-088 (feature-scoped glue). Drives the pure Moodle auditor directly: zero
 * HTTP, zero container, zero browser.
 *
 * Every step is prefixed with the `Moodle` vocabulary: the glue path is shared
 * with the other academy step classes, so unprefixed phrases would collide
 * (lesson S-088).
 */
class AcademyMoodleA11ySteps : En {
    private val location = "http://localhost:8080/login/index.php"
    private var pages: MutableList<MoodlePage> = mutableListOf()
    private var report: MaterialA11yReport? = null

    private val accessiblePage =
        """
        <!DOCTYPE html>
        <html lang="fr">
          <body>
            <h1>Connexion</h1>
            <label for="username">Nom d'utilisateur</label>
            <input id="username" name="username" type="text">
            <table><thead><tr><th>Module</th></tr></thead><tbody><tr><td>SPG</td></tr></tbody></table>
          </body>
        </html>
        """.trimIndent()

    init {
        Given("a Moodle page with a lang, a heading, a labelled control and a table with a header") {
            pages = mutableListOf(MoodlePage(location = location, html = accessiblePage))
            report = null
        }

        Given("an unreachable Moodle page") {
            pages = mutableListOf(MoodlePage(location = location, reachable = false))
            report = null
        }

        Given("no Moodle page is observed") {
            pages = mutableListOf()
            report = null
        }

        And("the Moodle page has no lang attribute") {
            mutate { it.replace("<html lang=\"fr\">", "<html>") }
        }

        And("the Moodle page has an image without an alt attribute") {
            mutate { it.replace("<h1>Connexion</h1>", "<h1>Connexion</h1><img src=\"logo.png\">") }
        }

        And("the Moodle page has a form control without an accessible name") {
            mutate { it.replace("<label for=\"username\">Nom d'utilisateur</label>", "") }
        }

        And("the Moodle page has a table without a header cell") {
            mutate { it.replace("<thead><tr><th>Module</th></tr></thead>", "") }
        }

        When("the Moodle accessibility is audited") {
            report = MoodleA11yAuditor.audit(pages)
        }

        Then("the Moodle accessibility readiness is {string}") { expected: String ->
            assertThat(audited().readiness).isEqualTo(A11yReadiness.valueOf(expected))
        }

        Then("the Moodle accessibility report contains the rule {string} with severity {string}") { rule: String, severity: String ->
            assertThat(audited().findings.any { it.rule == rule && it.severity.name == severity })
                .describedAs("expected a finding %s/%s in %s", rule, severity, audited().findings)
                .isTrue()
        }

        Then("the Moodle accessibility report contains no finding") {
            assertThat(audited().findings).isEmpty()
        }

        Then("the Moodle accessibility report declares its limits") {
            assertThat(audited().limits).isNotEmpty()
        }

        Then("the Moodle accessibility report declares that static HTML is its limit") {
            assertThat(audited().limits.any { it.contains("Static HTML") }).isTrue()
        }

        Then("the Moodle accessibility report declares that opencode is out of scope") {
            assertThat(audited().limits.any { it.contains("opencode") }).isTrue()
        }
    }

    private fun audited(): MaterialA11yReport = checkNotNull(report) { "no audit ran yet" }

    private fun mutate(transform: (String) -> String) {
        val page = pages.first()
        pages[0] = page.copy(html = transform(page.html))
    }
}

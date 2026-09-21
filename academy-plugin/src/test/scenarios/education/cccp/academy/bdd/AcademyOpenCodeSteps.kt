package education.cccp.academy.bdd

import education.cccp.academy.opencode.LearnerGuideGenerator
import education.cccp.academy.opencode.OpenCodeCatalog
import education.cccp.academy.opencode.OpenCodeConfig
import education.cccp.academy.opencode.OpenCodeConfigGenerator
import education.cccp.academy.opencode.WorkspaceDockerfileGenerator
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_opencode.feature` (ACADEMY-5-1) — pattern S-088
 * (feature-scoped glue). Drives the pure opencode generators directly: zero
 * Gradle task invocation, zero I/O, pure domain BDD (pattern codex). Cucumber
 * re-instantiates the glue per scenario, so instance fields reset naturally.
 */
class AcademyOpenCodeSteps : En {

    private var providerUrl: String = "http://ollama:11434/v1"
    private var model: String = "gpt-oss:120b-cloud"
    private var json: String = ""
    private var guide: String = ""
    private var dockerfile: String = ""
    private var exposureRoot: String = ""

    init {
        Given("an opencode config with provider url {string}") { url: String -> providerUrl = url }
        And("model {string}") { name: String -> model = name }

        When("the opencode config generator renders the config") {
            json = OpenCodeConfigGenerator.render(config())
        }

        When("the learner guide generator renders the guide") {
            guide = LearnerGuideGenerator.render(config())
        }

        When("the workspace dockerfile generator renders the dockerfile") {
            dockerfile = WorkspaceDockerfileGenerator.render(config())
        }

        Then("the json contains {string}") { fragment: String ->
            assertThat(json).contains(fragment)
        }

        And("the json braces balance") {
            assertThat(json.count { it == '{' }).isEqualTo(json.count { it == '}' })
            assertThat(json.count { it == '[' }).isEqualTo(json.count { it == ']' })
            assertThat(json).doesNotContain(",}")
        }

        And("the json contains no credential") {
            listOf("API_KEY", "SECRET", "TOKEN", "PASSWORD").forEach { secret ->
                assertThat(json).doesNotContain(secret)
            }
        }

        Then("the guide contains {string}") { fragment: String ->
            assertThat(guide).contains(fragment)
        }

        And("the guide does not contain {string}") { fragment: String ->
            assertThat(guide).doesNotContain(fragment)
        }

        Then("the dockerfile contains {string}") { fragment: String ->
            assertThat(dockerfile).contains(fragment)
        }

        When("the exposure guardrail contract is read") {
            exposureRoot = OpenCodeCatalog.exposure().publicBoroughsRoot
        }

        Then("the exposure guardrail names the public root {string}") { root: String ->
            assertThat(exposureRoot).isEqualTo(root)
        }
    }

    private fun config() = OpenCodeConfig(providerUrl = providerUrl, model = model)
}

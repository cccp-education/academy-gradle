package education.cccp.academy.bdd

import contracts.runtime.LlmProviderKind
import education.cccp.academy.opencode.LearnerGuideGenerator
import education.cccp.academy.opencode.OpenCodeConfig
import education.cccp.academy.opencode.OpenCodeConfigGenerator
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_byok.feature` (ACADEMY-7-3) — pattern S-088
 * (feature-scoped glue). Drives the pure opencode generators directly: zero
 * Gradle task invocation, zero I/O, pure domain BDD (pattern academy S-009).
 * Cucumber re-instantiates the glue per scenario, so instance fields reset
 * naturally.
 *
 * Every step is prefixed with the `byok` vocabulary: the glue path is shared
 * with [AcademyOpenCodeSteps] and [AcademyInstallerSteps], so unprefixed
 * phrases such as `model {string}` or `the json contains {string}` would
 * collide and raise an ambiguous-step error (lesson S-088).
 */
class AcademyByokSteps : En {

    private var provider: LlmProviderKind = LlmProviderKind.OLLAMA_LOCAL
    private var model: String = "gpt-oss:120b-cloud"
    private var providerUrl: String = "http://ollama:11434/v1"
    private var apiKeyEnvVar: String? = null
    private var json: String = ""
    private var guide: String = ""

    init {
        Given("a byok config for provider {string}") { name: String ->
            provider = LlmProviderKind.valueOf(name)
            if (provider == LlmProviderKind.CUSTOM && apiKeyEnvVar == null) {
                apiKeyEnvVar = "MY_PROVIDER_KEY"
            }
        }
        And("a byok model {string}") { name: String -> model = name }
        And("a byok provider url {string}") { url: String -> providerUrl = url }
        And("a byok api key env var {string}") { name: String -> apiKeyEnvVar = name }

        When("the byok config generator renders the config") {
            json = OpenCodeConfigGenerator.render(config())
        }

        When("the byok learner guide generator renders the guide") {
            guide = LearnerGuideGenerator.render(config())
        }

        Then("the byok json contains {string}") { fragment: String ->
            assertThat(json).contains(fragment)
        }

        And("the byok json does not contain {string}") { fragment: String ->
            assertThat(json).doesNotContain(fragment)
        }

        And("the byok json braces balance") {
            assertThat(json.count { it == '{' }).isEqualTo(json.count { it == '}' })
            assertThat(json.count { it == '[' }).isEqualTo(json.count { it == ']' })
            assertThat(json).doesNotContain(",}")
            assertThat(json).doesNotContain(",]")
        }

        And("the byok json contains no credential") {
            listOf("sk-", "ghp_", "gho_", "AIza").forEach { secret ->
                assertThat(json).doesNotContain(secret)
            }
        }

        And("the byok json never references an api key") {
            assertThat(json).doesNotContain("\"apiKey\"")
        }

        Then("the byok guide contains {string}") { fragment: String ->
            assertThat(guide).contains(fragment)
        }
    }

    private fun config() = OpenCodeConfig(
        providerUrl = providerUrl,
        model = model,
        provider = provider,
        apiKeyEnvVar = apiKeyEnvVar,
    )
}

package education.cccp.academy.bdd

import education.cccp.academy.bridge.BridgeEventTranslator
import education.cccp.academy.bridge.BridgeRoutes
import education.cccp.academy.bridge.MoodleEvent
import contracts.session.SessionPrompt
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_bridge.feature` (ACADEMY-8-3) — pattern S-088
 * (feature-scoped glue). Drives the pure bridge domain directly: zero HTTP,
 * zero Gradle task, zero I/O. The Ktor layer is covered by the
 * `testApplication` suite (D-ACADEMY-8-10).
 *
 * Steps are prefixed with the `bridge` vocabulary: the glue path is shared with
 * the installer/opencode/byok steps, so unprefixed phrases would collide
 * (lesson S-088).
 */
class AcademyBridgeSteps : En {

    private var event: MoodleEvent? = null
    private var second: MoodleEvent? = null
    private var prompt: SessionPrompt? = null
    private var otherPrompt: SessionPrompt? = null
    private var promptAgain: SessionPrompt? = null

    init {
        Given("a moodle event {string} for course {string} and learner {string}") { name: String, course: String, learner: String ->
            event = MoodleEvent(eventName = name, courseId = course, userId = learner)
        }
        And("a second moodle event {string} for course {string} and learner {string}") { name: String, course: String, learner: String ->
            second = MoodleEvent(eventName = name, courseId = course, userId = learner)
        }
        And("the event payload {string} is {string}") { key: String, value: String ->
            event = checkNotNull(event).let { it.copy(payload = it.payload + (key to value)) }
        }

        When("the bridge translator builds the session prompt") {
            prompt = BridgeEventTranslator.toSessionPrompt(checkNotNull(event))
        }
        When("the bridge translator builds the session prompt twice") {
            val e = checkNotNull(event)
            prompt = BridgeEventTranslator.toSessionPrompt(e)
            promptAgain = BridgeEventTranslator.toSessionPrompt(e)
        }
        When("both bridge events are translated") {
            prompt = BridgeEventTranslator.toSessionPrompt(checkNotNull(event))
            otherPrompt = BridgeEventTranslator.toSessionPrompt(checkNotNull(second))
        }
        When("the bridge route table is read") {
            assertThat(BridgeRoutes.routes).isNotEmpty()
        }

        Then("the bridge prompt contains {string}") { fragment: String ->
            assertThat(checkNotNull(prompt).prompt).contains(fragment)
        }
        And("the bridge prompt does not claim {string}") { fragment: String ->
            assertThat(checkNotNull(prompt).prompt).doesNotContain(fragment)
        }
        And("the bridge context contains {string}") { fragment: String ->
            assertThat(checkNotNull(prompt).context?.eagerRules).contains(fragment)
        }
        And("the bridge context is empty") {
            assertThat(checkNotNull(prompt).context?.eagerRules).isEqualTo("")
        }
        Then("the two bridge session ids are equal") {
            assertThat(checkNotNull(prompt).sessionId).isEqualTo(checkNotNull(promptAgain).sessionId)
        }
        Then("the two bridge session ids differ") {
            assertThat(checkNotNull(prompt).sessionId).isNotEqualTo(checkNotNull(otherPrompt).sessionId)
        }
        Then("the bridge declares route {string}") { path: String ->
            assertThat(BridgeRoutes.routes.map { it.path }).contains(path)
        }
        And("the bridge health route is a {string}") { method: String ->
            assertThat(BridgeRoutes.methodFor(BridgeRoutes.HEALTH.path)).isEqualTo(method)
        }
        And("the bridge event route is a {string}") { method: String ->
            assertThat(BridgeRoutes.methodFor(BridgeRoutes.MOODLE_EVENT.path)).isEqualTo(method)
        }
    }
}

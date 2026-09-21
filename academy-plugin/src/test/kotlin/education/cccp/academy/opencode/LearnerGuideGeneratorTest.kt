package education.cccp.academy.opencode

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * ACADEMY-5-1 — the learner guide (`AGENTS.md`) is opencode's official anchor
 * file. It must teach the learner the real workflow (compose up → opencode →
 * a Gradle task) and inventory the exposed ecosystem tasks from the catalog.
 */
class LearnerGuideGeneratorTest {

    @Test
    fun `renders the workspace identity and the agent workflow`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(providerUrl = "http://ollama:11434/v1", model = "gpt-oss:120b-cloud"),
        )

        assertTrue(guide.contains("Academy"))
        assertTrue(guide.contains("opencode"))
        assertTrue(guide.contains("docker compose"))
        assertTrue(guide.contains("./gradlew"))
    }

    @Test
    fun `inventories the exposed ecosystem tasks from the catalog`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(providerUrl = "http://ollama:11434/v1", model = "gpt-oss:120b-cloud"),
        )

        assertTrue(guide.contains("education.cccp.slider"), "the guide must list the slider plugin")
        assertTrue(guide.contains("generateDeck"), "the guide must list the slider entry point")
        assertTrue(guide.contains("bookPipeline"), "the guide must list the document pipeline")
    }

    @Test
    fun `states the public-only guardrail`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(providerUrl = "http://ollama:11434/v1", model = "gpt-oss:120b-cloud"),
        )

        assertTrue(guide.contains("foundry/public"), "the guide must document the guardrail")
        assertTrue(guide.contains("training"), "the guide must name what is NOT exposed")
    }

    @Test
    fun `never embeds a credential`() {
        val guide = LearnerGuideGenerator.render(
            OpenCodeConfig(providerUrl = "http://ollama:11434/v1", model = "gpt-oss:120b-cloud"),
        )

        listOf("API_KEY=", "SECRET=", "TOKEN=").forEach { secret ->
            assertTrue(!guide.contains(secret), "the guide must never embed $secret")
        }
    }
}

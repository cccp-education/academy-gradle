package education.cccp.academy.opencode

/**
 * Pure learner guide renderer (ACADEMY-5-1) — produces the `AGENTS.md` file,
 * opencode's official project anchor (opencode discovers `AGENTS.md`
 * automatically at the project root).
 *
 * The guide teaches the real workflow: bring the stack up, open the agent,
 * then drive the formation pipeline through the exposed Gradle tasks. The task
 * inventory is read from [OpenCodeCatalog] so documentation and configuration
 * can never drift apart (single source of truth).
 */
object LearnerGuideGenerator {

    /** Renders the learner `AGENTS.md`. */
    fun render(config: OpenCodeConfig): String {
        val taskInventory = OpenCodeCatalog.plugins.joinToString("\n") { plugin ->
            val tasks = plugin.tasks.joinToString(", ") { "`./gradlew $it`" }
            "- **${plugin.id}** — ${plugin.role}: $tasks"
        }
        return """
            |# Academy Workspace — Learner Guide
            |
            |Welcome. This workspace is the hands-on experimentation track of the
            |CCCP education pipeline. You drive it locally, without any VPS.
            |
            |## 1. Start the stack
            |
            |```sh
            |docker compose up -d
            |```
            |
            |This starts Moodle (the course), PostgreSQL (its database), Ollama
            |(the local LLM runtime) and Portainer (the Docker UI).
            |
            |## 2. Open the agent
            |
            |```sh
            |opencode
            |```
            |
            |The agent reads `AGENTS.md` (this file) and `opencode.json`, which
            |points it at the embedded Ollama runtime (${config.providerUrl},
            |model `${config.model}`).
            |
            |## 3. Drive the formation pipeline
            |
            |The agent exposes the public ecosystem tasks to you. A few entry points:
            |
            |```sh
            |./gradlew generateDeck
            |./gradlew bookPipeline
            |```
            |
            |## Exposed ecosystem tasks
            |
            |Only the public boroughs (`foundry/public`) are invocable. The
            |private boroughs — `training`, `edster`, `workspace`, `waiter`,
            |`quizz` — are never exposed to the learner.
            |
            |$taskInventory
            |
            |## Credentials
            |
            |Never put a key in this workspace. Provide your LLM provider key
            |through the environment when you configure bring-your-own-key
            |(BYOK) — the agent reads it from there, never from a file.
            |""".trimMargin()
    }
}

package education.cccp.academy.opencode

/**
 * Renders the **FPA certification workflow** for the learner (ACADEMY-16 US-3,
 * D-LWA-4) — the sequence that turns the published public tools into the
 * deliverables the jury evaluates.
 *
 * A deliberate boundary: the private `training` borough is **never exposed**.
 * The SPG and SPD are the candidate's **own** authored production (the jury
 * evaluates the candidate, not a generator); the public tools format the
 * multimedia and the documents. The guide names the exact opencode tools
 * (`cccp_<borough>`) and the exact Gradle tasks, read from the catalog, so the
 * documentation can never invent a command (S-007 lesson).
 */
object WorkflowGuideGenerator {
    fun render(plugins: List<OpenCodePlugin> = OpenCodeCatalog.plugins): String {
        val enabled = plugins.filter { it.version.isNotBlank() }

        fun tool(id: String): String = enabled.firstOrNull { it.id == id }?.let { "cccp_${OpenCodeToolGenerator.exportName(it.id)}" } ?: "(not enabled)"

        fun tasks(id: String): String =
            enabled
                .firstOrNull { it.id == id }
                ?.tasks
                ?.joinToString(", ") { "`$it`" }
                ?: "(not enabled)"

        val slider = tool("education.cccp.slider")
        val capsule = tool("education.cccp.capsule")
        val document = tool("education.cccp.document")
        val plantuml = tool("education.cccp.plantuml")

        return """
            |## Certification workflow (FPA)
            |
            |You produce the deliverables the jury evaluates. The private `training`
            |borough is **not exposed**: the SPG and SPD are **your own work**, written
            |in AsciiDoc with the agent's help. The public tools format the slides, the
            |capsule, the diagrams and the dossier.
            |
            |### 1. Frame the training — SPG, then SPD
            |
            |Write `spg.adoc` then `spd.adoc` in AsciiDoc, using the module templates
            |under `material/`. Ask the agent to review them against the AFNOR/REAC
            |criteria. There is no Gradle task here: this is the candidate's authored
            |deliverable.
            |
            |### 2. Slides — $slider
            |
            |Ask the agent to call the `$slider` tool with one of ${tasks("education.cccp.slider")}.
            |
            |### 3. Storyboard and video capsule — $capsule
            |
            |Write the storyboard in the deck speaker notes, then call the `$capsule`
            |tool with one of ${tasks("education.cccp.capsule")}.
            |
            |### 4. Technical dossier (PDF/EPUB/HTML) — $document
            |
            |Call the `$document` tool with one of ${tasks("education.cccp.document")}.
            |
            |### 5. Diagrams (optional) — $plantuml
            |
            |Call the `$plantuml` tool with one of ${tasks("education.cccp.plantuml")}.
            |
            |The agent runs each task with `gradle <task>` in this workspace; the tools
            |only wrap the published Gradle tasks. No borough business logic is
            |duplicated here.
            """.trimMargin()
    }
}

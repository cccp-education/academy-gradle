package education.cccp.academy.opencode

/**
 * The curated catalog of public ecosystem plugins exposed to the learner
 * (ACADEMY-5-1) — the single source of truth consumed by
 * [OpenCodeConfigGenerator] and [LearnerGuideGenerator].
 *
 * **Every entry is verified against the borough's source** (audit S-009), never
 * invented — cf. the S-007 `moodle:4.5` lesson (a plausible-but-inexistent
 * reference that passed tests). Only `foundry/public` boroughs are listed:
 * the guardrail is physical, `foundry/private` never appears here.
 */
object OpenCodeCatalog {

    /** Public ecosystem plugins with their formation entry points, in chain order. */
    val plugins: List<OpenCodePlugin> = listOf(
        OpenCodePlugin(
            id = "education.cccp.planner",
            role = "Decompose an intention into a structured execution plan",
            tasks = listOf("generatePlan"),
        ),
        OpenCodePlugin(
            id = "education.cccp.codex",
            role = "Acquire documents (PDF/EPUB) into a structured corpus (READ)",
            tasks = listOf("collectText", "collectBookStructure", "collectOcr", "generateCompositeContext"),
        ),
        OpenCodePlugin(
            id = "education.cccp.codebase",
            role = "Provide the shared RAG foundation and the augmented context",
            tasks = listOf("collectFromCodebase", "collectCompositeContext", "generateAugmentedPlan"),
        ),
        OpenCodePlugin(
            id = "education.cccp.slider",
            role = "Generate Reveal.js slide decks from the pedagogical material (SLIDES)",
            tasks = listOf("generateDeck", "generateDeckPipeline", "proposeDeckContext"),
        ),
        OpenCodePlugin(
            id = "education.cccp.document",
            role = "Create and publish the learner documents (PDF/EPUB/HTML)",
            tasks = listOf("bookPipeline", "generateDocument", "assembleBook", "validateDocument"),
        ),
        OpenCodePlugin(
            id = "education.cccp.capsule",
            role = "Capture narrated video capsules from the decks (VIDEO)",
            tasks = listOf("generateCapsuleVideo", "generateCapsule", "generateCapsuleScript"),
        ),
        OpenCodePlugin(
            id = "education.cccp.plantuml",
            role = "Generate the diagrams and the knowledge-graph visuals",
            tasks = listOf("generatePlantumlDiagrams", "generateDiagramDocs", "generateKnowledgeGraphDiagram"),
        ),
        OpenCodePlugin(
            id = "education.cccp.graphify",
            role = "Extract the knowledge graph of the workspace",
            tasks = listOf("collectAndVerify", "collectFromWorkspace"),
        ),
        OpenCodePlugin(
            id = "education.cccp.hyperframes",
            role = "Render AsciiDoc content to MP4 via HyperFrames",
            tasks = listOf("renderHyperframes", "generateHyperframesHtml"),
        ),
        OpenCodePlugin(
            id = "education.cccp.bakery",
            role = "Generate and deploy the static formation site",
            tasks = listOf("generateSite", "generateArticle", "bake"),
        ),
        OpenCodePlugin(
            id = "education.cccp.readme",
            role = "Generate the project README with PlantUML diagrams",
            tasks = listOf("generateReadme", "transformReadme"),
        ),
    )

    /** Entry points of one plugin id, or `null` when the plugin is not exposed. */
    fun tasksFor(pluginId: String): List<String>? =
        plugins.firstOrNull { it.id == pluginId }?.tasks

    /** Every exposed task, flattened in catalog order (deterministic). */
    val allTasks: List<String> get() = plugins.flatMap { it.tasks }
}

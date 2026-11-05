package education.cccp.academy.opencode

import contracts.runtime.ToolExposure

/**
 * The curated catalog of public ecosystem plugins exposed to the learner
 * (ACADEMY-5-1, versioned by ACADEMY-16) — the single source of truth consumed
 * by [OpenCodeConfigGenerator], [OpenCodeToolGenerator], [LearnerGuideGenerator]
 * and the workstation project generator.
 *
 * **Every entry is verified against the borough's source** (audit S-009) and
 * its published version against the workspace catalog (`workspace-bom`,
 * 2026-11-05), never invented — cf. the S-007 `moodle:4.5` lesson (a
 * plausible-but-inexistent reference that passed tests). Only `foundry/public`
 * boroughs are listed: the guardrail is physical, `foundry/private` never
 * appears here.
 *
 * A blank [OpenCodePlugin.version] means the plugin is **documented but
 * disabled**: it is not applied in the workstation build and gets no opencode
 * tool (ACADEMY-16, D-LWA-4).
 *
 * Since ACADEMY-11 the guardrail is also a **contract**, not a comment: see
 * [exposure], which reads the N0 `ToolExposure` the runtime declares.
 */
object OpenCodeCatalog {
    /**
     * The physical boundary the guardrail rests on — the single literal from
     * which [exposure] is built. `foundry/private` is the complementary
     * signal: a private borough can never satisfy this root.
     */
    const val PUBLIC_BOROUGHS_ROOT: String = "foundry/public"

    /**
     * The guardrail as the N0 `contracts.runtime.ToolExposure` contract
     * (ACADEMY-11) — the rule was documented in a KDoc and restated as prose by
     * [LearnerGuideGenerator], which is exactly the drift the shared contract
     * exists to prevent. Academy is the contract's consumer; the actual
     * enforcement lives in the private runtime (pilot N4).
     *
     * @param publicBoroughsRoot the public boundary, overridable for a caller
     *   that relocates the workspace (defaults to [PUBLIC_BOROUGHS_ROOT]).
     */
    fun exposure(publicBoroughsRoot: String = PUBLIC_BOROUGHS_ROOT): ToolExposure = ToolExposure(publicBoroughsRoot = publicBoroughsRoot)

    /** Public ecosystem plugins with their formation entry points, in chain order. */
    val plugins: List<OpenCodePlugin> =
        listOf(
            OpenCodePlugin(
                id = "education.cccp.planner",
                role = "Decompose an intention into a structured execution plan",
                tasks = listOf("generatePlan"),
                version = "0.0.2",
            ),
            OpenCodePlugin(
                id = "education.cccp.codex",
                role = "Acquire documents (PDF/EPUB) into a structured corpus (READ)",
                tasks = listOf("collectText", "collectBookStructure", "collectOcr", "generateCompositeContext"),
                version = "0.0.12",
            ),
            OpenCodePlugin(
                id = "education.cccp.codebase",
                role = "Provide the shared RAG foundation and the augmented context",
                tasks = listOf("collectFromCodebase", "collectCompositeContext", "generateAugmentedPlan"),
                version = "0.0.18",
            ),
            OpenCodePlugin(
                id = "education.cccp.slider",
                role = "Generate Reveal.js slide decks from the pedagogical material (SLIDES)",
                tasks = listOf("generateDeck", "generateDeckPipeline", "proposeDeckContext"),
                version = "0.0.20",
            ),
            OpenCodePlugin(
                id = "education.cccp.document",
                role = "Create and publish the learner documents (PDF/EPUB/HTML)",
                tasks = listOf("bookPipeline", "generateDocument", "assembleBook", "validateDocument"),
                version = "0.0.30",
            ),
            OpenCodePlugin(
                id = "education.cccp.capsule",
                role = "Capture narrated video capsules from the decks (VIDEO)",
                tasks = listOf("generateCapsuleVideo", "generateCapsule", "generateCapsuleScript"),
                version = "0.0.15",
            ),
            OpenCodePlugin(
                id = "education.cccp.plantuml",
                role = "Generate the diagrams and the knowledge-graph visuals",
                tasks = listOf("generatePlantumlDiagrams", "generateDiagramDocs", "generateKnowledgeGraphDiagram"),
                version = "0.0.16",
            ),
            OpenCodePlugin(
                id = "education.cccp.graphify",
                role = "Extract the knowledge graph of the workspace",
                tasks = listOf("collectAndVerify", "collectFromWorkspace"),
                version = "0.0.5",
            ),
            OpenCodePlugin(
                id = "education.cccp.hyperframes",
                role = "Render AsciiDoc content to MP4 via HyperFrames",
                tasks = listOf("renderHyperframes", "generateHyperframesHtml"),
                version = "0.0.14",
            ),
            OpenCodePlugin(
                id = "education.cccp.bakery",
                role = "Generate and deploy the static formation site",
                tasks = listOf("generateSite", "generateArticle", "bake"),
                version = "0.0.33",
            ),
            OpenCodePlugin(
                id = "education.cccp.readme",
                role = "Generate the project README with PlantUML diagrams",
                tasks = listOf("generateReadme", "transformReadme"),
                version = "0.0.2",
            ),
        )

    /** Only the plugins enabled for the learner workstation (non-blank version). */
    val enabledPlugins: List<OpenCodePlugin> get() = plugins.filter { it.version.isNotBlank() }

    /** Entry points of one plugin id, or `null` when the plugin is not exposed. */
    fun tasksFor(pluginId: String): List<String>? = plugins.firstOrNull { it.id == pluginId }?.tasks

    /** Every exposed task, flattened in catalog order (deterministic). */
    val allTasks: List<String> get() = plugins.flatMap { it.tasks }
}

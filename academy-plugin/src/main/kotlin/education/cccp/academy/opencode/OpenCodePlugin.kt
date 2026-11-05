package education.cccp.academy.opencode

/**
 * One public ecosystem plugin exposed to the learner, with its curated
 * formation entry points (ACADEMY-5-1, versioned by ACADEMY-16).
 *
 * The guardrail is physical: only `foundry/public` plugins ever appear here
 * (see [OpenCodeCatalog]); `foundry/private` (training, edster, workspace,
 * waiter, quizz) is never exposed to the learner (CADRAGE_EPIC_RUNTIME_AGENTIQUE
 * § 6 — the [contracts.runtime.ToolExposure] rule, consumed later by ACADEMY-2).
 *
 * @property id published Gradle plugin id (`education.cccp.*`, never blank)
 * @property role short human-readable role in the formation chain
 * @property tasks verified Gradle task entry points (never empty)
 * @property version published plugin version — **empty means disabled**. A
 *   disabled plugin is documented but neither applied in the workstation build
 *   nor exposed as an opencode tool (ACADEMY-16, D-LWA-4). Versions are verified
 *   against the workspace catalog (2026-11-05), never invented.
 */
data class OpenCodePlugin(
    val id: String,
    val role: String,
    val tasks: List<String>,
    val version: String = "",
) {
    init {
        require(id.isNotBlank()) { "plugin id must not be blank" }
        require(role.isNotBlank()) { "plugin role must not be blank" }
        require(tasks.isNotEmpty()) { "plugin $id must expose at least one task" }
        require(version.isBlank() || version.matches(Regex("^\\d+\\.\\d+\\.\\d+.*$"))) {
            "plugin $id version must be a semantic version, got: $version"
        }
    }
}

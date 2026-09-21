package education.cccp.academy.opencode

/**
 * Immutable, resolved configuration of the learner's opencode exposure
 * (ACADEMY-5-1) — the input of the pure [OpenCodeConfigGenerator],
 * [LearnerGuideGenerator] and [WorkspaceDockerfileGenerator].
 *
 * Built by the Gradle task from the lazy `Property`s of the `academyInstaller`
 * extension, so the domain stays free of Gradle types and unit-testable in
 * isolation (DDD pattern [education.cccp.academy.installer.InstallerPlatform]).
 *
 * Credentials are deliberately NOT modelled here: the learner supplies them
 * through the environment (BYOK, ACADEMY-7) — this data class describes only
 * where the agent points and which model it uses, never a value.
 *
 * @param providerUrl OpenAI-compatible base URL (usually the embedded ollama
 *   service of the compose scaffold, `http://ollama:11434/v1`)
 * @param model model id served by the provider (never blank)
 */
data class OpenCodeConfig(
    val providerUrl: String,
    val model: String,
) {
    init {
        require(providerUrl.isNotBlank()) { "providerUrl must not be blank" }
        require(model.isNotBlank()) { "model must not be blank" }
    }
}

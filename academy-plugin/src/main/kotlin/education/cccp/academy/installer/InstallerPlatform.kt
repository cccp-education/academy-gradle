package education.cccp.academy.installer

import contracts.runtime.LlmProviderKind
import education.cccp.academy.opencode.OpenCodeConfig

/**
 * Immutable, resolved configuration of one installer target — the input of the
 * pure [InstallerScriptGenerator].
 *
 * Built by the Gradle task from the lazy [org.gradle.api.provider.Property]s
 * of the `academyInstaller` extension at execution time, so the domain stays
 * free of Gradle types and unit-testable in isolation (DDD pattern codex).
 *
 * @param os target operating system
 * @param applicationName resource name (never blank)
 * @param applicationVersion distributed version (never blank)
 * @param javaVersion Java Temurin major version of the workspace image (digits only)
 * @param gradleVersion Gradle version pin of the workspace image (never blank)
 * @param composeEnabled whether the script embeds the docker-compose scaffold
 * @param credentialsEnvPrefix env prefix the credentials are read from
 * @param openCodeEnabled whether the workspace service, `opencode.json` and
 *   `AGENTS.md` are provisioned (ACADEMY-5)
 * @param openCodeModel learner agent model rendered into `opencode.json`
 * @param openCodeProviderUrl OpenAI-compatible provider URL rendered into `opencode.json`
 * @param openCodeProvider N0 provider kind the agent uses (ACADEMY-7)
 * @param openCodeApiKeyEnvVar name of the env var holding the key, blank means
 *   "documented per-provider default" (ACADEMY-7; never a value)
 */
data class InstallerPlatform(
    val os: TargetOs,
    val applicationName: String,
    val applicationVersion: String,
    val javaVersion: String,
    val gradleVersion: String,
    val composeEnabled: Boolean,
    val credentialsEnvPrefix: String,
    val openCodeEnabled: Boolean = true,
    val openCodeModel: String = "gpt-oss:120b-cloud",
    val openCodeProviderUrl: String = "http://ollama:11434/v1",
    val openCodeProvider: LlmProviderKind = LlmProviderKind.OLLAMA_LOCAL,
    val openCodeApiKeyEnvVar: String? = null,
) {
    init {
        require(applicationName.isNotBlank()) { "applicationName must not be blank" }
        require(applicationVersion.isNotBlank()) { "applicationVersion must not be blank" }
        require(javaVersion.matches(Regex("\\d+"))) { "javaVersion must be numeric major, got: $javaVersion" }
        require(gradleVersion.isNotBlank()) { "gradleVersion must not be blank" }
        require(credentialsEnvPrefix.isNotBlank()) { "credentialsEnvPrefix must not be blank" }
        require(openCodeModel.isNotBlank()) { "openCodeModel must not be blank" }
        require(openCodeProviderUrl.isNotBlank()) { "openCodeProviderUrl must not be blank" }
        require(
            openCodeProvider != LlmProviderKind.CUSTOM || !openCodeApiKeyEnvVar.isNullOrBlank(),
        ) {
            "CUSTOM provider requires an openCodeApiKeyEnvVar name (never a value)"
        }
    }

    /** Resolved opencode exposure configuration (ACADEMY-5, BYOK ACADEMY-7). */
    fun openCodeConfig(): OpenCodeConfig = OpenCodeConfig(
        providerUrl = openCodeProviderUrl,
        model = openCodeModel,
        provider = openCodeProvider,
        apiKeyEnvVar = openCodeApiKeyEnvVar?.takeIf { it.isNotBlank() },
    )
}

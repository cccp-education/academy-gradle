package education.cccp.academy.installer

import contracts.runtime.LlmProviderKind
import education.cccp.academy.material.MaterialRequirement
import education.cccp.academy.moodle.MoodleImportPlan
import education.cccp.academy.opencode.BridgeGuide
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
 * @param bridgeEnabled whether the local webhook bridge is documented (ACADEMY-8)
 * @param bridgeHost bridge bind address documented in the learner guide
 * @param bridgePort bridge bind port documented in the learner guide
 * @param material declared training material requirement (ACADEMY-4), or null
 *   when the project consumes no versioned material — the material guide is
 *   written only when declared (D-ACADEMY-4-10)
 * @param moodleMaterialEnabled whether the compose scaffold declares the
 *   one-shot `moodle-material` service that applies the generated plan after
 *   Moodle created its schema (ACADEMY-11-4, D-ACADEMY-11-5). Defaults to
 *   `false`: without injection the scaffold is byte-identical (D-ACADEMY-11-6)
 * @param moodlePlan the material import plan to stage inside the installer, or
 *   `null` when no material was read — the installer then stages a degraded,
 *   empty plan that injects nothing (ACADEMY-11-4)
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
    val bridgeEnabled: Boolean = false,
    val bridgeHost: String = "127.0.0.1",
    val bridgePort: Int = 8765,
    val material: MaterialRequirement? = null,
    val moodleMaterialEnabled: Boolean = false,
    val moodlePlan: MoodleImportPlan? = null,
    val offlineBundleEnabled: Boolean = false,
    /**
     * ACADEMY-16 / D-LWA-5 — whether the compose scaffold embeds the local
     * Ollama runtime. Defaults to `true` (backward-compatible ACADEMY-6
     * behaviour); the `CLOUD_BYOK` workstation profile disables it so a learner
     * machine does not need the heavy local runtime.
     */
    val ollamaEnabled: Boolean = true,
    val imageDigests: Map<String, String> = emptyMap(),
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
        require(bridgePort in 1..65535) { "bridgePort must be in 1..65535, got: $bridgePort" }
    }

    /**
     * ACADEMY-14 — pins a compose image reference to its resolved digest.
     *
     * The compose scaffold historically writes `ollama/ollama` and
     * `portainer/portainer-ce` without a tag; the offline manifest records them
     * as `:latest`. Both keys are accepted so a pinned digest is never lost to a
     * tag spelling. Without a digest the reference is returned unchanged, which
     * keeps the generated scaffold byte-identical (backward compatibility).
     */
    fun image(reference: String): String {
        val digest = imageDigests[reference] ?: imageDigests["$reference:latest"]
        return if (digest.isNullOrBlank()) reference else "$reference@$digest"
    }

    /** Resolved opencode exposure configuration (ACADEMY-5, BYOK ACADEMY-7, bridge ACADEMY-8). */
    fun openCodeConfig(): OpenCodeConfig =
        OpenCodeConfig(
            providerUrl = openCodeProviderUrl,
            model = openCodeModel,
            provider = openCodeProvider,
            apiKeyEnvVar = openCodeApiKeyEnvVar?.takeIf { it.isNotBlank() },
            bridge = if (bridgeEnabled) BridgeGuide(host = bridgeHost, port = bridgePort) else null,
            localRuntimeEnabled = ollamaEnabled,
        )
}

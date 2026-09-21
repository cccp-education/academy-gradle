package education.cccp.academy

import contracts.runtime.LlmProviderKind
import education.cccp.academy.installer.TargetOs
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Typed `academyInstaller { }` extension — the entry point of the installer
 * generator socle (ACADEMY-1-0).
 *
 * Pattern: abstract class + lazy [Property]/[ListProperty]/[DirectoryProperty]
 * managed by Gradle (pattern planner/codebase/document). All defaults are
 * wired in the plugin registration via `convention(...)` so a consuming
 * project gets sane values out of the box and can override them lazily.
 *
 * ```
 * academyInstaller {
 *     applicationName.set("my-academy")
 *     javaVersion.set("25")
 *     gradleVersion.set("9.7.1")
 *     targets.set(listOf(TargetOs.LINUX))
 *     outputDir.set(layout.buildDirectory.dir("installers"))
 *     composeEnabled.set(false)
 *     credentialsEnvPrefix.set("MY_ACADEMY_")
 * }
 * ```
 *
 * Credentials are deliberately NOT modelled here: secrets flow through
 * environment variables only ([credentialsEnvPrefix] documents the naming
 * convention the generated scripts read, never a value).
 */
abstract class AcademyInstallerExtension {

    /** Resource name — defaults to `academy`. */
    abstract val applicationName: Property<String>

    /** Version of the distributed resource — defaults to the project version. */
    abstract val applicationVersion: Property<String>

    /** Java Temurin major version provisioned on the host (default 25). */
    abstract val javaVersion: Property<String>

    /** Gradle version provisioned on the host (default 9.7.1). */
    abstract val gradleVersion: Property<String>

    /** Target platforms — defaults to all three (LINUX, WINDOWS, MACOS). */
    abstract val targets: ListProperty<TargetOs>

    /** Output directory for the generated installers (default `build/academy/installers`). */
    abstract val outputDir: DirectoryProperty

    /**
     * Whether the generated script embeds the `docker-compose.yml` scaffold
     * (Moodle + PostgreSQL + Ollama + Portainer + opencode workspace) written
     * at install time.
     */
    abstract val composeEnabled: Property<Boolean>

    /** Environment variable prefix the generated scripts read credentials from. */
    abstract val credentialsEnvPrefix: Property<String>

    /**
     * Whether the scaffold provisions the learner workspace service — the
     * generated workspace image (`Dockerfile` on the pinned gradle base), the
     * `opencode.json` configuration and the `AGENTS.md` learner guide
     * (ACADEMY-5). Defaults to `true`.
     */
    abstract val openCodeEnabled: Property<Boolean>

    /** Model the learner agent uses by default (ACADEMY-5). */
    abstract val openCodeModel: Property<String>

    /** OpenAI-compatible provider base URL the agent points to (ACADEMY-5). */
    abstract val openCodeProviderUrl: Property<String>

    /**
     * LLM provider the agent uses (ACADEMY-7) — the N0 contract kind, default
     * `OLLAMA_LOCAL` (the embedded runtime, identical to ACADEMY-5).
     */
    abstract val openCodeProvider: Property<LlmProviderKind>

    /**
     * Name of the environment variable holding the provider key (ACADEMY-7) —
     * **never the value** (Secrets rule). Empty means "use the documented
     * per-provider default" from `ByokProviderCatalog`.
     */
    abstract val openCodeApiKeyEnvVar: Property<String>
}
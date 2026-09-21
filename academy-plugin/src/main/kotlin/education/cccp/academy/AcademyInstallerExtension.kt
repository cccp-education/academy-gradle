package education.cccp.academy

import contracts.runtime.LlmProviderKind
import education.cccp.academy.installer.TargetOs
import education.cccp.academy.material.MaterialArtifactType
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

    /**
     * Whether the local webhook bridge (ACADEMY-8) is started by the
     * `serveWebhookBridge` task. Defaults to `false`: a build plugin never opens
     * a port without an explicit opt-in (D-ACADEMY-8-7).
     */
    abstract val bridgeEnabled: Property<Boolean>

    /** Bind address of the local bridge — defaults to loopback. */
    abstract val bridgeHost: Property<String>

    /** Bind port of the local bridge (default 8765). */
    abstract val bridgePort: Property<Int>

    /**
     * Remote of the versioned training material the learner consumes
     * (ACADEMY-4) — the academy reading of the N0 `MaterialUpdateContract`.
     * Empty (the default) means **no requirement declared**: the resource is
     * generated before the learner pulls, so a build never fails on absent
     * material (D-ACADEMY-4-9).
     */
    abstract val materialRemoteUrl: Property<String>

    /**
     * Expected material version/tag (ACADEMY-4) — empty means the learner
     * accepts any version. Never a credential: the transport stays git-side.
     */
    abstract val materialCurrentVersion: Property<String>

    /**
     * Directory holding the pulled material and its EPIC K `metadata.json`
     * pivots (ACADEMY-4) — defaults to `material` in the consuming project.
     */
    abstract val materialDir: DirectoryProperty

    /**
     * Artifact types the requirement expects (ACADEMY-4) — the activated
     * deliverable vocabulary (SPG, SPD, SLIDES, DOCUMENT, CAPSULE, QUIZ).
     */
    abstract val materialExpectedTypes: ListProperty<MaterialArtifactType>

    /**
     * Whether the material present in [moodleMaterialDir] is injected into the
     * Moodle course (ACADEMY-11, D-ACADEMY-11-8). Defaults to `false`: the
     * installation is byte-identical unless the learner opts in, and an empty
     * material directory produces nothing (D-ACADEMY-11-6).
     */
    abstract val moodleImportEnabled: Property<Boolean>

    /**
     * Directory holding the pulled material to inject (ACADEMY-11) — defaults
     * to the same `material` directory the inspection reads (ACADEMY-4).
     */
    abstract val moodleMaterialDir: DirectoryProperty

    /**
     * Shortname of the Moodle course the material is injected into (ACADEMY-11).
     * It is the idempotency key: re-running resolves the existing course instead
     * of creating a duplicate. Defaults to the seeded course `academy-seed`.
     */
    abstract val moodleCourseShortName: Property<String>

    /** Human-readable full name of the target course (ACADEMY-11). */
    abstract val moodleCourseFullName: Property<String>
}
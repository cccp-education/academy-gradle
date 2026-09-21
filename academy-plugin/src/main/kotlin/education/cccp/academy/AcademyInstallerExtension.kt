package education.cccp.academy

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
}
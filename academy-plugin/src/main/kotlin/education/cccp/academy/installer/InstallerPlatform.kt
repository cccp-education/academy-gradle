package education.cccp.academy.installer

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
 */
data class InstallerPlatform(
    val os: TargetOs,
    val applicationName: String,
    val applicationVersion: String,
    val javaVersion: String,
    val gradleVersion: String,
    val composeEnabled: Boolean,
    val credentialsEnvPrefix: String,
) {
    init {
        require(applicationName.isNotBlank()) { "applicationName must not be blank" }
        require(applicationVersion.isNotBlank()) { "applicationVersion must not be blank" }
        require(javaVersion.matches(Regex("\\d+"))) { "javaVersion must be numeric major, got: $javaVersion" }
        require(gradleVersion.isNotBlank()) { "gradleVersion must not be blank" }
        require(credentialsEnvPrefix.isNotBlank()) { "credentialsEnvPrefix must not be blank" }
    }
}
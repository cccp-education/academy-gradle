package education.cccp.academy.installer.offline

/**
 * ACADEMY-14 — the process seam of the offline bundle builder.
 *
 * The Gradle task never calls `ProcessBuilder` directly: it runs commands
 * through this fun interface, so the orchestration is exercised with a fake
 * runner (no Docker, no machine) and the real adapter is the only I/O.
 */
fun interface DockerImageRunner {
    /** Runs one command and returns its exit code and captured output. */
    fun run(command: List<String>): DockerRunResult
}

/**
 * @property exitCode the process exit code, or `-1` when it could not run or
 *   timed out
 * @property output stdout and stderr (merged), empty when not captured
 */
data class DockerRunResult(
    val exitCode: Int,
    val output: String = "",
)

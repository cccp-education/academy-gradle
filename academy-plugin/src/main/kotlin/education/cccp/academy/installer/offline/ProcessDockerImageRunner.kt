package education.cccp.academy.installer.offline

import java.util.concurrent.TimeUnit

/**
 * ACADEMY-14 — the real Docker adapter.
 *
 * The only place that starts a process. Output is drained on a daemon thread
 * **while** the process runs: `docker pull` writes a lot of progress on stderr,
 * and a pipe that is not drained deadlocks the process before [waitFor].
 *
 * A missing Docker binary, a timeout and a non-zero exit are all reported as a
 * result (exit `-1` or the real code), never as a crash: the caller decides.
 */
class ProcessDockerImageRunner(
    private val timeoutSeconds: Long = 900,
) : DockerImageRunner {
    override fun run(command: List<String>): DockerRunResult =
        try {
            val process =
                ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
            val output = StringBuilder()
            val drain =
                Thread {
                    process.inputStream.bufferedReader().forEachLine { output.appendLine(it) }
                }.apply {
                    isDaemon = true
                    start()
                }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            drain.join(2_000)
            DockerRunResult(
                exitCode = if (finished) process.exitValue() else -1,
                output = output.toString(),
            )
        } catch (exception: Exception) {
            DockerRunResult(exitCode = -1, output = exception.message ?: "")
        }
}

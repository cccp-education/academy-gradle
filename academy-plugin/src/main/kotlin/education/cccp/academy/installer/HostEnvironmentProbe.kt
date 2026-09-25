package education.cccp.academy.installer

import education.cccp.academy.env.EnvironmentFacts
import education.cccp.academy.env.EnvironmentProbe
import education.cccp.academy.env.HostOs
import java.util.concurrent.TimeUnit

/**
 * The real host probe adapter (ACADEMY-12-2, D-ACADEMY-12-2) — the only place
 * that reads the machine. The domain stays pure: this adapter is injected into
 * [education.cccp.academy.CheckContainerEngineTask] and is never referenced by
 * [education.cccp.academy.env].
 *
 * Probes (facts, never decisions):
 *  - OS from `os.name`
 *  - engine reachability from a `docker info` exit code
 *  - WSL2 presence from a `wsl --status` exit code (Windows only)
 *  - admin rights from a `net session` exit code (Windows only)
 *
 * Every command runs with a timeout and swallows failures into the fact `false`:
 * a missing tool is a fact, not a crash — the decider turns facts into a verdict.
 */
class HostEnvironmentProbe(
    private val commandTimeoutSeconds: Long = 5,
) : EnvironmentProbe {

    override fun observe(): EnvironmentFacts {
        val os = HostOs.fromOsName(System.getProperty("os.name"))
        return EnvironmentFacts(
            os = os,
            dockerEngineReady = runSucceeds("docker", "info"),
            wsl2Present = os == HostOs.WINDOWS && runSucceeds("wsl", "--status"),
            admin = os == HostOs.WINDOWS && runSucceeds("net", "session"),
        )
    }

    private fun runSucceeds(vararg command: String): Boolean = try {
        val process = ProcessBuilder(*command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS) && process.exitValue() == 0
    } catch (_: Exception) {
        false
    }
}

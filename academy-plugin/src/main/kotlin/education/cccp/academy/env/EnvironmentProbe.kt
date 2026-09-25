package education.cccp.academy.env

/**
 * The host probe port (ACADEMY-12-2, D-ACADEMY-12-2) — the seam between the pure
 * [DockerStrategyDecider] and the machine.
 *
 * Probing reads the real host (OS, engine reachability, WSL2, admin rights) and
 * therefore involves I/O; it belongs to an adapter, never to the domain. A task
 * depends on this interface so its decision can be unit-tested with a fake probe,
 * without a machine and without executing a single command.
 */
fun interface EnvironmentProbe {

    /** Reads the observed facts of the host running the build. */
    fun observe(): EnvironmentFacts
}

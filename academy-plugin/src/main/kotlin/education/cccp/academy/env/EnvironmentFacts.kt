package education.cccp.academy.env

/**
 * Immutable, observed facts about the host (ACADEMY-12-1, D-ACADEMY-12-2) — the
 * input of the pure [DockerStrategyDecider].
 *
 * Built by the installer task from real probes at execution time (OS, admin
 * rights, WSL2 presence, engine reachability), so the decision stays free of
 * I/O and unit-testable in isolation. The probe itself belongs to the task
 * adapter, never to this domain.
 *
 * @param os detected host operating system
 * @param dockerEngineReady true when a Docker daemon is already reachable
 * @param wsl2Present true when WSL2 is available (Windows only, ignored elsewhere)
 * @param admin true when the installer runs with administrator rights
 */
data class EnvironmentFacts(
    val os: HostOs,
    val dockerEngineReady: Boolean,
    val wsl2Present: Boolean,
    val admin: Boolean,
)

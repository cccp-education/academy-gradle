package education.cccp.academy.env

/**
 * The pure environment decider (ACADEMY-12-1, D-ACADEMY-12-2) — a deterministic
 * function from the observed [EnvironmentFacts] to a [DockerStrategy] verdict.
 * No I/O, no Gradle, no command executed: the probe belongs to the task adapter.
 *
 * Rules, in order:
 *  - a reachable engine is always [DockerStrategy.AlreadyReady]
 *  - Linux                 -> [DockerStrategy.InstallDockerEngine]
 *  - macOS                 -> [DockerStrategy.InstallColima]
 *  - Windows + WSL2        -> [DockerStrategy.InstallDockerDesktop]
 *  - Windows, no WSL2, admin -> [DockerStrategy.BootstrapWsl2First]
 *  - otherwise             -> [DockerStrategy.Unsupported] (never a throw)
 */
object DockerStrategyDecider {

    /** Computes the provisioning strategy for [facts]. */
    fun decide(facts: EnvironmentFacts): DockerStrategy = when {
        facts.dockerEngineReady -> DockerStrategy.AlreadyReady(facts.os)

        facts.os == HostOs.LINUX -> DockerStrategy.InstallDockerEngine(facts.os)

        facts.os == HostOs.MACOS -> DockerStrategy.InstallColima(facts.os)

        facts.os == HostOs.WINDOWS -> when {
            facts.wsl2Present -> DockerStrategy.InstallDockerDesktop(facts.os)
            facts.admin -> DockerStrategy.BootstrapWsl2First(facts.os)
            else -> DockerStrategy.Unsupported(
                os = facts.os,
                reason = "WSL2 is required for Linux containers on Windows and enabling it " +
                    "needs administrator rights - re-run the installer as administrator",
            )
        }

        else -> DockerStrategy.Unsupported(
            os = facts.os,
            reason = "unsupported host operating system - install Docker manually then re-run",
        )
    }
}

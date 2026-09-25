package education.cccp.academy.env

/**
 * The provisioning verdict of [DockerStrategyDecider] (ACADEMY-12-1,
 * D-ACADEMY-12-3) — a sealed result instead of a thrown exception.
 *
 * The matrix is per-OS (D-ACADEMY-12-1): Linux installs the native Docker
 * Engine, Windows installs Docker Desktop (WSL2 required, managed by Desktop),
 * macOS installs Colima. Portainer is layered on every OS by the installer —
 * the strategy only decides how the *engine* is provisioned, never the GUI.
 *
 * An unprovisionable host is a legitimate verdict, never an error: [Unsupported]
 * carries the reason so the installer can explain it to the learner.
 */
sealed interface DockerStrategy {

    /** The detected host OS, useful to log the decision. */
    val os: HostOs

    /** A Docker daemon is already reachable — nothing to provision. */
    data class AlreadyReady(override val os: HostOs) : DockerStrategy

    /** Linux: install the native Docker Engine from the distribution repository. */
    data class InstallDockerEngine(override val os: HostOs) : DockerStrategy

    /** macOS: install Colima (MIT), the VM-backed Docker runtime. */
    data class InstallColima(override val os: HostOs) : DockerStrategy

    /**
     * Windows with WSL2 already present: install Docker Desktop.
     * WSL2 is never installed by us here — Desktop owns its own distro.
     */
    data class InstallDockerDesktop(override val os: HostOs) : DockerStrategy

    /**
     * Windows without WSL2, running with administrator rights: enable WSL2
     * first (a reboot may be required — the installer is resumable).
     */
    data class BootstrapWsl2First(override val os: HostOs) : DockerStrategy

    /**
     * The host cannot be provisioned automatically.
     *
     * @property reason human-readable explanation, never blank
     */
    data class Unsupported(override val os: HostOs, val reason: String) : DockerStrategy {
        init {
            require(reason.isNotBlank()) { "reason must not be blank" }
        }
    }
}

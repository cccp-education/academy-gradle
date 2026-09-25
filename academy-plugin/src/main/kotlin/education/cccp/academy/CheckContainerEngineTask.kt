package education.cccp.academy

import education.cccp.academy.env.DockerStrategy
import education.cccp.academy.env.DockerStrategyDecider
import education.cccp.academy.env.EnvironmentProbe
import education.cccp.academy.installer.HostEnvironmentProbe
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Reports the container engine provisioning strategy of the host running the
 * build (ACADEMY-12-2, D-ACADEMY-12-2/12-3).
 *
 * The task is the adapter: it observes the host through an injectable
 * [EnvironmentProbe] and delegates the decision to the pure
 * [DockerStrategyDecider]. It never provisions itself — provisioning happens at
 * install time via the generated `install.sh` / `install.bat` (D-ACADEMY-12-5);
 * the task's role is to *state* what the host needs.
 *
 * Per D-ACADEMY-12-3 an unprovisionable host is a **verdict, never a throw**:
 * the task always reports the strategy and never fails the build, so a bare host
 * (fresh Windows, CI without Docker) can still configure, build and test
 * (P0 S-014). The learner reads the reported strategy and runs the installer.
 */
@DisableCachingByDefault(because = "Probes the host, which lives outside the build cache (ACADEMY-12-2)")
abstract class CheckContainerEngineTask : DefaultTask() {

    /**
     * The host probe — injected. Defaults to the real [HostEnvironmentProbe]
     * adapter; tests override it with a fake to drive the decision without a
     * machine (D-ACADEMY-12-6).
     */
    @get:Internal
    abstract val environmentProbe: Property<EnvironmentProbe>

    init {
        group = "academy"
        description = "Reports the container engine provisioning strategy of the host (read-only)"
    }

    /** The pure verdict for the observed host. */
    fun strategy(): DockerStrategy = DockerStrategyDecider.decide(environmentProbe.get().observe())

    @TaskAction
    fun checkContainerEngine() {
        when (val strategy = strategy()) {
            is DockerStrategy.AlreadyReady -> logger.lifecycle(
                "[academy] a Docker engine is ready on ${strategy.os.name.lowercase()} - nothing to provision",
            )
            is DockerStrategy.Unsupported -> logger.lifecycle(
                "[academy] this host cannot be provisioned automatically - ${strategy.reason}",
            )
            else -> logger.lifecycle(
                "[academy] the generated installer will provision the engine on " +
                    "${strategy.os.name.lowercase()} (${strategy::class.simpleName})",
            )
        }
    }
}


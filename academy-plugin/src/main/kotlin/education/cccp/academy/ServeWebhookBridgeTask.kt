package education.cccp.academy

import education.cccp.academy.bridge.BridgeServerConfig
import education.cccp.academy.bridge.ByokBridgeSupport
import education.cccp.academy.bridge.startBridge
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Starts the local webhook bridge (ACADEMY-8-2) and blocks until the build is
 * interrupted — daemon mode, pattern codebase `SessionProtocolDaemonTask`.
 *
 * Opt-in by construction (D-ACADEMY-8-7): the task does nothing unless
 * [bridgeEnabled] is `true`, so a build never opens a port by accident. It is
 * **local only** (`AGENT.adoc`: no VPS, single-user build artefact).
 */
@DisableCachingByDefault(because = "Interactive daemon — binds a port and blocks, non-cacheable (ACADEMY-8-2)")
abstract class ServeWebhookBridgeTask : DefaultTask() {

    /** Opt-in switch — mirrors `academyInstaller.bridgeEnabled`. */
    @get:Input
    abstract val bridgeEnabled: Property<Boolean>

    /** Bind address, defaulting to loopback. */
    @get:Input
    abstract val bridgeHost: Property<String>

    /** Bind port. */
    @get:Input
    abstract val bridgePort: Property<Int>

    /** Resolved provider id reported by the health probe (descriptive only). */
    @get:Input
    abstract val providerId: Property<String>

    /** Resolved model reported by the health probe (descriptive only). */
    @get:Input
    abstract val model: Property<String>

    /** Test seam: overrides the blocking server launcher (mirror codex `storeOverride`). */
    @get:Internal
    abstract val serverLauncher: Property<(BridgeServerConfig) -> Unit>

    init {
        group = "academy"
        description = "Serves the local webhook bridge (Ktor) until interrupted — opt-in, local only"
        // Guard: never open a port without an explicit opt-in.
        onlyIf { bridgeEnabled.get() }
    }

    @TaskAction
    fun serve() {
        val config = config()
        logger.lifecycle("[academy] bridge listening on http://${config.host}:${config.port}")
        logger.lifecycle("[academy] routes: ${ByokBridgeSupport.describeRoutes()}")
        logger.lifecycle("[academy] press Ctrl-C to stop the bridge")
        if (serverLauncher.isPresent) serverLauncher.get().invoke(config) else startBridge(config)
    }

    /** The resolved, pure bridge configuration (testable without a server). */
    fun config(): BridgeServerConfig = BridgeServerConfig(
        host = bridgeHost.get(),
        port = bridgePort.get(),
        providerId = providerId.get(),
        model = model.get(),
    )
}

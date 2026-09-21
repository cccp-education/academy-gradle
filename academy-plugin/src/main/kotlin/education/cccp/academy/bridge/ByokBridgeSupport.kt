package education.cccp.academy.bridge

import education.cccp.academy.byok.ByokProviderCatalog
import contracts.runtime.LlmProviderKind

/**
 * Small pure adapter between the BYOK domain (ACADEMY-7) and the bridge
 * (ACADEMY-8-1): resolves the descriptive provider id the health probe reports,
 * and renders the documented route list for the task log.
 *
 * No credential ever crosses here — only the provider **id** (never its key).
 */
object ByokBridgeSupport {

    /** The opencode provider id for [kind] (e.g. `ollama`, `google`). */
    fun providerIdFor(kind: LlmProviderKind): String = ByokProviderCatalog.specFor(kind).id

    /** The route list as a single log line, read from [BridgeRoutes] (single source). */
    fun describeRoutes(): String =
        BridgeRoutes.routes.joinToString(", ") { "${it.method} ${it.path}" }
}

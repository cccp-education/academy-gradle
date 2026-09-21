package education.cccp.academy.bridge

import contracts.session.SessionPrompt
import kotlinx.serialization.Serializable

/**
 * The bridge's HTTP wire schemas (ACADEMY-8-1) — an **anti-corruption layer**
 * between the network and the N0 contracts.
 *
 * The N0 contracts (`contracts.session.*`) are deliberately pure data classes
 * with **no serialization dependency** (found in S-011: they carry no
 * `@Serializable`, codebase serializes them with Jackson). The bridge therefore
 * does not annotate them — it maps to its own `@Serializable` view at the edge,
 * keeping the shared contract untouched for every other borough.
 */
@Serializable
data class SessionRequest(
    val prompt: String,
    val maxActions: Int = 10,
    val model: String? = null,
)

/** Inbound [SessionRequest] mapped to the N0 [SessionPrompt]. */
fun SessionRequest.toSessionPrompt(): SessionPrompt = SessionPrompt(
    prompt = prompt,
    maxActions = maxActions,
    model = model,
)

/** The bridge's acknowledgement of a routed prompt — never an LLM answer. */
@Serializable
data class BridgeAck(
    val sessionId: String,
    val output: String,
    val status: String,
    val eventName: String? = null,
)

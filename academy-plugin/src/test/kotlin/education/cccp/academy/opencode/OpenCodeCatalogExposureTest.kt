package education.cccp.academy.opencode

import contracts.runtime.ToolExposure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * ACADEMY-11 (ex-TODO S-012) — the learner guardrail is a **contract**, not a
 * comment.
 *
 * Until now `OpenCodeCatalog` documented the rule "only `foundry/public` is
 * exposed" in a KDoc, and [LearnerGuideGenerator] restated it as prose: the
 * same rule lived in three places and could drift. The N0 `ToolExposure`
 * contract (`runtime-contracts:0.0.1`, on the classpath since ACADEMY-7) exists
 * precisely to formalize it — this test locks the catalog as its single source
 * and the guide as its renderer.
 *
 * Baby-step TDD: this file drove the addition of
 * [OpenCodeCatalog.PUBLIC_BOROUGHS_ROOT] / [OpenCodeCatalog.exposure] and the
 * guide refactor. RED proved by `Unresolved reference 'exposure'`.
 */
class OpenCodeCatalogExposureTest {

    @Test
    fun `the catalog exposes the N0 guardrail contract`() {
        val exposure: ToolExposure = OpenCodeCatalog.exposure()

        assertEquals("foundry/public", exposure.publicBoroughsRoot)
        assertEquals(ToolExposure(publicBoroughsRoot = "foundry/public").rule, exposure.rule)
    }

    @Test
    fun `the guardrail root is the physical public boundary`() {
        assertEquals("foundry/public", OpenCodeCatalog.PUBLIC_BOROUGHS_ROOT)
        assertEquals(OpenCodeCatalog.PUBLIC_BOROUGHS_ROOT, OpenCodeCatalog.exposure().publicBoroughsRoot)
    }

    @Test
    fun `the exposure root is overridable for a caller that relocates the workspace`() {
        assertEquals(
            "opt/cccp/public",
            OpenCodeCatalog.exposure(publicBoroughsRoot = "opt/cccp/public").publicBoroughsRoot,
        )
    }

    @Test
    fun `the N0 contract rejects a blank public root`() {
        assertFailsWith<IllegalArgumentException> {
            OpenCodeCatalog.exposure(publicBoroughsRoot = "  ")
        }
    }
}

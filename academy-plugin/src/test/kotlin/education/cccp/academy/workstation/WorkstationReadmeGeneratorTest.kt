package education.cccp.academy.workstation

import education.cccp.academy.opencode.OpenCodeCatalog
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * ACADEMY-16 US-3 — the learner README states the module, the profile, the
 * certification workflow and the declared limits, without ever embedding a
 * secret (D-LWA-7).
 */
class WorkstationReadmeGeneratorTest {
    private fun readme(profile: WorkstationProfile = WorkstationProfile.CLOUD_BYOK) =
        WorkstationReadmeGenerator.render(
            applicationName = "academy-pilot",
            moduleId = "fpa-pilot",
            moduleVersion = "0.0.1",
            profile = profile,
            plugins = OpenCodeCatalog.plugins,
        )

    @Test
    fun `states the module and the cloud profile`() {
        val readme = readme()

        assertContains(readme, "fpa-pilot")
        assertContains(readme, "Cloud BYOK")
        assertContains(readme, "Docker")
    }

    @Test
    fun `documents the certification workflow and the tools`() {
        val readme = readme()

        assertContains(readme, "Parcours de certification")
        assertContains(readme, "cccp_slider")
        assertContains(readme, "cccp_document")
        assertContains(readme, "votre")
    }

    @Test
    fun `states the limits and never embeds a secret`() {
        val readme = readme()

        assertContains(readme, "mono-utilisateur")
        assertContains(readme, "jamais")
        assertFalse(readme.contains("API_KEY="), "the README must never embed a key assignment")
    }
}

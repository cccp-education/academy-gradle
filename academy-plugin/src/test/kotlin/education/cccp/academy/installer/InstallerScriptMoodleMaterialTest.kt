package education.cccp.academy.installer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-11-4 — the one-shot `moodle-material` compose service (D-ACADEMY-11-5).
 *
 * The service **applies** the generated material plan after Moodle created its
 * schema, on the exact `moodle-seed` pattern (ACADEMY-6-5): `restart: "no"`,
 * explicit `entrypoint`, `depends_on` the Moodle health, and a read-only mount
 * of the pulled material. It shares the Moodle code tree through the named
 * `moodle-html` volume — `moosh` bootstraps against `/var/www/html/config.php`,
 * so a sibling container without that volume could never reach the install
 * (verified on the real image, S-016).
 *
 * Baby-step TDD: this file drives the `moodleMaterialEnabled` platform flag and
 * the conditional compose wiring. RED proved by `Unresolved reference`.
 *
 * Degraded by default (D-ACADEMY-11-6): with no injection enabled, the compose
 * scaffold is **byte-identical** — no service, no shared volume.
 */
class InstallerScriptMoodleMaterialTest {

    private fun platform(
        os: TargetOs = TargetOs.LINUX,
        moodleMaterialEnabled: Boolean = false,
    ) = InstallerPlatform(
        os = os,
        applicationName = "academy",
        applicationVersion = "0.0.1",
        javaVersion = "25",
        gradleVersion = "9.7.1",
        composeEnabled = true,
        credentialsEnvPrefix = "ACADEMY_",
        moodleMaterialEnabled = moodleMaterialEnabled,
    )

    @Test
    fun `no injection keeps the compose scaffold free of any material service - degraded by default`() {
        val script = InstallerScriptGenerator.render(platform(moodleMaterialEnabled = false)).single().content

        assertFalse(script.contains("moodle-material"), "no injection means no service (D-ACADEMY-11-6)")
        assertFalse(script.contains("moodle-html"), "no injection means no shared volume (D-ACADEMY-11-6)")
        assertFalse(script.contains("\$APP_DIR/moodle"), "no injection means no staging directory (D-ACADEMY-11-6)")
    }

    @Test
    fun `linux injection declares the one-shot material service sharing the moodle tree`() {
        val script = InstallerScriptGenerator.render(platform(moodleMaterialEnabled = true)).single().content

        assertTrue(script.contains("moodle-material"), "the one-shot material service must be declared (D-ACADEMY-11-5)")
        assertTrue(script.contains("restart: \"no\""), "the material service must be one-shot")
        assertTrue(
            script.contains("entrypoint: [\"sh\", \"/ingest/entrypoint.sh\"]"),
            "the material service must run the generic staged entrypoint",
        )
        assertTrue(
            script.contains("erseco/alpine-moodle:v5.2.3"),
            "the applicator runs in the image that ships moosh (S-013)",
        )
        assertTrue(script.contains("./moodle:/ingest"), "the staged plan must be mounted into the applicator")
        assertTrue(script.contains("./material:/material:ro"), "the pulled material must be mounted read-only")
        assertTrue(script.contains("MATERIAL_DIR"), "the applicator must read the material directory")
        assertTrue(
            script.contains("moodle-html:/var/www/html"),
            "the moodle service must mount the shared tree moosh bootstraps against",
        )
        assertTrue(script.contains("  moodle-html:"), "the shared tree must be a declared named volume")
    }

    @Test
    fun `the material service waits for the moodle health before injecting`() {
        val script = InstallerScriptGenerator.render(platform(moodleMaterialEnabled = true)).single().content

        assertTrue(
            script.contains("condition: service_healthy") && script.contains("moodle:"),
            "the applicator must start only once Moodle is healthy: $script",
        )
    }

    @Test
    fun `the installer writes the generic entrypoint and the staging directories`() {
        val script = InstallerScriptGenerator.render(platform(moodleMaterialEnabled = true)).single().content

        assertTrue(script.contains("mkdir -p \"\$APP_DIR/moodle\""), "the ingest staging directory must be created")
        assertTrue(script.contains("mkdir -p \"\$APP_DIR/material\""), "the material mount point must be created")
        assertTrue(script.contains("\$APP_DIR/moodle/entrypoint.sh"), "the generic entrypoint must be written")
        assertTrue(script.contains("moosh course-list"), "the entrypoint must wait for the moosh bootstrap")
        assertTrue(script.contains("/ingest/ingest.sh"), "the entrypoint must delegate to the staged plan")
    }

    @Test
    fun `windows injection mirrors the material service and the entrypoint - structural parity`() {
        val windows = InstallerScriptGenerator.render(
            platform(os = TargetOs.WINDOWS, moodleMaterialEnabled = true),
        ).single().content

        assertTrue(windows.contains("moodle-material"), "windows must declare the one-shot material service (parity)")
        assertTrue(windows.contains("restart: \"no\""), "windows material service must be one-shot (parity)")
        assertTrue(windows.contains("moodle-html"), "windows must share the Moodle tree (parity)")
        assertTrue(windows.contains("moodle\\entrypoint.sh"), "windows must write the entrypoint (parity)")
        assertTrue(windows.contains("MATERIAL_DIR"), "windows applicator must read the material directory (parity)")
    }
}

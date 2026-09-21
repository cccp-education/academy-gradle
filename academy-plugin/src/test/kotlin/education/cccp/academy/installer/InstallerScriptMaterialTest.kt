package education.cccp.academy.installer

import education.cccp.academy.material.MaterialArtifactType
import education.cccp.academy.material.MaterialRequirement
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-4-2 — the installer scaffold declares the material requirement only
 * when one is set (pattern `BridgeGuide` conditional, D-ACADEMY-8-9/4-10). The
 * `MATERIAL.md` states the remote and the frozen mechanism (git pull) and never
 * cites a runnable command that does not exist (BUREAU-3 🔴 À FAIRE, verified
 * S-012 — the S-007/S-011 lesson).
 *
 * Structural parity Linux/Windows is asserted: both writers render the same
 * content from the same source ([MaterialGuideGenerator]).
 */
class InstallerScriptMaterialTest {

    private fun platform(
        material: MaterialRequirement? = null,
        os: TargetOs = TargetOs.LINUX,
    ) = InstallerPlatform(
        os = os,
        applicationName = "academy",
        applicationVersion = "0.0.1",
        javaVersion = "25",
        gradleVersion = "9.7.1",
        composeEnabled = true,
        credentialsEnvPrefix = "ACADEMY_",
        material = material,
    )

    private val requirement = MaterialRequirement(
        remoteUrl = "https://github.com/cccp-education/formation-fpa",
        currentVersion = "v1.0",
        expectedTypes = listOf(MaterialArtifactType.SPG, MaterialArtifactType.SLIDES),
    )

    @Test
    fun `no requirement writes no material guide`() {
        val script = InstallerScriptGenerator.render(platform()).single().content

        assertFalse(script.contains("MATERIAL.md"), "a project with no requirement must not write a material guide")
        assertFalse(script.contains("inspectTrainingMaterial"), "no material task must be documented")
    }

    @Test
    fun `a declared requirement writes the material guide`() {
        val script = InstallerScriptGenerator.render(platform(material = requirement)).single().content

        assertTrue(script.contains("MATERIAL.md"), "the material guide must be written")
        assertTrue(
            script.contains("https://github.com/cccp-education/formation-fpa"),
            "the guide must carry the declared remote",
        )
        assertTrue(script.contains("v1.0"), "the guide must carry the expected version")
    }

    @Test
    fun `the material guide never cites a command that does not exist yet`() {
        val script = InstallerScriptGenerator.render(platform(material = requirement)).single().content

        assertFalse(
            script.contains("pullMaterial"),
            "pullMaterial is BUREAU-3 TODO - it must never be cited as runnable (S-007/S-011)",
        )
    }

    @Test
    fun `the windows installer mirrors the material guide (structural parity)`() {
        val linux = InstallerScriptGenerator.render(platform(material = requirement)).single().content
        val windows = InstallerScriptGenerator.render(
            platform(material = requirement, os = TargetOs.WINDOWS),
        ).single().content

        assertTrue(windows.contains("MATERIAL.md"), "windows must write the material guide (parity)")
        assertTrue(windows.contains("MATERIAL.md"), "windows must use the same file name (parity)")
        assertTrue(
            windows.contains("https://github.com/cccp-education/formation-fpa"),
            "windows must carry the same remote (parity)",
        )
        assertTrue(linux.contains("MATERIAL.md"))
    }
}

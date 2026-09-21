package education.cccp.academy.material

/**
 * Pure renderer of `MATERIAL.md` (ACADEMY-4-1/4-2) — the learner-facing
 * declaration of the training material requirement.
 *
 * It states **what** the learner will receive (remote, version, expected
 * artifact types) and **how** the material is delivered: the frozen mechanism
 * is *git pull of a tagged version* (vision § 3.2 / figeage S-011) — the learner
 * pulls, the creator never pushes.
 *
 * It deliberately cites **no runnable command**: `bureau-gradle pullMaterial` is
 * still `BUREAU-3` 🔴 À FAIRE and does not exist (verified S-012), so naming it
 * would reproduce the S-007 (`moodle:4.5`) / S-011 ("verify the source before
 * citing a precedent") bug class. The guide documents the mechanism, never an
 * absent binary.
 */
object MaterialGuideGenerator {

    /** Renders the material guide for [requirement]. */
    fun render(requirement: MaterialRequirement): String = buildString {
        appendLine("# Academy Training Material")
        appendLine()
        appendLine("This workspace consumes versioned training material produced by the")
        appendLine("CCCP education pipeline. The material is **pulled**, never pushed:")
        appendLine("the creator publishes tagged versions, you fetch the one you want.")
        appendLine()
        appendLine("## Declared requirement")
        appendLine()
        appendLine("- **Remote** — ${requirement.remoteUrl}")
        if (requirement.currentVersion != null) {
            appendLine("- **Expected version** — ${requirement.currentVersion}")
        } else {
            appendLine("- **Expected version** — any (no version pinned)")
        }
        appendLine("- **Expected artifacts** — ${renderTypes(requirement)}")
        appendLine()
        appendLine("## How the material arrives")
        appendLine()
        appendLine("The delivery mechanism is fixed: **git pull** of the tagged version")
        appendLine("from the declared remote into your office. You are the one who pulls —")
        appendLine("the creator never writes into your workspace.")
        appendLine()
        appendLine("```sh")
        appendLine("git fetch <remote>")
        appendLine("git checkout ${requirement.currentVersion ?: "<tag>"}")
        appendLine("```")
        appendLine()
        appendLine("## Verify what you received")
        appendLine()
        appendLine("Every artifact ships an EPIC K `metadata.json` pivot next to its")
        appendLine("AsciiDoc output. Run the verification task to see whether the pulled")
        appendLine("material satisfies this requirement:")
        appendLine()
        appendLine("```sh")
        appendLine("./gradlew inspectTrainingMaterial")
        appendLine("```")
    }.trimEnd('\n') + "\n"

    private fun renderTypes(requirement: MaterialRequirement): String =
        if (requirement.expectedTypes.isEmpty()) {
            "any (presence is enough)"
        } else {
            requirement.expectedTypes.joinToString(", ") { it.pivotType }
        }
}

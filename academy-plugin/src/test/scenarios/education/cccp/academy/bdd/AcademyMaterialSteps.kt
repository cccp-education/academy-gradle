package education.cccp.academy.bdd

import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import education.cccp.academy.material.MaterialArtifactType
import education.cccp.academy.material.MaterialGuideGenerator
import education.cccp.academy.material.MaterialInspector
import education.cccp.academy.material.MaterialManifest
import education.cccp.academy.material.MaterialReadiness
import education.cccp.academy.material.MaterialRequirement

/**
 * Steps for `academy_material.feature` (ACADEMY-4-3) — pattern S-088
 * (feature-scoped glue). Drives the pure material domain directly: zero Gradle
 * task invocation, zero I/O, zero git (D-ACADEMY-4-3/4-11).
 *
 * Every step is prefixed with the `material` vocabulary: the glue path is shared
 * with the installer/opencode/byok/bridge steps, so unprefixed phrases such as
 * `the guide contains {string}` would collide (lesson S-088). Cucumber
 * re-instantiates the glue per scenario, so instance fields reset naturally.
 */
class AcademyMaterialSteps : En {

    private var remote: String = ""
    private var version: String? = null
    private var expected: List<MaterialArtifactType> = emptyList()
    private var manifests: MutableList<MaterialManifest> = mutableListOf()
    private var guide: String = ""
    private var readiness: MaterialReadiness? = null

    init {
        Given("a material requirement for remote {string} at version {string}") { url: String, tag: String ->
            remote = url
            version = tag
        }
        And("the requirement expects the artifacts {string}") { list: String ->
            expected = list.split(",").mapNotNull { MaterialArtifactType.fromPivotType(it) }
        }
        And("no training material is present") {
            manifests = mutableListOf()
        }
        And("a pivot manifest of type {string} at version {string}") { type: String, tag: String ->
            manifests += pivot(type = type, version = tag)
        }
        And("a pivot manifest of unknown type {string} at version {string}") { type: String, tag: String ->
            manifests += pivot(type = type, version = tag)
        }
        And("a pivot manifest of type {string} at version {string} carrying an unknown field") { type: String, tag: String ->
            manifests += MaterialManifest.fromJson(
                """{ "source": "training", "type": "$type", "version": "$tag", "futureField": true }""",
            ) ?: error("the tolerant reader must parse a pivot with an unknown field")
        }

        When("the material requirement is read") {
            // The requirement is built on demand by the assertions below.
        }
        When("the material requirement is inspected") {
            readiness = MaterialInspector.inspect(requirement(), manifests)
        }
        When("the material guide is rendered") {
            guide = MaterialGuideGenerator.render(requirement())
        }

        Then("the requirement declares the remote {string}") { url: String ->
            assertThat(requirement().remoteUrl).isEqualTo(url)
        }
        And("the requirement declares the version {string}") { tag: String ->
            assertThat(requirement().currentVersion).isEqualTo(tag)
        }
        And("the requirement declares the artifact {string}") { type: String ->
            assertThat(requirement().expectedTypes).contains(MaterialArtifactType.valueOf(type))
        }
        And("the material verdict is {string}") { verdict: String ->
            val actual = when (checkNotNull(readiness)) {
                is MaterialReadiness.Ready -> "READY"
                is MaterialReadiness.Incomplete -> "INCOMPLETE"
                is MaterialReadiness.Empty -> "EMPTY"
            }
            assertThat(actual).isEqualTo(verdict)
        }
        And("the material report names the missing artifact {string}") { type: String ->
            val incomplete = checkNotNull(readiness)
            assertThat(incomplete).isInstanceOf(MaterialReadiness.Incomplete::class.java)
            assertThat((incomplete as MaterialReadiness.Incomplete).missingTypes)
                .contains(MaterialArtifactType.valueOf(type))
        }
        And("the material report marks the material outdated") {
            val incomplete = checkNotNull(readiness)
            assertThat(incomplete).isInstanceOf(MaterialReadiness.Incomplete::class.java)
            assertThat((incomplete as MaterialReadiness.Incomplete).outdated).isTrue()
        }
        And("the material guide contains {string}") { fragment: String ->
            assertThat(guide).contains(fragment)
        }
        And("the material guide never cites {string}") { fragment: String ->
            assertThat(guide).doesNotContain(fragment)
        }
    }

    private fun requirement() = MaterialRequirement(
        remoteUrl = remote.ifBlank { "https://example.org/training.git" },
        currentVersion = version,
        expectedTypes = expected,
    )

    private fun pivot(type: String, version: String): MaterialManifest =
        MaterialManifest.fromJson("""{ "source": "training", "type": "$type", "version": "$version" }""")
            ?: error("the reader must parse a well-formed pivot")
}

package education.cccp.academy.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * ACADEMY-4-1 — the pure inspector: a function of the declared requirement and
 * the read manifests to a verdict (D-ACADEMY-4-3/4-7). It never throws on absent
 * material — the resource is generated *before* the learner pulls, so absence is
 * a legitimate `Empty`, not an error.
 */
class MaterialInspectorTest {

    private val spg = manifest(MaterialArtifactType.SPG, version = "1.0")
    private val slides = manifest(MaterialArtifactType.SLIDES, version = "1.0")

    @Test
    fun `no manifest is an empty report, never a failure`() {
        val readiness = MaterialInspector.inspect(requirement(expected = listOf(MaterialArtifactType.SPG)), emptyList())

        assertIs<MaterialReadiness.Empty>(readiness)
    }

    @Test
    fun `every expected type present and version matching is ready`() {
        val readiness = MaterialInspector.inspect(
            requirement(expected = listOf(MaterialArtifactType.SPG, MaterialArtifactType.SLIDES)),
            listOf(spg, slides),
        )

        val ready = assertIs<MaterialReadiness.Ready>(readiness)
        assertEquals(
            listOf(MaterialArtifactType.SPG, MaterialArtifactType.SLIDES),
            ready.presentTypes,
        )
    }

    @Test
    fun `a missing expected type is incomplete`() {
        val readiness = MaterialInspector.inspect(
            requirement(expected = listOf(MaterialArtifactType.SPG, MaterialArtifactType.QUIZ)),
            listOf(spg),
        )

        val incomplete = assertIs<MaterialReadiness.Incomplete>(readiness)
        assertEquals(listOf(MaterialArtifactType.QUIZ), incomplete.missingTypes)
    }

    @Test
    fun `a version divergence is incomplete and reported as outdated`() {
        val readiness = MaterialInspector.inspect(
            requirement(expected = listOf(MaterialArtifactType.SPG), version = "2.0"),
            listOf(manifest(MaterialArtifactType.SPG, version = "1.0")),
        )

        val incomplete = assertIs<MaterialReadiness.Incomplete>(readiness)
        assertTrue(incomplete.outdated, "a version divergence must be reported")
        assertTrue(incomplete.missingTypes.isEmpty(), "the type is present - only the version diverges")
    }

    @Test
    fun `an unexpected artifact is simply ignored`() {
        val readiness = MaterialInspector.inspect(
            requirement(expected = listOf(MaterialArtifactType.SPG)),
            listOf(spg, manifest(MaterialArtifactType.CAPSULE, version = "1.0")),
        )

        assertIs<MaterialReadiness.Ready>(readiness)
    }

    @Test
    fun `an unknown producer type does not satisfy a requirement`() {
        val unknown = manifest(null, version = "1.0", rawType = "SomeFutureThing")

        val readiness = MaterialInspector.inspect(
            requirement(expected = listOf(MaterialArtifactType.SPG)),
            listOf(unknown),
        )

        val incomplete = assertIs<MaterialReadiness.Incomplete>(readiness)
        assertEquals(listOf(MaterialArtifactType.SPG), incomplete.missingTypes)
    }

    @Test
    fun `a requirement with no expected type is ready as soon as material exists`() {
        val readiness = MaterialInspector.inspect(requirement(expected = emptyList()), listOf(spg))

        assertIs<MaterialReadiness.Ready>(readiness)
    }

    @Test
    fun `a versioned requirement only checks types carrying a version`() {
        val versionless = manifest(MaterialArtifactType.SPG, version = null)

        val readiness = MaterialInspector.inspect(
            requirement(expected = listOf(MaterialArtifactType.SPG), version = "2.0"),
            listOf(versionless),
        )

        assertIs<MaterialReadiness.Ready>(readiness)
    }

    @Test
    fun `the verdict never throws on any combination`() {
        MaterialArtifactType.entries.forEach { type ->
            val readiness = MaterialInspector.inspect(
                requirement(expected = listOf(type), version = "1.0"),
                listOf(manifest(type, version = "1.0")),
            )
            assertIs<MaterialReadiness.Ready>(readiness)
        }
    }

    private fun requirement(
        expected: List<MaterialArtifactType>,
        version: String? = "1.0",
    ) = MaterialRequirement(
        remoteUrl = "https://github.com/cccp-education/formation-fpa",
        currentVersion = version,
        expectedTypes = expected,
    )

    private fun manifest(
        type: MaterialArtifactType?,
        version: String?,
        rawType: String = type?.pivotType.orEmpty(),
    ) = MaterialManifest(
        source = "training",
        type = type,
        rawType = rawType,
        version = version,
        model = "",
        dependencies = emptyList(),
    )
}

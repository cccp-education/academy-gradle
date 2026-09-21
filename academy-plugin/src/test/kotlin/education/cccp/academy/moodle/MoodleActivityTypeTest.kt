package education.cccp.academy.moodle

import education.cccp.academy.material.MaterialArtifactType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ACADEMY-11-1 — the material-to-activity mapping (D-ACADEMY-11-7).
 *
 * The vocabulary is Moodle's own (`public/mod` verified on the real image
 * `erseco/alpine-moodle:v5.2.3`, audit S-013): a page carries the session
 * content, a label carries the course-level presentation, a quiz carries the
 * evaluation, a resource/url link an external artifact. The mapping is total
 * over the material vocabulary — no default invented.
 */
class MoodleActivityTypeTest {

    @Test
    fun `the activity vocabulary is the real Moodle module set`() {
        assertEquals("label", MoodleActivityType.LABEL.moduleName)
        assertEquals("page", MoodleActivityType.PAGE.moduleName)
        assertEquals("quiz", MoodleActivityType.QUIZ.moduleName)
        assertEquals("resource", MoodleActivityType.RESOURCE.moduleName)
        assertEquals("url", MoodleActivityType.URL.moduleName)
    }

    @Test
    fun `every material artifact type maps to a real activity`() {
        assertEquals(MoodleActivityType.LABEL, MoodleActivityMapping.forMaterial(MaterialArtifactType.SPG))
        assertEquals(MoodleActivityType.PAGE, MoodleActivityMapping.forMaterial(MaterialArtifactType.SPD))
        assertEquals(MoodleActivityType.QUIZ, MoodleActivityMapping.forMaterial(MaterialArtifactType.QUIZ))
        assertEquals(MoodleActivityType.RESOURCE, MoodleActivityMapping.forMaterial(MaterialArtifactType.DOCUMENT))
        assertEquals(MoodleActivityType.URL, MoodleActivityMapping.forMaterial(MaterialArtifactType.SLIDES))
        assertEquals(MoodleActivityType.URL, MoodleActivityMapping.forMaterial(MaterialArtifactType.CAPSULE))
    }
}

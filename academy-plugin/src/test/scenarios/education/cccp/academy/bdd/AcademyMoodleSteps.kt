package education.cccp.academy.bdd

import education.cccp.academy.installer.InstallerPlatform
import education.cccp.academy.installer.InstallerScriptGenerator
import education.cccp.academy.installer.TargetOs
import education.cccp.academy.material.MaterialManifest
import education.cccp.academy.moodle.MoodleImportPlan
import education.cccp.academy.moodle.MoodleIngestionScriptGenerator
import education.cccp.academy.moodle.MoodleMaterial
import education.cccp.academy.moodle.MoodlePlanBuilder
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * Steps for `academy_moodle.feature` (ACADEMY-11-3/11-4) — pattern S-088
 * (feature-scoped glue). Drives the pure moodle domain directly: zero Gradle
 * task invocation, zero I/O, zero Moodle, zero PHP (D-ACADEMY-11-3).
 *
 * Every step is prefixed with the `moodle` vocabulary: the glue path is shared
 * with the installer/opencode/byok/bridge/material steps, so unprefixed phrases
 * such as `the material carries {string}` would collide (lesson S-088).
 */
class AcademyMoodleSteps : En {

    private var shortName: String = ""
    private var fullName: String = ""
    private val material: MutableList<MoodleMaterial> = mutableListOf()
    private var plan: MoodleImportPlan? = null
    private var planJson: String = ""
    private var script: String = ""
    private var installerOs: TargetOs = TargetOs.LINUX
    private var injectionEnabled: Boolean = false
    private var installerScript: String = ""

    init {
        Given("a moodle plan for course {string} named {string}") { short: String, full: String ->
            shortName = short
            fullName = full
            material.clear()
            plan = null
            planJson = ""
            script = ""
        }

        And("the material carries an artifact {string} of type {string}") { path: String, type: String ->
            material += MoodleMaterial(
                relativePath = path,
                manifest = MaterialManifest.fromJson(
                    """{ "source": "training", "type": "$type", "version": "1.0" }""",
                ) ?: error("the tolerant reader must parse a well-formed pivot"),
            )
        }

        And("the material carries no artifact") {
            material.clear()
        }

        When("the moodle plan is built") {
            plan = MoodlePlanBuilder.build(shortName, fullName, material)
        }

        When("the moodle plan json is rendered") {
            planJson = MoodleIngestionScriptGenerator.renderPlanJson(
                MoodlePlanBuilder.build(shortName, fullName, material),
            )
        }

        When("the moodle ingestion script is rendered") {
            script = MoodleIngestionScriptGenerator.renderApplicator(
                MoodlePlanBuilder.build(shortName, fullName, material),
            )
        }

        Given("a moodle material service installer for {string}") { target: String ->
            installerOs = TargetOs.entries.first { it.name.lowercase() == target }
            injectionEnabled = false
            installerScript = ""
        }

        And("moodle material injection is enabled") { injectionEnabled = true }
        And("moodle material injection is disabled") { injectionEnabled = false }

        When("the moodle material installer is rendered") {
            val built = MoodlePlanBuilder.build(shortName, fullName, material)
            val platform = InstallerPlatform(
                os = installerOs,
                applicationName = "academy",
                applicationVersion = "0.0.1",
                javaVersion = "25",
                gradleVersion = "9.7.1",
                composeEnabled = true,
                credentialsEnvPrefix = "ACADEMY_",
                moodleMaterialEnabled = injectionEnabled,
                moodlePlan = built,
            )
            installerScript = InstallerScriptGenerator.render(platform).single().content
        }

        Then("the moodle plan carries a {string} activity") { moduleName: String ->
            val activities = checkNotNull(plan).sections.flatMap { it.activities }
            assertThat(activities.map { it.type.moduleName }).contains(moduleName)
        }

        And("the moodle plan carries {int} {string} activities") { count: Int, moduleName: String ->
            val activities = checkNotNull(plan).sections.flatMap { it.activities }
            assertThat(activities.count { it.type.moduleName == moduleName }).isEqualTo(count)
        }

        And("the moodle plan carries the source path {string}") { path: String ->
            val activities = checkNotNull(plan).sections.flatMap { it.activities }
            assertThat(activities.map { it.sourcePath }).contains(path)
        }

        And("the moodle plan declares module {string}") { module: String ->
            assertThat(checkNotNull(plan).sections.mapNotNull { it.module }).contains(module)
        }

        And("the moodle plan is empty") {
            assertThat(checkNotNull(plan).isEmpty).isTrue()
        }

        Then("the plan json contains {string}") { fragment: String ->
            assertThat(planJson).contains(fragment)
        }

        And("the plan json braces balance") {
            assertThat(planJson.count { it == '{' }).isEqualTo(planJson.count { it == '}' })
            assertThat(planJson.count { it == '[' }).isEqualTo(planJson.count { it == ']' })
            assertThat(planJson).doesNotContain(",}")
            assertThat(planJson).doesNotContain(",]")
        }

        Then("the moodle script contains {string}") { fragment: String ->
            assertThat(script).contains(fragment)
        }

        And("the moodle script never contains {string}") { fragment: String ->
            assertThat(script).doesNotContain(fragment)
        }

        Then("the moodle material installer contains {string}") { fragment: String ->
            assertThat(installerScript).contains(fragment)
        }

        And("the moodle material installer never contains {string}") { fragment: String ->
            assertThat(installerScript).doesNotContain(fragment)
        }
    }
}

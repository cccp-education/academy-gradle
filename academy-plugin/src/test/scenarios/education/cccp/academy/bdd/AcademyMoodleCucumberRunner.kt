package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_moodle.feature` (ACADEMY-11-3) —
 * pattern S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so
 * only the moodle feature runs, filtered to `@moodle`. Glue is bound to the
 * shared `education.cccp.academy.bdd` path (all step classes live there with
 * distinct vocabularies, lesson S-088).
 *
 * The scenarios lock the injection contract: academy builds a plan from the
 * material structure and the EPIC K pivots, generates data plus a generic
 * applicator using the official Moodle API, never writes SQL, never reads the
 * AsciiDoc body, and stays degraded by default.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_moodle.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-moodle.html, json:build/reports/cucumber-moodle.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@moodle and not @wip and not @integration"
)
class AcademyMoodleCucumberRunner

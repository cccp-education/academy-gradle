package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_a11y.feature` (ACADEMY-15) — pattern
 * S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so only the
 * accessibility feature runs, filtered to `@a11y`. Glue is bound to the shared
 * `education.cccp.academy.bdd` path (all step classes live there with distinct
 * vocabularies, lesson S-088).
 *
 * The scenarios lock the measurement contract: video companions, producer report
 * consumption, coverage (a silent report never verifies a deliverable), declared
 * limits, and tolerance to a malformed report.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_a11y.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-a11y.html, json:build/reports/cucumber-a11y.json",
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@a11y and not @wip and not @integration",
)
class AcademyA11yCucumberRunner

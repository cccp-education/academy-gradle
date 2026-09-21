package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_material.feature` (ACADEMY-4-3) —
 * pattern S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so
 * only the material feature runs, filtered to `@material`. Glue is bound to the
 * shared `education.cccp.academy.bdd` path (all step classes live there with
 * distinct vocabularies, lesson S-088).
 *
 * The scenarios lock the consumption contract: academy reads the EPIC K pivot,
 * computes a verdict, never executes git, never fails on absent material, and
 * never cites a bureau command that does not exist (BUREAU-3 TODO, S-012).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_material.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-material.html, json:build/reports/cucumber-material.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@material and not @wip and not @integration"
)
class AcademyMaterialCucumberRunner

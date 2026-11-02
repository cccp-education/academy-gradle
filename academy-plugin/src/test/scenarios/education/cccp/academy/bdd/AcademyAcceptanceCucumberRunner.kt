package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_acceptance.feature` (ACADEMY-15 /
 * GBL-011-5) — pattern S-082 (dedicated runner). Scoped via
 * `@SelectClasspathResource` so only the acceptance feature runs, filtered to
 * `@acceptance`. Glue is bound to the shared `education.cccp.academy.bdd` path
 * (all step classes live there with distinct vocabularies, lesson S-088).
 *
 * The scenarios lock the box verdict: the conjunction of the layer audits, the
 * declared gap of a missing layer, the carried limits and the two limits no
 * machine audit can lift.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_acceptance.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-acceptance.html, json:build/reports/cucumber-acceptance.json",
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@acceptance and not @wip and not @integration",
)
class AcademyAcceptanceCucumberRunner

package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_opencode.feature` (ACADEMY-5-1) —
 * pattern S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so
 * only the opencode feature runs, filtered to `@opencode`. Glue is bound to
 * [AcademyOpenCodeSteps].
 *
 * The scenarios lock the exposure contract: an opencode config targeting the
 * embedded ollama runtime, a learner guide inventorying the public ecosystem
 * tasks, and a workspace image built on the pinned gradle base.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_opencode.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-opencode.html, json:build/reports/cucumber-opencode.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@opencode and not @wip and not @integration"
)
class AcademyOpenCodeCucumberRunner

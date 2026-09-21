package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_bridge.feature` (ACADEMY-8-3) — pattern
 * S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so only the
 * bridge feature runs, filtered to `@bridge`. Glue is bound to the shared
 * `education.cccp.academy.bdd` path (all step classes live there with distinct
 * vocabularies, lesson S-088).
 *
 * The scenarios lock the bridge contract: academy's own event schema, a stable
 * (replayable) session id, an opaque payload carried verbatim, and a route
 * table that cannot drift from what is served.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_bridge.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-bridge.html, json:build/reports/cucumber-bridge.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@bridge and not @wip and not @integration"
)
class AcademyBridgeCucumberRunner

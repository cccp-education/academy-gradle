package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_byok.feature` (ACADEMY-7-3) — pattern
 * S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so only the
 * byok feature runs, filtered to `@byok`. Glue is bound to the shared
 * `education.cccp.academy.bdd` path (both [AcademyByokSteps] and
 * [AcademyOpenCodeSteps] live there; the steps use distinct vocabularies).
 *
 * The scenarios lock the multi-provider contract: the provider id, package and
 * endpoint come from the verified catalog, and a key is only ever referenced by
 * the name of its environment variable (`{env:NAME}`) — never a value.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_byok.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-byok.html, json:build/reports/cucumber-byok.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@byok and not @wip and not @integration"
)
class AcademyByokCucumberRunner

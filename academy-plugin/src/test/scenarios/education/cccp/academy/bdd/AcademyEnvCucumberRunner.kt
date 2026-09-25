package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_env.feature` (ACADEMY-12-1) —
 * pattern S-082 (dedicated runner). Scoped via `@SelectClasspathResource` so
 * only the environment feature runs, filtered to `@env`. Glue is bound to the
 * shared `education.cccp.academy.bdd` path (all step classes live there with
 * distinct vocabularies, lesson S-088).
 *
 * The scenarios lock the per-OS provisioning matrix: Linux installs the native
 * Docker Engine, Windows installs Docker Desktop (WSL2 required), macOS
 * installs Colima, and an unprovisionable host is an explicit Unsupported
 * verdict that never throws.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_env.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-env.html, json:build/reports/cucumber-env.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@env and not @wip and not @integration"
)
class AcademyEnvCucumberRunner

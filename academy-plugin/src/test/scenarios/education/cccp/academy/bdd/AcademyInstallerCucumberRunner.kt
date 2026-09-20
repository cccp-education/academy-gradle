package education.cccp.academy.bdd

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `academy_installer.feature` (ACADEMY-1-2) —
 * pattern S-082 (first dedicated runner academy).
 *
 * Scoped via `@SelectClasspathResource` so only the installer feature runs,
 * filtered to `@installer`. Glue is bound to [AcademyInstallerSteps].
 *
 * The scenarios lock the socle contract: a deterministic bash/batch script
 * per platform with the pinned toolchain, Docker host bootstrap, idempotence
 * guard, environment-only credentials, and an optional docker-compose
 * scaffold (Moodle + MariaDB + opencode workspace).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/academy_installer.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "education.cccp.academy.bdd")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-installer.html, json:build/reports/cucumber-installer.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@installer and not @wip and not @integration"
)
class AcademyInstallerCucumberRunner
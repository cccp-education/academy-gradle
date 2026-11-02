@moodle-a11y
Feature: Academy Moodle accessibility audit (ACADEMY-15 / GBL-011-5)

  Academy measures the accessibility of the Moodle box it installs. The audit
  reads the HTML the server actually returns and applies the structural checks a
  theme or a plugin breaks first: document language, image alternatives, form
  control names, table headers and the first level heading. It is browser-free,
  so focus order, contrast and JavaScript behaviour are declared as limits - a
  report never claims a check it did not run.

  Scenario: an accessible Moodle page is verified
    Given a Moodle page with a lang, a heading, a labelled control and a table with a header
    When the Moodle accessibility is audited
    Then the Moodle accessibility readiness is "VERIFIED"
    And the Moodle accessibility report contains no finding

  Scenario: a Moodle page without lang degrades the report
    Given a Moodle page with a lang, a heading, a labelled control and a table with a header
    And the Moodle page has no lang attribute
    When the Moodle accessibility is audited
    Then the Moodle accessibility readiness is "DEGRADED"
    And the Moodle accessibility report contains the rule "moodle-lang-missing" with severity "ERROR"

  Scenario: an image without an alternative degrades the report
    Given a Moodle page with a lang, a heading, a labelled control and a table with a header
    And the Moodle page has an image without an alt attribute
    When the Moodle accessibility is audited
    Then the Moodle accessibility report contains the rule "moodle-image-alt-missing" with severity "ERROR"

  Scenario: a form control without an accessible name degrades the report
    Given a Moodle page with a lang, a heading, a labelled control and a table with a header
    And the Moodle page has a form control without an accessible name
    When the Moodle accessibility is audited
    Then the Moodle accessibility report contains the rule "moodle-form-control-label-missing" with severity "ERROR"

  Scenario: a table without a header is a warning
    Given a Moodle page with a lang, a heading, a labelled control and a table with a header
    And the Moodle page has a table without a header cell
    When the Moodle accessibility is audited
    Then the Moodle accessibility readiness is "DEGRADED"
    And the Moodle accessibility report contains the rule "moodle-table-header-missing" with severity "WARNING"

  Scenario: an unreachable Moodle is a declared warning, never a crash
    Given an unreachable Moodle page
    When the Moodle accessibility is audited
    Then the Moodle accessibility readiness is "DEGRADED"
    And the Moodle accessibility report contains the rule "moodle-page-unreachable" with severity "WARNING"

  Scenario: no observed page is a missing report
    Given no Moodle page is observed
    When the Moodle accessibility is audited
    Then the Moodle accessibility readiness is "MISSING"

  Scenario: the report declares what it did not measure
    Given a Moodle page with a lang, a heading, a labelled control and a table with a header
    When the Moodle accessibility is audited
    Then the Moodle accessibility report declares its limits
    And the Moodle accessibility report declares that static HTML is its limit
    And the Moodle accessibility report declares that opencode is out of scope

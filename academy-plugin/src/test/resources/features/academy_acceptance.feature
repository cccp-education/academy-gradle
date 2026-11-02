@acceptance
Feature: Academy box accessibility acceptance (ACADEMY-15 / GBL-011-5)

  The acceptance gate aggregates the machine audits of the box (pulled material
  and running Moodle) into one verdict. A missing layer is declared as a gap,
  never silently skipped, and the report always states what no machine audit can
  lift: the usage test with a concerned person and the opencode terminal.

  Scenario: two verified audits are accepted
    Given a verified material audit report
    And a verified Moodle audit report
    When the box accessibility acceptance is computed
    Then the acceptance readiness is "VERIFIED"
    And the acceptance has 2 layers
    And the acceptance contains no finding

  Scenario: no audit report is a missing acceptance
    Given no accessibility audit report
    When the box accessibility acceptance is computed
    Then the acceptance readiness is "MISSING"
    And the acceptance has 0 layers

  Scenario: a missing layer is a declared gap
    Given a verified material audit report
    When the box accessibility acceptance is computed
    Then the acceptance readiness is "DEGRADED"
    And the acceptance declares the missing "moodle" layer

  Scenario: an error finding degrades the acceptance
    Given a verified material audit report
    And a verified Moodle audit report
    And the material audit report contains the error rule "video-subtitles-missing"
    When the box accessibility acceptance is computed
    Then the acceptance readiness is "DEGRADED"
    And the acceptance contains the rule "video-subtitles-missing" with severity "ERROR"

  Scenario: the layer limits are carried and deduplicated
    Given a verified material audit report declaring the limit "PDF is not PDF/UA"
    And a verified Moodle audit report declaring the limit "PDF is not PDF/UA"
    When the box accessibility acceptance is computed
    Then the acceptance declares the limit "PDF is not PDF/UA" once

  Scenario: the acceptance always declares what no machine audit can lift
    Given a verified material audit report
    And a verified Moodle audit report
    When the box accessibility acceptance is computed
    Then the acceptance declares the usage test limit
    And the acceptance declares the opencode limit

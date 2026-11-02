@a11y
Feature: Academy training material accessibility audit (ACADEMY-15)

  Academy measures the accessibility of the delivered training material and
  reports what it could not verify. The producer (document for the text
  deliverables, capsule for the videos) owns the checks; academy aggregates the
  producer reports and inspects the video companions. A report that does not
  cover a deliverable never verifies it, and a malformed report is a declared
  limit, never a crash.

  Scenario: no material at all is a missing report, never a crash
    Given an empty training material for accessibility
    When the material accessibility is audited
    Then the accessibility readiness is "MISSING"
    And the accessibility report declares its limits

  Scenario: a video without companions degrades the report
    Given a training material with the video "clip.mp4" lacking its accessibility companions
    When the material accessibility is audited
    Then the accessibility readiness is "DEGRADED"
    And the accessibility report contains the rule "video-subtitles-missing" with severity "ERROR"

  Scenario: a video with every companion is verified
    Given a training material with the video "clip.mp4" carrying subtitles, transcript and audio description
    When the material accessibility is audited
    Then the accessibility readiness is "VERIFIED"
    And the accessibility report contains no finding

  Scenario: a text deliverable without producer report is unverifiable
    Given a training material with the text deliverable "cours.html"
    When the material accessibility is audited
    Then the accessibility readiness is "DEGRADED"
    And the accessibility report contains the rule "producer-report-missing" with severity "WARNING"

  Scenario: a fully covered text deliverable is verified
    Given a training material with the text deliverable "cours.html"
    And a producer accessibility report covering "HTML" with no finding
    When the material accessibility is audited
    Then the accessibility readiness is "VERIFIED"
    And the accessibility report contains no finding

  Scenario: a report that does not cover a deliverable never verifies it
    Given a training material with the text deliverable "cours.html"
    And the training material also contains the text deliverable "cours.pdf"
    And a producer accessibility report covering "HTML" with no finding
    When the material accessibility is audited
    Then the accessibility readiness is "DEGRADED"
    And the accessibility report contains the rule "producer-report-incomplete" with severity "WARNING"
    And the accessibility report names the uncovered deliverable "PDF"

  Scenario: producer findings and declared limits are aggregated verbatim
    Given a training material with the text deliverable "cours.html"
    And a producer accessibility report covering "HTML" declaring the finding "html-lang-missing"
    When the material accessibility is audited
    Then the accessibility readiness is "DEGRADED"
    And the accessibility report contains the rule "html-lang-missing" with severity "ERROR"
    And the accessibility report declares the limit "PDF not tagged"

  Scenario: a malformed producer report is a declared limit, never a crash
    Given a training material with the text deliverable "cours.html"
    And a malformed producer accessibility report
    When the material accessibility is audited
    Then the accessibility readiness is "DEGRADED"
    And the accessibility report contains the rule "producer-report-missing" with severity "WARNING"
    And the accessibility report declares a malformed producer report

@material
Feature: Academy training material consumption (ACADEMY-4)

  Academy consumes versioned training material produced by the bureau. The
  delivery mechanism is frozen: the learner PULLS tagged versions (git); the
  creator never pushes. Academy reads the EPIC K metadata.json pivot of every
  artifact and reports a verdict - it never executes git, and it never fails
  on material that is legitimately absent (the resource is generated before
  the pull).

  Scenario: a requirement declares the remote, the version and the expected artifacts
    Given a material requirement for remote "https://github.com/cccp-education/formation-fpa" at version "v1.0"
    And the requirement expects the artifacts "SPG,SLIDES"
    When the material requirement is read
    Then the requirement declares the remote "https://github.com/cccp-education/formation-fpa"
    And the requirement declares the version "v1.0"
    And the requirement declares the artifact "SPG"
    And the requirement declares the artifact "SLIDES"

  Scenario: no material at all is an empty report, never an error
    Given a material requirement for remote "https://example.org/training.git" at version "v1.0"
    And no training material is present
    When the material requirement is inspected
    Then the material verdict is "EMPTY"

  Scenario: the pull is a tagged version, never a push by the creator
    Given a material requirement for remote "https://example.org/training.git" at version "v1.0"
    When the material guide is rendered
    Then the material guide contains "git"
    And the material guide contains "v1.0"
    And the material guide never cites "pullMaterial"

  Scenario: every expected artifact present with the matching version is ready
    Given a material requirement for remote "https://example.org/training.git" at version "1.0"
    And the requirement expects the artifacts "SPG,SLIDES"
    And a pivot manifest of type "SPG" at version "1.0"
    And a pivot manifest of type "SLIDES" at version "1.0"
    When the material requirement is inspected
    Then the material verdict is "READY"

  Scenario: a missing expected artifact is incomplete
    Given a material requirement for remote "https://example.org/training.git" at version "1.0"
    And the requirement expects the artifacts "SPG,QUIZ"
    And a pivot manifest of type "SPG" at version "1.0"
    When the material requirement is inspected
    Then the material verdict is "INCOMPLETE"
    And the material report names the missing artifact "QUIZ"

  Scenario: a diverging version is incomplete and reported outdated
    Given a material requirement for remote "https://example.org/training.git" at version "2.0"
    And the requirement expects the artifacts "SPG"
    And a pivot manifest of type "SPG" at version "1.0"
    When the material requirement is inspected
    Then the material verdict is "INCOMPLETE"
    And the material report marks the material outdated

  Scenario: an unknown producer type never satisfies a requirement
    Given a material requirement for remote "https://example.org/training.git" at version "1.0"
    And the requirement expects the artifacts "SPG"
    And a pivot manifest of unknown type "SomeFutureThing" at version "1.0"
    When the material requirement is inspected
    Then the material verdict is "INCOMPLETE"
    And the material report names the missing artifact "SPG"

  Scenario: a producer adding a field never breaks the consumer
    Given a material requirement for remote "https://example.org/training.git" at version "1.0"
    And the requirement expects the artifacts "SPG"
    And a pivot manifest of type "SPG" at version "1.0" carrying an unknown field
    When the material requirement is inspected
    Then the material verdict is "READY"

@moodle
Feature: Academy material injection into Moodle (ACADEMY-11)

  Academy consumes the training material produced by training and turns it into
  a Moodle import plan: the structure carries the module and session layout, the
  EPIC K metadata.json pivot carries the artifact type. Academy generates DATA
  (a plan.json) and a GENERIC applicator (a CLI script using the official Moodle
  API) - it never parses the AsciiDoc body, never writes SQL, and never embeds a
  formation identity in the script.

  Scenario: an SPG becomes a course-level presentation activity
    Given a moodle plan for course "academy-seed" named "Academy"
    And the material carries an artifact "SPG/spg.adoc" of type "SPG"
    When the moodle plan is built
    Then the moodle plan carries a "label" activity
    And the moodle plan carries the source path "SPG/spg.adoc"

  Scenario: each SPD module becomes a section and each session a page
    Given a moodle plan for course "academy-seed" named "Academy"
    And the material carries an artifact "SPD/01_accueil/001_bienvenue.adoc" of type "SPD"
    And the material carries an artifact "SPD/02_coeur/002_algorithmes.adoc" of type "SPD"
    When the moodle plan is built
    Then the moodle plan declares module "01_accueil"
    And the moodle plan declares module "02_coeur"
    And the moodle plan carries 2 "page" activities

  Scenario: a quiz becomes a quiz activity
    Given a moodle plan for course "academy-seed" named "Academy"
    And the material carries an artifact "QUIZ/quiz.adoc" of type "QUIZ"
    When the moodle plan is built
    Then the moodle plan carries a "quiz" activity

  Scenario: an unknown producer type is never guessed
    Given a moodle plan for course "academy-seed" named "Academy"
    And the material carries an artifact "MYSTERY/thing.adoc" of type "SomeFutureThing"
    When the moodle plan is built
    Then the moodle plan is empty

  Scenario: an empty material produces an empty plan - degraded by default
    Given a moodle plan for course "academy-seed" named "Academy"
    And the material carries no artifact
    When the moodle plan is built
    Then the moodle plan is empty

  Scenario: the generated plan is balanced json carrying the course identity
    Given a moodle plan for course "formation-fpa" named "Formation FPA"
    And the material carries an artifact "SPG/spg.adoc" of type "SPG"
    When the moodle plan json is rendered
    Then the plan json contains "\"courseShortName\": \"formation-fpa\""
    And the plan json braces balance

  Scenario: the applicator drives the native tools of the image
    Given a moodle plan for course "formation-fpa" named "Formation FPA"
    And the material carries an artifact "SPG/spg.adoc" of type "SPG"
    When the moodle ingestion script is rendered
    Then the moodle script contains "moosh course-create"
    And the moodle script contains "moosh activity-add"
    And the moodle script never contains "<?php"
    And the moodle script never contains "INSERT INTO mdl_"

  Scenario: the applicator resolves the numeric course id before adding activities
    Given a moodle plan for course "formation-fpa" named "Formation FPA"
    And the material carries an artifact "SPG/spg.adoc" of type "SPG"
    When the moodle ingestion script is rendered
    Then the moodle script contains "COURSEID"

  Scenario: the applicator reads the content moodle-side and never embeds it
    Given a moodle plan for course "formation-fpa" named "Formation FPA"
    And the material carries an artifact "SPD/01_accueil/001_bienvenue.adoc" of type "SPD"
    When the moodle ingestion script is rendered
    Then the moodle script contains "moosh section-config-set"
    And the moodle script contains "01_accueil"
    And the moodle script contains "cat \"$SRC\""

  Scenario: the applicator script never embeds a credential
    Given a moodle plan for course "formation-fpa" named "Formation FPA"
    And the material carries an artifact "SPG/spg.adoc" of type "SPG"
    When the moodle ingestion script is rendered
    Then the moodle script never contains "PASSWORD="
    And the moodle script never contains "TOKEN="

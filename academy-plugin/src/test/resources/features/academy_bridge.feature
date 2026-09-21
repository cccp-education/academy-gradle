@bridge
Feature: Academy local webhook bridge (ACADEMY-8)

  A local, opt-in Ktor bridge routes inbound formation events to the N0
  session contracts and acknowledges them. It never executes the agent and
  never carries a credential: the bridge routes, opencode executes.

  Scenario: the inbound event schema is academy's own and carries an opaque payload
    Given a moodle event "course_module_completed" for course "academy-seed" and learner "learner-1"
    And the event payload "module" is "M01"
    When the bridge translator builds the session prompt
    Then the bridge prompt contains "course_module_completed"
    And the bridge prompt contains "academy-seed"
    And the bridge prompt contains "learner-1"
    And the bridge context contains "module=M01"

  Scenario: translating the same event twice yields the same stable session
    Given a moodle event "quiz_submitted" for course "academy-seed" and learner "learner-1"
    When the bridge translator builds the session prompt twice
    Then the two bridge session ids are equal

  Scenario: two distinct events map to two distinct sessions
    Given a moodle event "course_viewed" for course "c1" and learner "u1"
    And a second moodle event "quiz_submitted" for course "c1" and learner "u1"
    When both bridge events are translated
    Then the two bridge session ids differ

  Scenario: an event without a payload invents no context
    Given a moodle event "course_viewed" for course "academy-seed" and learner "learner-1"
    When the bridge translator builds the session prompt
    Then the bridge context is empty

  Scenario: the route table documents every served route
    When the bridge route table is read
    Then the bridge declares route "/health"
    And the bridge declares route "/events/moodle"
    And the bridge declares route "/session"
    And the bridge health route is a "GET"
    And the bridge event route is a "POST"

  Scenario: the event schema is not a third party moodle format
    Given a moodle event "course_module_completed" for course "academy-seed" and learner "learner-1"
    When the bridge translator builds the session prompt
    Then the bridge prompt does not claim "local_webhooks"
    And the bridge prompt does not claim "mod_hvp"

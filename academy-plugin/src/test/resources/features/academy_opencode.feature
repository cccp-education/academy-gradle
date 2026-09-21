@opencode
Feature: Academy opencode exposure (ACADEMY-5)

  The academy workspace exposes the ecosystem's Gradle tasks to the learner
  through a generated opencode configuration, a learner guide (AGENTS.md,
  opencode's official anchor) and a workspace image built on the pinned
  gradle base — public boroughs only, never a credential.

  Scenario: the opencode config targets the embedded ollama runtime
    Given an opencode config with provider url "http://ollama:11434/v1"
    And model "gpt-oss:120b-cloud"
    When the opencode config generator renders the config
    Then the json contains "\"$schema\": \"https://opencode.ai/config.json\""
    And the json contains "\"model\": \"ollama/gpt-oss:120b-cloud\""
    And the json contains "\"baseURL\": \"http://ollama:11434/v1\""

  Scenario: the opencode config is valid balanced json
    Given an opencode config with provider url "http://ollama:11434/v1"
    And model "gpt-oss:120b-cloud"
    When the opencode config generator renders the config
    Then the json braces balance

  Scenario: the opencode config never embeds a credential
    Given an opencode config with provider url "http://ollama:11434/v1"
    And model "gpt-oss:120b-cloud"
    When the opencode config generator renders the config
    Then the json contains no credential

  Scenario: the learner guide inventories the public ecosystem tasks
    Given an opencode config with provider url "http://ollama:11434/v1"
    And model "gpt-oss:120b-cloud"
    When the learner guide generator renders the guide
    Then the guide contains "education.cccp.slider"
    And the guide contains "generateDeck"
    And the guide contains "foundry/public"
    And the guide does not contain "foundry/private"

  Scenario: the workspace image builds on the pinned gradle base
    Given an opencode config with provider url "http://ollama:11434/v1"
    And model "gpt-oss:120b-cloud"
    When the workspace dockerfile generator renders the dockerfile
    Then the dockerfile contains "FROM gradle:9.7.1-jdk25"
    And the dockerfile contains "zsh"
    And the dockerfile contains "opencode"
    And the dockerfile contains "learner"

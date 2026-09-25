@env
Feature: Academy container engine provisioning across operating systems (ACADEMY-12)

  Academy provisions the container engine the learner needs, per operating
  system: the native Docker Engine on Linux, Docker Desktop on Windows (WSL2
  required, managed by Desktop), Colima on macOS. Portainer is layered on every
  OS - the strategy only decides how the ENGINE is provisioned. The decision is
  a pure function of observed facts: it never throws, and an unprovisionable
  host is an explicit Unsupported verdict that explains why.

  Scenario: a reachable engine needs no provisioning on any OS
    Given an environment on "linux" where the docker engine is ready
    When the container engine strategy is decided
    Then the strategy is already ready

  Scenario: Linux provisions the native Docker Engine
    Given an environment on "linux" where the docker engine is not ready
    When the container engine strategy is decided
    Then the strategy installs the native docker engine

  Scenario: macOS provisions Colima, never Docker Desktop
    Given an environment on "macos" where the docker engine is not ready
    When the container engine strategy is decided
    Then the strategy installs colima

  Scenario: Windows with WSL2 provisions Docker Desktop
    Given an environment on "windows" where the docker engine is not ready
    And the environment has WSL2
    When the container engine strategy is decided
    Then the strategy installs docker desktop

  Scenario: Windows without WSL2 and with admin rights bootstraps WSL2 first
    Given an environment on "windows" where the docker engine is not ready
    And the environment has no WSL2
    And the environment runs with administrator rights
    When the container engine strategy is decided
    Then the strategy bootstraps WSL2 first

  Scenario: Windows without WSL2 and without admin rights is unsupported
    Given an environment on "windows" where the docker engine is not ready
    And the environment has no WSL2
    And the environment runs without administrator rights
    When the container engine strategy is decided
    Then the strategy is unsupported with a reason

  Scenario: an undetected operating system is unsupported, never a crash
    Given an environment on "unknown" where the docker engine is not ready
    When the container engine strategy is decided
    Then the strategy is unsupported with a reason

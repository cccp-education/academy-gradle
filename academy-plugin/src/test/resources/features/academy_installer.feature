@installer
Feature: Academy installer generation (ACADEMY-1)

  The academy installer socle renders deterministic, per-platform
  installers — host bootstrap (Docker, Java Temurin, Gradle), idempotence
  guard, environment-only credentials and an optional docker-compose
  scaffold — without any I/O or network.

  Scenario: linux installer is a deterministic bash script with the pinned toolchain
    Given a platform targeting "linux"
    And application name "academy"
    And version "0.0.1"
    And Java major "25"
    And Gradle "9.7.1"
    And compose embedding enabled
    When the installer generator renders the platform
    Then exactly one file "install.sh" is produced
    And the script starts with "#!/usr/bin/env bash"
    And the script contains "JAVA_VERSION=\"25\""
    And the script contains "GRADLE_VERSION=\"9.7.1\""
    And the script is idempotent
    And the script bootstraps Docker

  Scenario: compose scaffold is embedded only when enabled
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "docker-compose.yml"
    And the script contains "moodle"
    And the script contains "postgres"
    And the script contains "POSTGRES_PASSWORD"
    And the script does not contain "mariadb"
    And the script contains "ollama"
    And the script contains "portainer"

    Given a platform targeting "linux"
    And compose embedding disabled
    When the installer generator renders the platform
    Then the script does not contain "docker-compose.yml"

  Scenario: credentials are a convention, never embedded values
    Given a platform targeting "linux"
    And credential env prefix "MY_ACADEMY_"
    When the installer generator renders the platform
    Then the script contains "MY_ACADEMY_"
    And the script does not contain "SECRET="
    And the script does not contain "TOKEN="

  Scenario: windows installer is a batch script with admin check and setx
    Given a platform targeting "windows"
    When the installer generator renders the platform
    Then exactly one file "install.bat" is produced
    And the script starts with "@echo off"
    And the script contains "net session"
    And the script contains "setx"
    And the script uses PowerShell

  Scenario: macos installer uses Homebrew Docker Desktop without sudo
    Given a platform targeting "macos"
    When the installer generator renders the platform
    Then exactly one file "install.sh" is produced
    And the script contains "brew install --cask docker"
    And the script contains "uname -m"
    And the script does not contain "sudo"
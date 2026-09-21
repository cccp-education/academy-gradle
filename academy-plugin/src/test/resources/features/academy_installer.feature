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

  Scenario: compose scaffold declares network named volumes postgres healthcheck and env file
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "networks:"
    And the script contains "academy-net"
    And the script contains "postgres-data:"
    And the script contains "ollama-models:"
    And the script contains "portainer-data:"
    And the script contains "pg_isready"
    And the script contains ".env"
    And the script contains "POSTGRES_PASSWORD="
    And the script contains "DB_TYPE"
    And the script contains "pgsql"

  Scenario: windows compose scaffold mirrors the complete linux contract
    Given a platform targeting "windows"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "networks:"
    And the script contains "academy-net"
    And the script contains "postgres-data:"
    And the script contains "ollama-models:"
    And the script contains "portainer-data:"
    And the script contains "pg_isready"
    And the script contains ".env"
    And the script contains "POSTGRES_PASSWORD="
    And the script contains "DB_TYPE"
    And the script contains "pgsql"

  Scenario: moodle service uses the real latest image with its actual environment contract
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "erseco/alpine-moodle:v5.2.3"
    And the script does not contain "moodle:4.5"
    And the script contains "DB_HOST"
    And the script contains "MOODLE_USERNAME"
    And the script contains "MOODLE_SITENAME"
    And the script does not contain "MOODLE_DATABASE_TYPE"

  Scenario: portainer uses the maintained community edition image
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "portainer/portainer-ce"

  Scenario: the unpublished workspace image is deferred with a TODO
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script does not contain "cccp-education/academy-workspace"
    And the script contains "TODO ACADEMY-5"

  Scenario: env documents the moodle runtime variables without values
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "POSTGRES_USER="
    And the script contains "MOODLE_USERNAME="
    And the script contains "MOODLE_PASSWORD="
    And the script contains "MOODLE_SITENAME="

  Scenario: compose declares a one-shot seed service applying the course sql
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "moodle-seed"
    And the script contains "restart: \"no\""
    And the script contains "seed/seed.sh"
    And the script contains "seed/seed-course.sql"
    And the script contains "./seed:/seed"

  Scenario: seed entrypoint is idempotent and waits for the Moodle schema
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "set -eu"
    And the script contains "mdl_course"
    And the script contains "until psql"
    And the script contains "PGPASSWORD"
    And the script contains "seed-course.sql"

  Scenario: course seed sql is data-only and idempotent
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "INSERT INTO mdl_course"
    And the script contains "WHERE NOT EXISTS"
    And the script contains "academy-seed"
    And the script does not contain "DROP "
    And the script does not contain "DELETE "
    And the script does not contain "TRUNCATE "
    And the script does not contain "UPDATE "

  Scenario: windows installer writes the seed files with cmd-safe escaping
    Given a platform targeting "windows"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "moodle-seed"
    And the script contains "restart: \"no\""
    And the script contains "seed\seed.sh"
    And the script contains "seed\seed-course.sql"
    And the script contains "2^>^&1"
    And the script contains "mdl_course"
    And the script contains "WHERE NOT EXISTS"

  Scenario: linux host bootstrap provisions Docker only
    Given a platform targeting "linux"
    When the installer generator renders the platform
    Then the script contains "command -v docker"
    And the script does not contain "JAVA_HOME"
    And the script does not contain "temurin"
    And the script does not contain "services.gradle.org"

  Scenario: windows host bootstrap provisions Docker only with the admin guard
    Given a platform targeting "windows"
    When the installer generator renders the platform
    Then the script contains "net session"
    And the script contains "Get-Command docker"
    And the script does not contain "adoptium"
    And the script does not contain "services.gradle.org"
    And the script does not contain "setx JAVA_HOME"

  Scenario: macos host bootstrap provisions Docker only without sudo
    Given a platform targeting "macos"
    When the installer generator renders the platform
    Then the script contains "brew install --cask docker"
    And the script does not contain "brew install openjdk"
    And the script does not contain "brew install gradle"
    And the script does not contain "sudo"

  Scenario: installers abort with an actionable message when Docker is missing
    Given a platform targeting "linux"
    When the installer generator renders the platform
    Then the script contains "Docker is required"
    And the script contains "exit 1"

  Scenario: compose scaffold is staged under the application directory
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script contains "mkdir -p \"$APP_DIR\""
    And the script contains "$APP_DIR/docker-compose.yml"
    And the script contains "$APP_DIR/.env"
    And the script contains "$APP_DIR/seed/seed.sh"

  Scenario: opencode configuration is deferred until the workspace image is published
    Given a platform targeting "linux"
    And compose embedding enabled
    When the installer generator renders the platform
    Then the script does not contain "opencode.json"
    And the script contains "TODO ACADEMY-5"

  Scenario: credentials are a convention, never embedded values
    Given a platform targeting "linux"
    And credential env prefix "MY_ACADEMY_"
    When the installer generator renders the platform
    Then the script contains "MY_ACADEMY_"
    And the script does not contain "SECRET="
    And the script does not contain "TOKEN="

  Scenario: windows installer is a batch script with admin check and docker bootstrap
    Given a platform targeting "windows"
    When the installer generator renders the platform
    Then exactly one file "install.bat" is produced
    And the script starts with "@echo off"
    And the script contains "net session"
    And the script contains "Get-Command docker"
    And the script uses PowerShell

  Scenario: macos installer uses Homebrew Docker Desktop without sudo
    Given a platform targeting "macos"
    When the installer generator renders the platform
    Then exactly one file "install.sh" is produced
    And the script contains "brew install --cask docker"
    And the script contains "uname -m"
    And the script does not contain "sudo"
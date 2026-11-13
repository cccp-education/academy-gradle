#!/usr/bin/env bash
# GBL-013 US-5 (wave 2) — CI contract for academy-gradle.
# The publication adapter (Portal/Central, GPG/OSSRH) stays on the host; this
# contract covers the build gate and the keyless POM check.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/academy-plugin"

verify_pom() {
  local pom=build/publications/pluginMaven/pom-default.xml
  test -f "$pom"
  grep -q "<groupId>education.cccp</groupId>" "$pom"
  grep -q "<artifactId>academy-plugin</artifactId>" "$pom"
  grep -Eq "<version>[0-9]+\.[0-9]+\.[0-9]+</version>" "$pom"
  ! grep -q "SNAPSHOT" "$pom"
}

job="${1:-}"
case "$job" in
  fast|test)
    ./gradlew build --no-daemon --console=plain
    ;;
  publish)
    case "${PUBLISH_TARGET:-pom}" in
      pom)
        ./gradlew generatePomFileForPluginMavenPublication --no-daemon --console=plain
        verify_pom
        ;;
      *)
        printf 'unsupported publish target for academy-gradle: %s\n' "$PUBLISH_TARGET" >&2
        printf 'supported: pom (the Portal/Central publication stays on the host adapter)\n' >&2
        exit 2
        ;;
    esac
    ;;
  *)
    printf 'unsupported job for academy-gradle: %s\n' "${job:-<none>}" >&2
    printf 'supported: fast | test | publish\n' >&2
    exit 2
    ;;
esac

plugins {
    id("education.cccp.academy")
}

// ACADEMY-14 — the dogfooding build always opts into the offline bundle: it is
// the only consumer of this build, and the dedicated workflow builds the archive
// then starts the box with `--pull never --no-build`.
academyInstaller {
    offlineBundleEnabled.set(true)
}

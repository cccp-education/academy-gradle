pluginManagement {
    // ACADEMY-14 dogfooding — resolve the plugin from its source build, not from
    // a published artefact: no publishToMavenLocal, no signing, and the workflow
    // always exercises the current commit.
    includeBuild("../academy-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
// CNV-16 — JitPack mirror for the education.cccp.build.* conventions
// (bypasses Sonatype / Gradle Plugin Portal). The JitPack tag is pinned while
// the canonical plugin declarations may stay on the released version.
pluginManagement.repositories.maven("https://jitpack.io")
pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("education.cccp.build.")) {
        useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.16")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "academy-dogfood"

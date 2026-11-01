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

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "academy-dogfood"

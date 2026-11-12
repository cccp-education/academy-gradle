@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

val globalProps =
    java.util.Properties().also {
        val globalFile = file(System.getProperty("user.home") + "/.gradle/gradle.properties")
        if (globalFile.exists()) it.load(globalFile.inputStream())
    }

// Publishing credentials are required only to publish (ACADEMY-12-0, D-ACADEMY-12-7):
// a bare host (fresh Windows environment, CI) must be able to configure and test
// the build without ~/.gradle/gradle.properties. When absent, nmcp is left
// unconfigured - publishing fails later with a clear message, never at configuration.
val ossrhUsername = globalProps.getProperty("ossrhUsername")
val ossrhPassword = globalProps.getProperty("ossrhPassword")

if (ossrhUsername != null && ossrhPassword != null) {
    nmcpSettings {
        centralPortal {
            username = ossrhUsername
            password = ossrhPassword
            publishingType = "AUTOMATIC"
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "academy-plugin"

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
    signing
    alias(libs.plugins.plugin.publish)
}

group = "education.cccp"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val functionalTest by sourceSets.creating {
    java.srcDir("src/functionalTest/kotlin")
    resources.srcDir("src/functionalTest/resources")
}

sourceSets.test {
    java.srcDir("src/test/scenarios")
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.kotlin.gradle.plugin)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.java8)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)

    add(functionalTest.implementationConfigurationName, gradleTestKit())
    add(functionalTest.implementationConfigurationName, libs.kotlin.test.junit5)
    add(functionalTest.implementationConfigurationName, libs.junit.jupiter)
    add(functionalTest.runtimeOnlyConfigurationName, libs.junit.platform.launcher)
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTest)
tasks.check { dependsOn(functionalTestTask) }

val cucumberTest = tasks.register<Test>("cucumberTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = configurations.testRuntimeClasspath.get() +
        sourceSets.test.get().output +
        sourceSets.main.get().output +
        files(tasks.jar.get().archiveFile)
    useJUnitPlatform { excludeEngines("junit-jupiter") }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    shouldRunAfter("test")
    failOnNoDiscoveredTests = false
}

tasks.named<Test>("test") {
    filter { excludeTestsMatching("*.scenarios.*") }
}

tasks.check { dependsOn(cucumberTest) }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

gradlePlugin {
    website.set("https://github.com/cccp-education/academy-gradle")
    vcsUrl.set("https://github.com/cccp-education/academy-gradle.git")
    plugins {
        register("academy") {
            id = "education.cccp.academy"
            displayName = "Academy Gradle Plugin"
            description = "Learner experience as a Gradle plugin — Moodle LMS archive (no VPS), opencode agent, BYOK LLM. Single-user build artefact."
            implementationClass = "education.cccp.academy.AcademyPlugin"
            tags = listOf("cccp", "academy", "education", "learning", "lms", "opencode")
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Academy Gradle Plugin")
                description.set("Academy Plugin — consommateur d'expérience pédagogique via Moodle LMS archive, opencode agent et BYOK LLM. Aucun VPS - artefact de build mono-utilisateur.")
                url.set("https://github.com/cccp-education/academy-gradle")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cccp-education")
                        name.set("CCCP Education")
                        email.set("cccp.edu@gmail.com")
                    }
                }
                scm {
                    connection.set("https://github.com/cccp-education/academy-gradle.git")
                    developerConnection.set("https://github.com/cccp-education/academy-gradle.git")
                    url.set("https://github.com/cccp-education/academy-gradle")
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
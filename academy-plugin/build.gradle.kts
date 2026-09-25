plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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

    // ACADEMY-7-1 — N0 formation runtime contracts (ByokLlmConfig, LlmProviderKind).
    // Supersedes D-ACADEMY-5-9 (zero compile dependency): ACADEMY-7 is the moment
    // designated to consume the contract. Transitive cost measured S-010:
    // opencode-session-contracts:0.0.2 + i18n-contracts:0.0.2, both on Central.
    implementation(libs.runtime.contracts)
    // ACADEMY-8-1 — the bridge consumes the N0 opencode session contracts
    // (SessionPrompt/SessionResponse). runtime-contracts declares them as
    // `implementation`, so the bridge must depend on them explicitly.
    implementation(libs.opencode.session.contracts)

    // ACADEMY-8-1 — local HTTP bridge (Ktor 3.2.0, D-ACADEMY-8-2/3).
    // Receiving JSON over HTTP requires real serialization (kotlinx), unlike the
    // literal-concatenation opencode.json renderer (D-ACADEMY-5-3).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.ktor.server.test.host)
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

// ── ACADEMY-1-2 — Dedicated Cucumber runner for academy_installer.feature (pattern S-082) ──
// Scoped to AcademyInstallerCucumberRunner so only the installer feature runs.
val cucumberTestInstaller by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_installer.feature Cucumber suite (ACADEMY-1-2)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyInstallerCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_installer.feature")
    systemProperty("cucumber.filter.tags", "@installer and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestInstaller) }

// ── ACADEMY-5-1 — Dedicated Cucumber runner for academy_opencode.feature (pattern S-082) ──
// Scoped to AcademyOpenCodeCucumberRunner so only the opencode feature runs.
val cucumberTestOpenCode by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_opencode.feature Cucumber suite (ACADEMY-5-1)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyOpenCodeCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_opencode.feature")
    systemProperty("cucumber.filter.tags", "@opencode and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestOpenCode) }

// ── ACADEMY-7-3 — Dedicated Cucumber runner for academy_byok.feature (pattern S-082) ──
// Scoped to AcademyByokCucumberRunner so only the byok feature runs.
val cucumberTestByok by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_byok.feature Cucumber suite (ACADEMY-7-3)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyByokCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_byok.feature")
    systemProperty("cucumber.filter.tags", "@byok and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestByok) }

// ── ACADEMY-8-3 — Dedicated Cucumber runner for academy_bridge.feature (pattern S-082) ──
// Scoped to AcademyBridgeCucumberRunner so only the bridge feature runs.
val cucumberTestBridge by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_bridge.feature Cucumber suite (ACADEMY-8-3)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyBridgeCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_bridge.feature")
    systemProperty("cucumber.filter.tags", "@bridge and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestBridge) }

// ── ACADEMY-4-3 — Dedicated Cucumber runner for academy_material.feature (pattern S-082) ──
// Scoped to AcademyMaterialCucumberRunner so only the material feature runs.
val cucumberTestMaterial by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_material.feature Cucumber suite (ACADEMY-4-3)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyMaterialCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_material.feature")
    systemProperty("cucumber.filter.tags", "@material and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestMaterial) }

// ── ACADEMY-11-3 — Dedicated Cucumber runner for academy_moodle.feature (pattern S-082) ──
// Scoped to AcademyMoodleCucumberRunner so only the moodle feature runs.
val cucumberTestMoodle by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_moodle.feature Cucumber suite (ACADEMY-11-3)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyMoodleCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_moodle.feature")
    systemProperty("cucumber.filter.tags", "@moodle and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestMoodle) }

// ── ACADEMY-12-1 — Dedicated Cucumber runner for academy_env.feature (pattern S-082) ──
// Scoped to AcademyEnvCucumberRunner so only the environment feature runs.
val cucumberTestEnv by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the academy_env.feature Cucumber suite (ACADEMY-12-1)"
    testClassesDirs = sourceSets.getByName("test").output.classesDirs
    classpath = configurations.getByName("testRuntimeClasspath") +
        sourceSets.getByName("test").output +
        sourceSets.getByName("main").output
    useJUnitPlatform {
        excludeEngines("junit-jupiter")
    }
    filter {
        includeTestsMatching("education.cccp.academy.bdd.AcademyEnvCucumberRunner")
    }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    systemProperty("cucumber.features", "src/test/resources/features/academy_env.feature")
    systemProperty("cucumber.filter.tags", "@env and not @wip and not @integration")
    shouldRunAfter(tasks.named("test"))
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(cucumberTestEnv) }

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
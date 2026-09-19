plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("maven-publish")
}

group = "education.cccp"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-javadoc")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("Academy Gradle Plugin")
                description.set("Academy Plugin — consommateur d'expérience pédagogique via Moodle LMS archive, opencode agent et BYOK LLM. Aucun VPS - artefact de build mono-utilisateur.")
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

gradlePlugin {
    plugins {
        create("academy") {
            id = "education.cccp.academy"
            implementationClass = "education.cccp.academy.AcademyPlugin"
            displayName = "Academy Plugin"
            description = """
                Academy Plugin — consommateur d'expérience pédagogique via Moodle LMS archive,
                opencode agent et BYOK LLM. Aucun VPS - artefact de build mono-utilisateur.
            """.trimIndent()
            tags.set(listOf("academy", "education", "learning", "lms", "opencode"))
        }
    }
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

val ktorVersion = "2.3.2"
val log4jVersion = "2.20.0"
val jacksonVersion = "2.15.2"

plugins {
    kotlin("jvm") version "1.8.22"
    application
    id("com.palantir.docker") version "0.35.0"
    id("com.github.ben-manes.versions") version "0.47.0"
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {
    gradleReleaseChannel = "current"
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.lowercase().contains("alpha") ||
                candidate.version.lowercase().contains("beta") ||
                candidate.version.lowercase().contains("rc")
    }
}

group = "at.rueckgr.rocketchat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        resources {
            srcDirs("build/generated/resources")
        }
    }
}

dependencies {
    implementation("org.litote.kmongo:kmongo:4.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.10.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.22")
}

tasks.test {
    useTestNG()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("at.rueckgr.rocketchat.archive.MainKt")
    applicationDefaultJvmArgs = listOf(
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "-Dio.netty.tryReflectionSetAccessible=true",
        "-agentlib:jdwp=transport=dt_socket,server=y,address=*:5005,suspend=n",
    )
}

distributions {
    main {
        version = "latest"
    }
}

docker {
    name = "${project.name}:latest"
    files("build/distributions")
}

tasks.dockerPrepare {
    dependsOn(tasks.build)
}

tasks.create("createVersionFile") {
    doLast {
        val file = File("build/generated/resources/git-revision")
        project.mkdir(file.parentFile.path)
        file.delete()

        file.appendText(String.format("revision = %s\n", runGit("git", "rev-parse", "--short", "HEAD")))
        file.appendText(String.format("commitMessage = %s\n", runGit("git", "log", "-1", "--pretty=%B")))
    }
}

fun runGit(vararg args: String): String {
    val outputStream = ByteArrayOutputStream()
    project.exec {
        commandLine(*args)
        standardOutput = outputStream
    }
    return outputStream.toString().split("\n")[0].trim()
}

tasks.processResources {
    dependsOn("createVersionFile")
}

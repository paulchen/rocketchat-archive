import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files

val ktorVersion = "3.2.0"
val log4jVersion = "2.25.0"
val jacksonVersion = "2.19.1"
val kotlinVersion = "2.1.21"

plugins {
    kotlin("jvm") version "2.1.21"
    application
    id("com.github.ben-manes.versions") version "0.52.0"
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
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("org.mongodb:mongodb-driver-kotlin-sync:5.5.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

tasks.test {
    useTestNG()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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

tasks.register("createVersionFile") {
    doLast {
        val file = file("build/generated/resources/git-revision")
        Files.createDirectories(file.parentFile.toPath())
        Files.deleteIfExists(file.toPath())

        file.appendText(String.format("revision = %s\n", runGit("git", "rev-parse", "--short", "HEAD")))
        file.appendText(String.format("commitMessage = %s\n", runGit("git", "log", "-1", "--pretty=%B")))
    }
}

fun runGit(vararg args: String) =
    project
        .providers.exec {
            commandLine(*args)
        }
        .standardOutput.asText.get()
        .split("\n")[0].trim()

tasks.processResources {
    dependsOn("createVersionFile")
}

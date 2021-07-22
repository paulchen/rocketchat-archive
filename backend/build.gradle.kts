import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version = "1.6.1"

plugins {
    kotlin("jvm") version "1.5.21"
    application
    id("com.palantir.docker") version "0.26.0"
}

group = "at.rueckgr.rocketchat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.litote.kmongo:kmongo:4.2.8")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")

    testImplementation(kotlin("test"))
}

tasks.test {
    useTestNG()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "at.rueckgr.rocketchat.archive.MainKt"
}

distributions {
    main {
        version = ""
    }
}

docker {
    name = "${project.name}:latest"
    files("build/distributions")
}

tasks.docker {
    dependsOn(tasks.distTar)
}
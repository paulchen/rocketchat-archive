import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
}

group = "at.rueckgr.rocketchat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.litote.kmongo:kmongo:4.2.8")

    testImplementation(kotlin("test"))
}

tasks.test {
    useTestNG()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

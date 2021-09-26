import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version = "1.6.1"
val log4jVersion = "2.14.1"

plugins {
    kotlin("jvm") version "1.5.21"
    application
    id("com.palantir.docker") version "0.26.0"
    id("com.palantir.git-version") version "0.12.3"
}

group = "at.rueckgr.rocketchat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources", "build/generated/resources")
        }
    }
}

dependencies {
    implementation("org.litote.kmongo:kmongo:4.2.8")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useTestNG()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("at.rueckgr.rocketchat.archive.MainKt")
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

tasks.dockerPrepare {
    dependsOn(tasks.build)
}

tasks.create("createVersionFile") {
    doLast {
        val gitVersion: groovy.lang.Closure<String> by project.extra
        val file = File("build/generated/resources/git-revision")
        project.mkdir(file.parentFile.path)
        file.delete()
        file.appendText(gitVersion())
    }
}

tasks.processResources {
    dependsOn("createVersionFile")
}

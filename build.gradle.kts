plugins {
    kotlin("jvm") version "1.5.10"
}

group = "io.github.emotionbug"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2")
}
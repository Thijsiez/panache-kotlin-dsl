plugins {
    kotlin("jvm")
}

group = "ch.icken"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val quarkusVersion: String by project
    val kspVersion: String by project
    val kotlinPoetVersion: String by project

    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")
    implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    testImplementation(kotlin("test"))
    testImplementation("io.quarkus:quarkus-junit5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
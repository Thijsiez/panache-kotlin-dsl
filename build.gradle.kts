/*
 * Copyright 2023-2024 Thijs Koppen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
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
    val mockkVersion: String by project
    val compileTestingVersion: String by project

    implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:$compileTestingVersion")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$compileTestingVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    jvmToolchain(17)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

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
    id("org.sonarqube")
}

dependencies {
    kover(project(":examples"))
    kover(project(":library"))
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    group = "ch.icken"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

configure(subprojects) {
    dependencies {
        val quarkusVersion: String by project
        val mockkVersion: String by project

        implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
        implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
        testImplementation("io.quarkus:quarkus-junit5")
        testImplementation("io.mockk:mockk:$mockkVersion")
    }

    tasks.test {
        useJUnitPlatform()
    }

    kotlin {
        jvmToolchain(17)
    }
}

tasks.test {
    useJUnitPlatform()
}

sonar {
    properties {
        property("sonar.projectKey", "Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6")
        property("sonar.projectName", "panache-kotlin-dsl")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/kover/report.xml")
    }
}

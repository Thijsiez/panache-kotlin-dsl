/*
 * Copyright 2023-2025 Thijs Koppen
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
    id("com.google.devtools.ksp")
    id("io.quarkus")
    id("org.jetbrains.kotlinx.kover")
    id("org.sonarqube")
}

repositories {
    mavenCentral()
}

dependencies {
    val quarkusVersion: String by project
    implementation(platform("io.quarkus:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-jdbc-h2")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation(project(":panache-kotlin-dsl"))
    ksp(project(":panache-kotlin-dsl"))

    testImplementation("io.quarkus:quarkus-junit5")
    kover(project(":panache-kotlin-dsl"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        javaParameters.set(true)
    }
    jvmToolchain(17)
}
allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.ws.rs.Path")
    annotation("io.quarkus.test.junit.QuarkusTest")
}
ksp {
    arg("addGeneratedAnnotation", "true")
}
kover {
    reports {
        filters {
            //Exclude non-library classes/packages
            excludes {
                packages("ch.icken.model")
            }
        }
    }
}
sonar {
    properties {
        property("sonar.projectKey", "Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6")
        property("sonar.projectName", "panache-kotlin-dsl")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/kover/report.xml")
        //Exclude non-library classes/packages
        property("sonar.coverage.exclusions", "**/ch/icken/model/*")
        property("sonar.exclusions", "**/ch/icken/model/*")
    }
}

//Basic Quarkus Gradle setup
tasks {
    test {
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        useJUnitPlatform()
    }
}

//Disable Quarkus native builds triggered by Kover
tasks.quarkusAppPartsBuild {
    isEnabled = false
}

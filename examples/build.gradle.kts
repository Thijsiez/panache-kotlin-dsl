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
    id("io.quarkus")
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlinx.kover")
    id("org.sonarqube")
}

group = "ch.icken"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val quarkusVersion: String by project

    implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
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
    jvmToolchain(17)
}
allOpen {
    annotation("jakarta.persistence.Entity")
}
ksp {
    arg("addGeneratedAnnotation", "true")
}
kover {
    reports {
        filters {
            //Exclude non-library coverage
            excludes {
                classes("ch.icken.ExamplesKt")
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
    }
}

//Basic Quarkus Gradle setup
tasks {
    test {
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        useJUnitPlatform()
    }
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.encoding = "UTF-8"
    }
}

//Fixes issue with task execution order
tasks.compileKotlin {
    dependsOn(tasks.compileQuarkusGeneratedSourcesJava)
}
tasks.configureEach {
    if (name == "kspKotlin") {
        dependsOn(tasks.compileQuarkusGeneratedSourcesJava)
    }
}

//Fixes issue with circular task dependency,
//see https://github.com/quarkusio/quarkus/issues/29698#issuecomment-1671861607
project.afterEvaluate {
    getTasksByName("quarkusGenerateCode", true).forEach { task ->
        task.setDependsOn(task.dependsOn.filterIsInstance<Provider<Task>>()
            .filterNot { it.get().name == "processResources" })
    }
    getTasksByName("quarkusGenerateCodeDev", true).forEach { task ->
        task.setDependsOn(task.dependsOn.filterIsInstance<Provider<Task>>()
            .filterNot { it.get().name == "processResources" })
    }
}

//Disable Quarkus native builds triggered by Kover
tasks.quarkusAppPartsBuild {
    isEnabled = false
}

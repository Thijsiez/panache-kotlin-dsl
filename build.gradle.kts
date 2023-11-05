/*
 * Copyright 2023 Thijs Koppen
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
    jacoco
    `jvm-test-suite`
    `jacoco-report-aggregation`
    id("org.sonarqube")
}

dependencies {
    implementation(project(":library"))
    implementation(project(":examples"))
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "jacoco")

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
        finalizedBy(tasks.jacocoTestReport)
    }

    kotlin {
        jvmToolchain(11)
    }
}

tasks.testCodeCoverageReport {
    dependsOn(project(":library").tasks.jacocoTestReport)
    dependsOn(project(":examples").tasks.jacocoTestReport)
}
tasks.test {
    finalizedBy(tasks.testCodeCoverageReport)
}

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

sonar {
    properties {
        property("sonar.projectKey", "Thijsiez_panache-kotlin-dsl_AYsggGcmXmm3_FAoLWCF")
        property("sonar.projectName", "panache-kotlin-dsl")
    }
}

/*
 * Copyright 2023-2026 Thijs Koppen
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

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        id("com.google.devtools.ksp") version providers.gradleProperty("kspVersion").get()
        id("io.quarkus") version providers.gradleProperty("quarkusVersion").get()
        id("org.jetbrains.dokka") version providers.gradleProperty("dokkaVersion").get()
        id("org.jetbrains.kotlinx.kover") version providers.gradleProperty("koverVersion").get()
        id("org.sonarqube") version providers.gradleProperty("sonarqubeVersion").get()
        id("com.vanniktech.maven.publish") version providers.gradleProperty("mavenPublishVersion").get()
    }

    include(":panache-kotlin-dsl")
    project(":panache-kotlin-dsl").projectDir = file("..")
}

rootProject.name = "tests"

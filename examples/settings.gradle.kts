/*
 * Copyright 2024 Thijs Koppen
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
        val quarkusVersion: String by settings
        val kotlinVersion: String by settings
        val kspVersion: String by settings
        val koverVersion: String by settings
        val sonarqubeVersion: String by settings
        val mavenPublishVersion: String by settings

        id("io.quarkus") version quarkusVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        id("com.google.devtools.ksp") version kspVersion
        id("org.jetbrains.kotlinx.kover") version koverVersion
        id("org.sonarqube") version sonarqubeVersion
        id("com.vanniktech.maven.publish") version mavenPublishVersion
    }

    include(":panache-kotlin-dsl")
    project(":panache-kotlin-dsl").projectDir = file("..")
}

rootProject.name = "examples"

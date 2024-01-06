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
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":library"))
    ksp(project(":library"))
    implementation("io.quarkus:quarkus-jdbc-h2")
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

ksp {
    arg("addGeneratedAnnotation", "true")
}

tasks.compileKotlin {
    dependsOn(tasks.compileQuarkusGeneratedSourcesJava)
}
tasks.configureEach {
    if (name == "kspKotlin") {
        dependsOn(tasks.compileQuarkusGeneratedSourcesJava)
    }
}

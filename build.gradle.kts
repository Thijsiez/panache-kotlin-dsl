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

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish")
}

val groupId: String by project
setGroup(groupId)
val version: String by project
setVersion(version)

repositories {
    mavenCentral()
}

dependencies {
    val quarkusVersion: String by project
    compileOnly("io.quarkus:quarkus-hibernate-orm-panache-kotlin:$quarkusVersion")

    val kspVersion: String by project
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    val kotlinPoetVersion: String by project
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

    testImplementation(kotlin("reflect"))
    testImplementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin:${quarkusVersion}")
    val jupiterVersion: String by project
    testImplementation(platform("org.junit:junit-bom:$jupiterVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    val mockkVersion: String by project
    testImplementation("io.mockk:mockk:$mockkVersion")
    val compileTestingVersion: String by project
    testImplementation("dev.zacsweers.kctfork:ksp:$compileTestingVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    jvmToolchain(17)
}
mavenPublishing {
    coordinates(groupId, rootProject.name, version)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

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
    val kspVersion: String by project
    val kotlinPoetVersion: String by project
    val mockkVersion: String by project
    val compileTestingVersion: String by project

    implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")

    testImplementation("io.quarkus:quarkus-junit5") {
        //Because of https://osv.dev/vulnerability/GHSA-hfq9-hggm-c56q, unused anyway
        exclude(group = "com.thoughtworks.xstream", module = "xstream")
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("dev.zacsweers.kctfork:ksp:$compileTestingVersion")
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

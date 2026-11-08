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

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish")
}

val groupId = project.property("groupId") as String
setGroup(groupId)
val version = project.property("version") as String
setVersion(version)

repositories {
    mavenCentral()
}

dependencies {
    val quarkusVersion = project.property("quarkusVersion") as String
    compileOnly("io.quarkus:quarkus-hibernate-orm-panache-kotlin:$quarkusVersion")

    implementation("com.google.devtools.ksp:symbol-processing-api:${project.property("kspVersion") as String}")
    implementation("com.squareup:kotlinpoet-ksp:${project.property("kotlinPoetVersion") as String}")

    testImplementation(kotlin("reflect"))
    testImplementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin:$quarkusVersion")
    testImplementation(platform("org.junit:junit-bom:${project.property("jupiterVersion") as String}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:${project.property("mockkVersion") as String}")
    testImplementation("dev.zacsweers.kctfork:ksp:${project.property("compileTestingVersion") as String}")

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

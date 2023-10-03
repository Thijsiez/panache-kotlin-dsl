pluginManagement {
    val quarkusVersion: String by settings
    val kotlinVersion: String by settings
    val kspVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("io.quarkus") version quarkusVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        id("com.google.devtools.ksp") version kspVersion
    }
}

rootProject.name = "panache-kotlin-dsl"

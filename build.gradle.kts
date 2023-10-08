plugins {
    kotlin("jvm")
}

allprojects {
    apply(plugin = "kotlin")

    group = "ch.icken"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

configure(subprojects) {
    dependencies {
        val quarkusVersion: String by project

        implementation(kotlin("stdlib"))
        implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
        testImplementation(kotlin("test"))
    }

    //TODO
//    kotlin {
//        jvmToolchain(11)
//    }
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

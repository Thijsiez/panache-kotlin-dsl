plugins {
    id("io.quarkus")
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation(project(":library"))
    ksp(project(":library"))
    testImplementation("io.quarkus:quarkus-junit5")
}

tasks.test {
    useJUnitPlatform()
}

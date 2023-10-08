dependencies {
    val kspVersion: String by project
    val kotlinPoetVersion: String by project

    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}

[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Maven Central](https://img.shields.io/badge/maven_central-0.0.3-brightgreen.svg)](https://search.maven.org/artifact/ch.icken/panache-kotlin-dsl/0.0.3/jar)
[![Test](https://github.com/Thijsiez/panache-kotlin-dsl/actions/workflows/test.yaml/badge.svg?branch=main)](https://github.com/Thijsiez/panache-kotlin-dsl/actions/workflows/test.yaml)  
![Quality Gate Status](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=alert_status&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)
![Reliability Rating](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=reliability_rating&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)
![Maintainability Rating](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=sqale_rating&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)
![Security Rating](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=security_rating&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)  
![Coverage](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=coverage&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)
![Technical Debt](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=sqale_index&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)
![Duplicated Lines (%)](https://sonarqube.icken.ch/api/project_badges/measure?project=Thijsiez_panache-kotlin-dsl_760170ef-68c7-43b0-880d-cf1034afe3c6&metric=duplicated_lines_density&token=sqb_cfafb36de6f18194da2e383324ee281b0de5b953)

# panache-kotlin-dsl
A dynamic, type-safe way to write your queries

[Changelog](CHANGELOG.md) | [Contributing](CONTRIBUTING.md)

## Examples
[//]: # (TODO add examples)

## Getting Started
<details open>
<summary><b>Gradle Kotlin DSL</b></summary>

Add the KSP Gradle plugin to your build file
```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.26"
}
```
Add this library to your build file and register it with KSP
```kotlin
dependencies {
  implementation("ch.icken:panache-kotlin-dsl:0.0.3")
  ksp("ch.icken:panache-kotlin-dsl:0.0.3")
}
```
Optionally configure the behavior
```kotlin
ksp {
  arg("addGeneratedAnnotation", "true")
}
```
Important: fix Gradle task dependency issues when combining Quarkus and KSP
```kotlin
//Fixes issue with task execution order
tasks.compileKotlin {
  dependsOn(tasks.compileQuarkusGeneratedSourcesJava)
}
tasks.configureEach {
  if (name == "kspKotlin") {
    dependsOn(tasks.compileQuarkusGeneratedSourcesJava)
  }
}

//Fixes issue with circular task dependency,
//see https://github.com/quarkusio/quarkus/issues/29698#issuecomment-1671861607
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
```
</details>

[//]: # (TODO add Gradle Groovy DSL)  
[//]: # (TODO add Apache Maven)

## Requirements
- Quarkus version `3.9.2` or newer
  - Dependency `io.quarkus:quarkus-hibernate-orm-panache-kotlin` is required
- Kotlin version `1.9.23` or newer
- KSP version `1.9.23-1.0.20` or newer
  - Your KSP version needs to match your Kotlin version. This is a strict requirement!  
    For example, when your Kotlin version is `2.0.21`, your KSP version needs to be built for and start with the same version, such as `2.0.21-1.0.25`

## Features
### Queries
- Supports the following expressions in a type-safe and null-safe way for all columns
  - `eq`, `neq`, `lt`, `gt`, `lte`, `gte`, `in`, `notIn`, `between`, and `notBetween`
- Supports `like` and `notLike` expressions in a null-safe way for String columns
- Supports `and` and `or` expressions for building up a query
- Adds the `PanacheSingleResult` sealed class and `singleResultSafe()` extension function to return a single result without throwing exceptions.
  - Allows you to handle no/multiple results with a `when (result) { ... }` block instead of try-catching
### Code Generation
- Generate `Column`s for non-transient and non-mapped fields in Panache entities
- Generate query entry point extension functions for entities with Panache companion objects
  - `where` to start building a SELECT/DELETE queries, which may be chained to other Panache functions
  - `update` with setters to bulk-update multiple rows at once
  - Single expression `updateAll` to update all rows without requiring a WHERE clause
  - Single expression `count`, `delete`, `find`, `stream`, `single`, `singleSafe`, and `multiple`
- Allows for overriding the generated `Column`'s type parameter using `@ColumnType`
  - Especially useful when using a JPA `@Converter` when the field's type is different to the column's type
- Optionally annotate generated code with `@Generated` so it can be excluded from test coverage reporting

## Known Issues
- Code generation does not handle fields of generic types (e.g. `List<E>`, `Set<E>`, etc.), but as far as I'm aware this is difficult to get working with Hibernate anyway.
Consider using `@ColumnType` from this library in combination with a JPA `@Converter` on such a field.

## Planned Features
These features will be added some time in the future. Please do submit an issue if you'd like these sooner rather than later :)
- DSL for sorting expressions

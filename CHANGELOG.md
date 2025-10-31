# Changelog

## 0.1.1
- Remove Quarkus runtime dependencies to not leak them transitively 

## 0.1.0
- Change minimum Quarkus version to `3.24.0`
  - Quarkus 3.24 was a major change by upgrading to Hibernate ORM 7
- Change minimum Kotlin version to `2.1.21`
- Change minimum KSP version to `2.1.21-2.0.1` to fully support KSP2
  - **Note:** This also means support for KSP1 is now dropped

## 0.0.8
- Correctly resolve `@JoinColumn` property type's package

## 0.0.7
- Find entity ID type based on PanacheCompanion\[Base\] declaration instead of `@Id` annotation

## 0.0.6
- Change `@JoinColumn` generated Column properties to use getter instead of value

## 0.0.5
- Remove inline modifier on generated extension functions so they can be mocked

## 0.0.4
- Add DSL for bulk updates
- Add printing of the Panache query at DEBUG log level
  - **Note:** output does not follow a specific format like SQL, JPQL, or HQL, and should only be used in debugging  
    Use Hibernate's SQL logging functionality to see what is actually being queried
- Add basic documentation for exposed API

## 0.0.3
- Remove `whereGroup`, `andGroup`, and `orGroup` functions in favor of expression chaining

## 0.0.2
- Add the `@ColumnType` annotation to override the generated `Column`'s type parameter

## 0.0.1
- Initial release

# Changelog

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

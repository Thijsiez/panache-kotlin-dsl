# panache-kotlin-dsl Tests
This projects contains integration tests for panache-kotlin-dsl.
These have been pulled into a separate project so KSP can be run like it would be by an implementing project,
and thus produces representative test results.  
This also helps in making sure the API provided by panache-kotlin-dsl does not regress,
and that the main project is not cluttered up with code pertaining to code quality scans etc.

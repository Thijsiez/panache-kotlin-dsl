# Release Checklist
- Update `version` in gradle.properties to match this release's tag
- Upgrade all unpinned dependencies in both the library and tests project's gradle.properties
- Make sure all tests are passing so we can be fairly sure there are no regressions
- Make sure the versions in the README's Requirements section match the actual dependencies
- Add a new section to the CHANGELOG describing what has changed
- Update the versions in the README's Getting Started section and the Maven Central badge
- Finally, add a tag with the new version, which will then be built and released

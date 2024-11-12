# Release Checklist
- Make sure all tests are passing so we can be fairly sure there are no regressions
- Update `version` in gradle.properties to match this release's tag
- Upgrade all unpinned dependencies in both the library and tests project's gradle.properties
- Make sure the versions in the README's Requirements section match the actual dependencies
- Add a new section to the CHANGELOG describing what has changed
- Update the versions in the README's Getting Started section
- Finally, add a tag with the new version, which will then be built and released

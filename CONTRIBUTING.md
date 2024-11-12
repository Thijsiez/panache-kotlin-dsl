# Release Checklist
- Run all tests to make sure there are no regressions
- Update the `version` in gradle.properties to match this release's tag
- Update all unpinned dependencies in both the library and tests project's gradle.properties
- Make sure the versions in the README's Requirements section match the used dependency versions
- Add a new section to the CHANGELOG describing what has changed
- Update the versions in the README's Getting Started section
- Add a tag with the new version to the commit which will be built and released

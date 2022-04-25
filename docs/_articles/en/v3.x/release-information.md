---
title: Release Information
lang: en
---

### To update the plugin, please follow the linked [instructions](./en/v3.x/getting-started/install/#upgrade-plug-in)

## [v3.0.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.0.0) (04-26-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.13.1...v3.0.0)

### Release Summary

* RetireJS is now included with the default engines
* Removed deprecated flag `--violations-cause-error` on `scanner:run`
* Updated PMD to 6.44.0
* Updated Eslint to 6.8.0
* Updated RetireJS to 2.2.5
* Updated Typescript to 4.6.2
* Fixed linting issues related to upgrades


**Closed issues:**

- \[BUG\] sfdx-scanner install/update fail - broken URL [\#613](https://github.com/forcedotcom/sfdx-scanner/issues/613)
- \[FEATURE REQUEST\]Support Scan Flows  [\#558](https://github.com/forcedotcom/sfdx-scanner/issues/558)

**Merged pull requests:**

- @W-11032595@ \[3.x\] Added salesforce-cli context to release pipeline [\#641](https://github.com/forcedotcom/sfdx-scanner/pull/641)
- @W-10998410@: Added backwards-compatibility for configs. [\#633](https://github.com/forcedotcom/sfdx-scanner/pull/633)
- @W-10909748@: RetireJS now properly counts as implicitly requested. [\#632](https://github.com/forcedotcom/sfdx-scanner/pull/632)
- @W-10977766@: Updated PMD to v6.44.0 [\#628](https://github.com/forcedotcom/sfdx-scanner/pull/628)
- @W-10909748@: RetireJS is now enabled by default. [\#626](https://github.com/forcedotcom/sfdx-scanner/pull/626)
- @W-10459712@: Final wave of linting fixes [\#624](https://github.com/forcedotcom/sfdx-scanner/pull/624)
- @W-9648595@: Updated dependencies [\#623](https://github.com/forcedotcom/sfdx-scanner/pull/623)
- @W-10459712@: Fifth wave of linting fixes. [\#621](https://github.com/forcedotcom/sfdx-scanner/pull/621)
- @W-10459712@: Fourth wave of linting fixes. [\#620](https://github.com/forcedotcom/sfdx-scanner/pull/620)
- @W-10459712@: Third wave of linting fixes. [\#619](https://github.com/forcedotcom/sfdx-scanner/pull/619)
- @W-10459712@: Second wave of linting fixes. [\#618](https://github.com/forcedotcom/sfdx-scanner/pull/618)
- @W-10459712@: First set of linting fixes. [\#616](https://github.com/forcedotcom/sfdx-scanner/pull/616)
- @W-10459712@: Fixed configuration errors with ESLint [\#615](https://github.com/forcedotcom/sfdx-scanner/pull/615)
- @W-10459713@: Remove deprecated/unimplemented flags [\#605](https://github.com/forcedotcom/sfdx-scanner/pull/605)
- @W-10771486@: Updated retirejs vulnerability catalog to capture Feb-Mar 2022 changes. [\#604](https://github.com/forcedotcom/sfdx-scanner/pull/604)
- @W-10459712@: Upgraded ECMAScript engines to latest versions [\#600](https://github.com/forcedotcom/sfdx-scanner/pull/600)
- @W-10782883@: Updating package.json to 3.0.0 [\#598](https://github.com/forcedotcom/sfdx-scanner/pull/598)
- @W-10777806@: Bump nokogiri from 1.12.5 to 1.13.3 in /docs [\#597](https://github.com/forcedotcom/sfdx-scanner/pull/597)
- @W-10769365@: Updated Typescript from 3.8.2 to 4.6.2 [\#596](https://github.com/forcedotcom/sfdx-scanner/pull/596)
- @W-10459699@: PR verification github action now expects a version indicator for PRs intended to be merged to dev.  [\#595](https://github.com/forcedotcom/sfdx-scanner/pull/595)
- @W-10459699@: Added smoke test execution against dev-3 pilot branch [\#591](https://github.com/forcedotcom/sfdx-scanner/pull/591)



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
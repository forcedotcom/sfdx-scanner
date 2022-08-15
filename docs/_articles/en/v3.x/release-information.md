---
title: Release Information
lang: en
---

### To update the plugin, please follow the linked [instructions](./en/v3.x/getting-started/install/#upgrade-plug-in)


## [v3.4.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.4.0) (08-17-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.3.0...v3.4.0)

### Release Summary
* Improvements to `eslint`:
	- Execute `eslint` engine with babel parser to accommodate more modern javascript syntax
	- Identify javascript target files that were analyzed by both `eslint` and `eslint-lwc`, and throw warning about the possibility of duplicate violations
* Survey request banner fix
* SFGE: Minor code fixes
* Updates to local RetireJS Vulnerability Repository
* PMD upgrade to 6.48.0

**Closed issues:**

- \[BUG\] UnexpectedException on v3 DFA on large ISV codebase [\#739](https://github.com/forcedotcom/sfdx-scanner/issues/739)
- \[BUG\] stdout not in proper JSON format when --format json is used [\#771](https://github.com/forcedotcom/sfdx-scanner/issues/771)
- apex.jorje.parser.impl.BaseApexLexer dedupe [\#779](https://github.com/forcedotcom/sfdx-scanner/issues/779)
- \[BUG\] adding custom rule [\#788](https://github.com/forcedotcom/sfdx-scanner/issues/788)

**Merged pull requests:**

- @W-11446174@: changed eslint parser to @babel/eslint-parser [\#767](https://github.com/forcedotcom/sfdx-scanner/pull/767)
- @W-10778096@ Handle unresolvable parameters passed to database operations [\#768](https://github.com/forcedotcom/sfdx-scanner/pull/768)
- @W-10778096@ Minor error message fixes [\#770](https://github.com/forcedotcom/sfdx-scanner/pull/770)
- @W-11494080@ Fix survey request formatting issue [\#773](https://github.com/forcedotcom/sfdx-scanner/pull/773)
- @W-11476976@ Don't block stripInaccessible() on custom value [\#780](https://github.com/forcedotcom/sfdx-scanner/pull/780)
- @W-11446192@: checks if any files are processed by both eslint and eslint-lwc and emits warning [\#782](https://github.com/forcedotcom/sfdx-scanner/pull/782)
- @W-11505293@ Skip info logs from dependency [\#784](https://github.com/forcedotcom/sfdx-scanner/pull/784)
- @W-10778080@ Handle unresolved apex value in DML statements gracefully [\#785](https://github.com/forcedotcom/sfdx-scanner/pull/785)
- @W-11567651@: Updating PMD to v6.48.0. [\#794](https://github.com/forcedotcom/sfdx-scanner/pull/794)


## [v3.3.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.3.0) (07-20-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.2.0...v3.3.0)

### Release Summary
* SFGE: Added new entry points for CRUD/FLS rule:
	- Methods annotated with `@InvocableMethod`
	- Methods annotated with `@NamespaceAccessible`
	- Methods annotated with `@RemoteAction`
	- Methods with `global` scope on any class
	- `handleInboundEmail()` method on implementations of `Messaging.InboundEmailHandler`
* SFGE: Ability to recognize static code blocks in Apex
* SFGE: Return intelligible error to users when the engine encounters unreachable code that blocks the code flow
* Upgraded PMD to 6.47.0
* New banner with survey link to get feedback from users

**Merged pull requests:**

- @W-11179348@: optimizing windows-unit-tests runtime [\#716](https://github.com/forcedotcom/sfdx-scanner/pull/716)
- @W-10459675@: Part 1 of several. Added NamespaceAccessible-annotated methods as sources. [\#735](https://github.com/forcedotcom/sfdx-scanner/pull/735)
- @W-10459675@: Added support for RemoteAction methods. [\#736](https://github.com/forcedotcom/sfdx-scanner/pull/736)
- @W-10459675@: Part 3 of several. Added global methods as sources. [\#737](https://github.com/forcedotcom/sfdx-scanner/pull/737)
- @W-11261233@ Handle static blocks in classes [\#740](https://github.com/forcedotcom/sfdx-scanner/pull/740)
- @W-11261233@ SFGE: adding ability to programmatically enable/disable rules [\#741](https://github.com/forcedotcom/sfdx-scanner/pull/741)
- @W-10459675@: Part 4 of several. Added Messaging.InboundEmailHandler implementations as sources. [\#742](https://github.com/forcedotcom/sfdx-scanner/pull/742)
- @W-10459675@: Part 5 of 5. Added InvocableMethod-annotated methods as sources. [\#743](https://github.com/forcedotcom/sfdx-scanner/pull/743)
- @W-10459675@: Renamed InheritanceEdgeBuilder to reflect its broadened behavior. [\#744](https://github.com/forcedotcom/sfdx-scanner/pull/744)
- @W-11404189@: Added VIRTUAL and OVERRIDE keywords to graph. [\#746](https://github.com/forcedotcom/sfdx-scanner/pull/746)
- @W-11404189@ Adding error message for unreachable code [\#747](https://github.com/forcedotcom/sfdx-scanner/pull/747)
- @W-11267443@ Upgrading to PMD 6.47.0 [\#749](https://github.com/forcedotcom/sfdx-scanner/pull/749)
- @W-11397711@: Added banner requesting that people take our survey. [\#753](https://github.com/forcedotcom/sfdx-scanner/pull/753)
- @W-11445992@: Update version number in preparation for 3.3.0 release. [\#754](https://github.com/forcedotcom/sfdx-scanner/pull/754)
- @W-11397711@: Updated text to match feedback from PR. [\#757](https://github.com/forcedotcom/sfdx-scanner/pull/757)


## [v3.2.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.2.0) (06-22-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.1.2...v3.2.0)

### Release Summary
* Support for method-level targeting while executing `scanner:run:dfa` command
* SFGE: Adding engine directive type `sfge-disable-stack` to annotate a stack of code paths
* New `--verbose-violations` flag on `scanner:run` command to fetch more informative violations from retire-js engine
* Update RetireJS vulnerabilities

**Closed issues:**

- \[Feature Request\] Support retire-js verbose output [\#560](https://github.com/forcedotcom/sfdx-scanner/issues/560)

**Merged pull requests:**

- @W-10759090@: Implemented method-level targeting for SFGE, and message-passing system to allow for proper logging. [\#710](https://github.com/forcedotcom/sfdx-scanner/pull/710)
- @W-10127077@: adds --verbose-violations flag and functionality [\#712](https://github.com/forcedotcom/sfdx-scanner/pull/712)
- @W-11120894@: Added proper cloning for EngineDirective nodes. [\#715](https://github.com/forcedotcom/sfdx-scanner/pull/715)
- @W-11281093@ sfge and pmd-cataloger should be available as independent projects [\#720](https://github.com/forcedotcom/sfdx-scanner/pull/720)
- @W-10759090@: Resolves problems identified in code review [\#721](https://github.com/forcedotcom/sfdx-scanner/pull/721)
- @W-11267235@: violation messages use a semicolon instead of a line break in json format [\#723](https://github.com/forcedotcom/sfdx-scanner/pull/723)
- @W-11267130@: violation messages use <br> instead of \n in html [\#724](https://github.com/forcedotcom/sfdx-scanner/pull/724)
- @W-11321290@ RetireJs updates + package number update to 3.2.0 [\#727](https://github.com/forcedotcom/sfdx-scanner/pull/727)


## [v3.1.2](https://github.com/forcedotcom/sfdx-scanner/tree/v3.1.2) (05-25-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.1.1...v3.1.2)

### Release Summary
* Fixed minor HTML format issues
* Allow `--outfile` to accept extensions in mixed case
* Update RetireJS vulnerabilities

**Closed issues:**

- \[BUG\] RetireJS can not be found [\#672](https://github.com/forcedotcom/sfdx-scanner/issues/672)

**Merged pull requests:**

- @W-11157448@: RetireJS now runs using the same node process that is currently running [\#682](https://github.com/forcedotcom/sfdx-scanner/pull/682)
- @W-11150817@: Changed HTML templates to use Code Analyzer instead of CLI Scanner [\#684](https://github.com/forcedotcom/sfdx-scanner/pull/684)
- @W-11149653@: Fixed HTML output formatting error. [\#683](https://github.com/forcedotcom/sfdx-scanner/pull/683)
- @W-11179316@ Updating retirejs JSON on dev-3 [\#698](https://github.com/forcedotcom/sfdx-scanner/pull/698)
- @W-11149717@: All cases accepted for file extension when using --outfile [\#696](https://github.com/forcedotcom/sfdx-scanner/pull/696)



## [v3.1.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.1.0) (05-18-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.0.3...v3.1.0)

### Release Summary
* Added Salesforce Graph Engine (SFGE) as a new backend engine
* Added `scanner:run:dfa` command to execute dfa-based rules
* Updated PMD version to 6.45.0
* Updated RetireJS vulnerabilities

**Merged pull requests:**

- @W-11056765@: Updated retire-js vulnerabilities. [\#654](https://github.com/forcedotcom/sfdx-scanner/pull/654)
- @W-11056765@: Updated retire-js vulnerabilities. [\#658](https://github.com/forcedotcom/sfdx-scanner/pull/658)
- @W-11099120@ Applying dependabot changes to 3.x [\#663](https://github.com/forcedotcom/sfdx-scanner/pull/663)
- @W-11100262@: Fixing failing smoke tests. [\#667](https://github.com/forcedotcom/sfdx-scanner/pull/667)
- @W-11099120@ Applying nokogiri change to dev-3 [\#668](https://github.com/forcedotcom/sfdx-scanner/pull/668)
- @W-11103167@: Updating PMD to 6.45.0. [\#670](https://github.com/forcedotcom/sfdx-scanner/pull/670)
- @W-11119708@ Adding SFG Engine [\#673](https://github.com/forcedotcom/sfdx-scanner/pull/673)
- @W-10909739@: Fixed problem with SFGE_IGNORE_PARSE_ERRORS env-var. [\#674](https://github.com/forcedotcom/sfdx-scanner/pull/674)
- @W-11119708@: Added files for sample documentation. [\#675](https://github.com/forcedotcom/sfdx-scanner/pull/675)
- @W-10459665@ Cleanup related to doc changes [\#678](https://github.com/forcedotcom/sfdx-scanner/pull/678)
- @W-11111352@: SARIF and Table format now properly format output in the event of SFGE errors. [\#679](https://github.com/forcedotcom/sfdx-scanner/pull/679)
- @W-11151793@: Changed logging level in tests from debug to info. [\#680](https://github.com/forcedotcom/sfdx-scanner/pull/680)
- @W-11163136@ Updating package version [\#686](https://github.com/forcedotcom/sfdx-scanner/pull/686)


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

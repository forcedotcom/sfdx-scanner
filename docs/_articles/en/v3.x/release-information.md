---
title: Release Information
lang: en
redirect_from: /en/release-information
---

Here are the new and changed features in recent updates of Salesforce Code Analyzer (Code Analyzer).

We publish the latest Code Analyzer monthly. 

* Run `sfdx plugin` to display the version of Code Analyzer installed on your computer. 
* Run `sfdx plugins:update` and `sfdx plugins:update --help` to update Code Analyzer and help to the latest version.
* Follow these [instructions](./en/v3.x/getting-started/install/#upgrade-plug-in) to update Code Analyzer

## [v3.11.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.11.0) (03-29-2023)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.11.0...v3.10.0)

### Release Summary

* NEW (CodeAnalyzer): We updated the PMD engine to version 6.55.0.
* NEW (CodeAnalyzer): We made some updates to the RetireJS vulnerability database.
* NEW (GraphEngine): We added a new rule, ApexNullPointerExceptionRule, to Graph Engine. Use this rule to identify Apex operations in your code that throw NullPointerExceptions. Read Graph Engine [Rules](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/) documentation for more info.
* FIX (GraphEngine): We updated Graph Engine’s UnusedMethodRule to detect all static, unused methods.
* FIX (GraphEngine): We updated Graph Engine's UnusedMethodRule to detect all unused constructors.

**Closed issues:**
* Unable to install sfdx-scanner plugin [\#1019](https://github.com/forcedotcom/sfdx-scanner/issues/1019)
* [BUG] SFGE reports false positives for constructors only called within a given class [\#1001](https://github.com/forcedotcom/sfdx-scanner/issues/1001)
* [BUG] FLS Violation not being found [\#1017](https://github.com/forcedotcom/sfdx-scanner/issues/1017)
* [BUG] Cannot add custom ruleset with XPATH rules  [\#1018](https://github.com/forcedotcom/sfdx-scanner/issues/1018)
* [Bug] How to Install the plugin link is not working [\#1011](https://github.com/forcedotcom/sfdx-scanner/issues/1011)
* [BUG] - Error when running any command: Only json and js message files are allowed, not .md [\#998](https://github.com/forcedotcom/sfdx-scanner/issues/998)
* [BUG] Cannot execute PMD scan with custom XML rules [\#992](https://github.com/forcedotcom/sfdx-scanner/issues/992)

**Merged pull requests**
* FIX (GraphEngine): @W-12672520@: Fixes format in UI text message [\#1029](https://github.com/forcedotcom/sfdx-scanner/pull/1029)
* CHANGE (GraphEngine): @W-12672520@: Applies UI text feedback [\#1024](https://github.com/forcedotcom/sfdx-scanner/pull/1024)
* CHANGE (GraphEngine): @W-12494075@: Adjusted messages for ApexNullPointerExceptionRule. [\#1021](https://github.com/forcedotcom/sfdx-scanner/pull/1021)
* CHANGE (PMD): @W-12699831@: Upgrades PMD version to 6.55.0 [\#1022](https://github.com/forcedotcom/sfdx-scanner/pull/1022)
* FIX (GraphEngine): @W-12696959@: UnusedMethodRule now fully supports constructors. [\#1020](https://github.com/forcedotcom/sfdx-scanner/pull/1020)
* NEW (GraphEngine): @W-11464344@: UnusedMethodRule supports static methods, tests enabled and passing. [\#1014](https://github.com/forcedotcom/sfdx-scanner/pull/1014)
* CHANGE (GraphEngine): @W-12672065@: Handles backend changes to cover remaining NPE Rule cases. [\#1016](https://github.com/forcedotcom/sfdx-scanner/pull/1016)
* NEW (GraphEngine): @W-12494075@: Add new rule `ApexNullPointerExceptionRule` [\#1010](https://github.com/forcedotcom/sfdx-scanner/pull/1010)
* CHANGE (CodeAnalyzer): @W-12671389@: Fixed broken links in README. [\#1012](https://github.com/forcedotcom/sfdx-scanner/pull/1012)
* FIX (GraphEngine): @W-11464344@: Refactored implementation of UnusedMethodRule. [\#1013](https://github.com/forcedotcom/sfdx-scanner/pull/1013)
* FIX (GraphEngine): @W-11464344@: Refactored tests for UnusedMethodRule. [\#1002](https://github.com/forcedotcom/sfdx-scanner/pull/1002)

## [v3.10.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.10.0) (02-27-2023)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.10.0...v3.9.0)

### Release Summary

* NEW (CodeAnalyzer): We updated the PMD engine to version 6.54.0.
* NEW (CodeAnalyzer): We made some updates to the RetireJS vulnerability database.
* FIX (CodeAnalyzer): We updated the reference to the `eslint-recommended` config to point to the updated ESLint dependency. This resolves the error received on new direct or CI/CD installs of Code Analyzer: ```ERROR Cannot find module '/home/runner/.local/share/sfdx/node_modules/eslint/conf/eslint-recommended.js```
* FIX (GraphEngine): We added a new parameter, `--pathexplimit`, on `scanner:run:dfa` to customize Graph Engine's scans of complex codebases. `SFGE_PATH_EXPANSION_LIMIT` is an alternative environment variable to provide the same customization. These options reduce the number of `OutOfMemory` errors produced. For more information, check out our [documentation](en/v3.x/salesforce-graph-engine/working-with-sfge/#understand-outofmemory-errors).

**Closed issues:**
* [BUG] ERROR Cannot find module '/home/runner/.local/share/sfdx/node_modules/eslint/conf/eslint-recommended.js' [\#986](https://github.com/forcedotcom/sfdx-scanner/issues/986)
* [Azure DevOps Pipeline error \|\| Cannot find module - share/sfdx/node_modules/eslint/conf/eslint-recommended.js ] [\#987](https://github.com/forcedotcom/sfdx-scanner/issues/987)
* naming convention [\#982](https://github.com/forcedotcom/sfdx-scanner/issues/982)
* [BUG] Typescript ESlint throws "Cannot read properties of undefined (reading 'type')" error. [\#974](https://github.com/forcedotcom/sfdx-scanner/issues/974)

**Merged pull requests**
* CHANGE (PMD): @W-12576383@: Upgrades PMD to version 6.54.0. [\#983](https://github.com/forcedotcom/sfdx-scanner/pull/983)
* CHANGE (CodeAnalyzer): @W-12439992@: Adds a new parameter to control path expansion limits. [\#977](https://github.com/forcedotcom/sfdx-scanner/pull/977)
* CHANGE (GraphEngine): @W-12439992@: Applies limit on how far Path Expansion registry data can grow to prevent OutOfMemory error [\#971](https://github.com/forcedotcom/sfdx-scanner/pull/971)
* CHANGE (GraphEngine): @W-12423302@: Decreasing stack depth to a more helpful value [\#972](https://github.com/forcedotcom/sfdx-scanner/pull/972)
* CHANGE (GraphEngine): @W-12423302@: Excludes evaluating paths that have stack depth over the allowed Apex governor limit. [\#970](https://github.com/forcedotcom/sfdx-scanner/pull/970)
* FIX (GraphEngine): @W-12138700@: Adds proper handling of boolean `== false` and `!= true` cases. [\#966](https://github.com/forcedotcom/sfdx-scanner/pull/966)

## [v3.9.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.9.0) (02-07-2023)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.9.0...v3.8.0)

### Release Summary

* NEW (GraphEngine): We added a new graph-based rule, `UnimplementedTypeRule`. This rule identifies abstract classes and interfaces that are non-global and are missing implementations or extensions. See [UnimplementedTypeRule](./en/v3.x/salesforce-graph-engine/rules/#unimplementedtyperule) for more information.
* NEW (CodeAnalyzer): We updated the PMD engine to version 6.53.0.
* NEW (CodeAnalyzer): We made some updates to the RetireJS vulnerability database.
* NEW (GraphEngine): We added sample code to support the unused methods and unused classes and interfaces rules to `sfge-working-app`.
* CHANGE (GraphEngine): We updated the `scanner:rule:describe` and `scanner:rule:list –engine` `sfge` commands to provide info on which Graph Engine rules run with scanner:run:dfa and which rules run with scanner:run.
* FIX (GraphEngine): We optimized heap usage of Graph Engine which decreases the frequency of `OutOfMemory` error.
* FIX (GraphEngine): In Winter '23, Apex added a `WITH USER_MODE` keyword to SOQL queries. Graph Engine recognizes this keyword as secure.
* FIX (CodeAnalyzer): We updated our internal dependencies on `@salesforce/core` and `@salesforce/command`, resulting in minor cosmetic changes to command output.

**Closed issues:**
* [BUG] Not able to run code scan in Staging environment. Error message is in attached screenshot. [\#933](https://github.com/forcedotcom/sfdx-scanner/issues/933)
* [Feature Request] Visual Studio Code Plugin [\#945](https://github.com/forcedotcom/sfdx-scanner/issues/945)
* [Feature Request] Do you scan for missing try catch?  Which rule would that be please [\#738](https://github.com/forcedotcom/sfdx-scanner/issues/738)
* [BUG]  Java version  not supported. Please install Java 1.8 or later in MacOS [\#625](https://github.com/forcedotcom/sfdx-scanner/issues/625)
* [BUG] Accept ES6 in LWC lint [\#372](https://github.com/forcedotcom/sfdx-scanner/issues/372)
* [Feature Request] Salesforce Scanner CLI - GitHub Action [\#345](https://github.com/forcedotcom/sfdx-scanner/issues/345)
* the extention salesforce cli integration took a very long time to complete its last operation [\#335](https://github.com/forcedotcom/sfdx-scanner/issues/335)
* How to use it in Jenkins? [\#303](https://github.com/forcedotcom/sfdx-scanner/issues/303)
* Is there a way to scan only the files specified in the manifest/package.xml file instead? [\#302](https://github.com/forcedotcom/sfdx-scanner/issues/302)
* Salesforce CLI Scanner Integration [\#304](https://github.com/forcedotcom/sfdx-scanner/issues/304)
* Make it an official Github scanner [\#239](https://github.com/forcedotcom/sfdx-scanner/issues/239)
* Feature Request - Visualization options [\#170](https://github.com/forcedotcom/sfdx-scanner/issues/170)
* [BUG] Unable to install the scanner [\#951](https://github.com/forcedotcom/sfdx-scanner/issues/951)

**Merged pull requests**
* FIX (CodeAnalyzer): @W-12458730@: Addresses InvalidRange error on output processing. [\#964](https://github.com/forcedotcom/sfdx-scanner/pull/964)
* CHANGE (GraphEngine): @W-11632796@: Optimizes data generated by path expansion to ease heap usage. [\#963](https://github.com/forcedotcom/sfdx-scanner/pull/963)
* NEW (GraphEngine): @W-12412851@: Add sample code for new GraphEngine rules. [\#962](https://github.com/forcedotcom/sfdx-scanner/pull/962)
* NEW (GraphEngine): @W-11988837@: Adds support for USER_MODE in SOQL queries. [\#961](https://github.com/forcedotcom/sfdx-scanner/pull/961)
* NEW (GraphEngine): @W-11988855@: Addition of new rule UnimplementedTypeRule. [\#944](https://github.com/forcedotcom/sfdx-scanner/pull/944)
* NEW (CodeAnalyzer): @W-12413070@: scanner:rule:list and scanner:rule:describe include DFA/non-DFA info. [\#960](https://github.com/forcedotcom/sfdx-scanner/pull/960)
* CHANGE (PMD): @W-12274063@: Update PMD to 6.53.0. [\#957](https://github.com/forcedotcom/sfdx-scanner/pull/957)
* FIX (GraphEngine): @W-11632796@: Changes to improve heapspace usage of Graph Engine [\#954](https://github.com/forcedotcom/sfdx-scanner/pull/954)
* CHANGE (CodeAnalyzer): @W-12274055@: Update various dependencies. [\#955](https://github.com/forcedotcom/sfdx-scanner/pull/955)
* CHANGE (CodeAnalyzer): @W-11294254@: Resolve errors in RetireJS catalog updater. [\#947](https://github.com/forcedotcom/sfdx-scanner/pull/947)
* CHANGE (CodeAnalyzer): @W-12300714@: Mocha test failures now display more readably. [\#940](https://github.com/forcedotcom/sfdx-scanner/pull/940)
* CHANGE (CodeAnalyzer): @W-12300714@: Test failures produce more readable output. [\#938](https://github.com/forcedotcom/sfdx-scanner/pull/938)

## [v3.8.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.8.0) (01-04-2023)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.8.0...v3.7.1)

### Release Summary

* NEW (Code Analyzer): `scanner:run` command executes graph-based rules from Graph Engine. Invoke the new rule by executing `scanner:run` command with `--engine sfge` and providing the `--projectdir` parameter. This behavior is different from path-based rules that are executed with `scanner:run:dfa`.
* NEW (Graph Engine): We added a new graph-based rule, `UnusedMethodRule`. This rule detects methods contained in your code that aren’t invoked. See [UnusedMethodRule](./en/v3.x/salesforce-graph-engine/rules/#UnusedMethodRule) for more information.
* FIX (Graph Engine): `ApexCrudFlsRule` now understands multiple levels of method invocations on Schema Standard Library objects within for-loops.
* FIX (Graph Engine): `ApexCrudFlsRule` now understands for-each iterations on Set data types and acknowledges Schema-based checks within the loops.

**Closed issues:**

- \[BUG\] False positive FLS when using custom access utility class [\#862](https://github.com/forcedotcom/sfdx-scanner/issues/862)
- \[BUG\] ERROR running scanner:run: Cannot read properties of undefined (reading 'getInstance') [\#891](https://github.com/forcedotcom/sfdx-scanner/issues/891)
- \[BUG\] ENV variable is not working [\#920](https://github.com/forcedotcom/sfdx-scanner/issues/920)

**Merged pull requests**

- NEW (GraphEngine): @W-11999008@: Add UnusedMethodRule to GraphEngine in disabled state. [\#915](https://github.com/forcedotcom/sfdx-scanner/pull/915)
- NEW (GraphEngine): @W-11999008@: Light refactor of UnusedMethodRule. [\#916](https://github.com/forcedotcom/sfdx-scanner/pull/916)
- CHANGE (GraphEngine): @W-11999008@: Refactoring appropriate methods into PathEntryPointUtil. [\#917](https://github.com/forcedotcom/sfdx-scanner/pull/917)
- CHANGE (CodeAnalyzer): @W-11999008@: Refactor DFA-based GraphEngine in preparation for enabling new rule. [\#918](https://github.com/forcedotcom/sfdx-scanner/pull/918)
- NEW (CodeAnalyzer): @W-11999008@: scanner:run now accepts --engine sfge and includes UnusedMethodRule. [\#919](https://github.com/forcedotcom/sfdx-scanner/pull/919)
- NEW (GraphEngine): @W-11533657@: New 'missingOptionsBehavior' config property allows control over what happens if GraphEngine lacks proper config. [\#921](https://github.com/forcedotcom/sfdx-scanner/pull/921)
- FIX (GraphEngine): @W-12138734@: Method invocation on Schema library objects within a forloop are now translated correctly. [\#922](https://github.com/forcedotcom/sfdx-scanner/pull/922)
- FIX (GraphEngine): @W-12138734@: Handles forEach loops executed on Set data type. [\#923](https://github.com/forcedotcom/sfdx-scanner/pull/923)
- CHANGE (CodeAnalyzer): @W-11999008@: Messages now meet doc team standards. [\#924](https://github.com/forcedotcom/sfdx-scanner/pull/924)
- CHANGE (CodeAnalyzer): @W-11533657@: Messages now meet doc team standards. [\#925](https://github.com/forcedotcom/sfdx-scanner/pull/925)
- CHANGE (CodeAnalyzer): @W-11533657@: Removed config. SFGE runs if explicitly requested, otherwise skipped. [\#926](https://github.com/forcedotcom/sfdx-scanner/pull/926)
- CHANGE (CodeAnalyzer): @W-12273138@: Updating find-java-home to latest to avoid MacOS Ventura error. [\#928](https://github.com/forcedotcom/sfdx-scanner/pull/928)
- FIX (GraphEngine): @W-11999008@: UnusedMethodRule has correct URL, and displays correct columns. [\#929](https://github.com/forcedotcom/sfdx-scanner/pull/929)

## [v3.7.1](https://github.com/forcedotcom/sfdx-scanner/tree/v3.7.1) (12-06-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.7.1...v3.6.2)

### Release Summary

* NEW: We opened the Discussions feature on our [GitHub repo](https://github.com/forcedotcom/sfdx-scanner). Use Discussions to ask and answer questions, share info, and participate in Code Analyzer and Graph Engine development.
* NEW: To integrate Code Analyzer into your continuous integration/continuous development (CI/CD) process, read our [CI/CD Integration](./en/v3.x/ci-cd-integration/) documentation.
* NEW: We made some updates to the RetireJS vulnerability database.
* NEW: We updated the PMD engine to version 6.51.0
* FIX: `SObjectType.My_Obj__c` is now recognized as a valid `DescribeSObjectResult`.
* FIX: `My_Obj__c.My_Field__c` is now recognized as a valid `SObjectField`- .

**Closed issues:**

- \[BUG\] Winter 22 Assert classes are not considered when scanning for PMD.ApexUnitTestClassShouldHaveAsserts rule [\#836](https://github.com/forcedotcom/sfdx-scanner/issues/836)
- List command - INTERNAL ERROR: Unexpected error occurred while cataloging rules: null [\#882](https://github.com/forcedotcom/sfdx-scanner/issues/882)
- \[Question\] Can the DFA detect CRUD/FLS using all forms of schema checking? [\#883](https://github.com/forcedotcom/sfdx-scanner/issues/883)
- \[BUG\] SObjectType.My_Obj__c is not recognized as a DescribeSObjectResult type [\#890](https://github.com/forcedotcom/sfdx-scanner/issues/890)

**Merged pull requests**

- @W-11831625@: Ported testing jobs from CircleCI to Github Actions. [\#843](https://github.com/forcedotcom/sfdx-scanner/pull/843)
- CHANGE(GraphEngine): @W-12024733@ We upgraded spotless plugin version to 6.11.0. [\#874](https://github.com/forcedotcom/sfdx-scanner/pull/874)
- CHANGE (CodeAnalyzer): @W-11326833@: We more robustly enforce PR naming conventions. [\#876](https://github.com/forcedotcom/sfdx-scanner/pull/876)
- CHANGE (GraphEngine): @W-12028485@: User-facing message no longer mentions SFGE. [\#879](https://github.com/forcedotcom/sfdx-scanner/pull/879)
- CHANGE (CodeAnalyzer): @W-11326833@: PR scope of 'other' is now allowed in PR titles. [\#880](https://github.com/forcedotcom/sfdx-scanner/pull/880)
- CHANGE (CodeAnalyzer): @W-11831625@: Our CI will now output more informative information. [\#884](https://github.com/forcedotcom/sfdx-scanner/pull/884)
- CHANGE (PMD): @W-12107365@: Upgrade to PMD 6.51.0 [\#892](https://github.com/forcedotcom/sfdx-scanner/pull/892)
- FIX (GraphEngine): @W-11992240@: Handle more SObjectField and DescribeSObjectResult formats [\#893](https://github.com/forcedotcom/sfdx-scanner/pull/893)
- FIX (GraphEngine): @W-12130636@: Update Apache Log4j to 2.17.1 [\#894](https://github.com/forcedotcom/sfdx-scanner/pull/894)
- FIX (CodeAnalyzer): @W-12130427@: Enclose telemetry callouts in try-catch blocks. [\#896](https://github.com/forcedotcom/sfdx-scanner/pull/896)
- CHANGE (GraphEngine): @W-12028485@: Replace SFGE in Graph Engine error messages. [\#897](https://github.com/forcedotcom/sfdx-scanner/pull/897)
- FIX (CodeAnalyzer): @W-12130427@: Replace unnecessary message template with hardcoded string. [\#898](https://github.com/forcedotcom/sfdx-scanner/pull/898)
- CHANGE (GraphEngine): @W-11988825@: Update error message for internal error violations. [\#901](https://github.com/forcedotcom/sfdx-scanner/pull/901)
- FIX (CodeAnalyzer): @W-12168234@: Replicate dependabot changes in new PR. [\#903](https://github.com/forcedotcom/sfdx-scanner/pull/903)
- FIX (GraphEngine): @W-11988825@: Links for non-rule-related GraphEngine violations now point to ApexFlsViolationRule doc [\#904](https://github.com/forcedotcom/sfdx-scanner/pull/904)


## [v3.6.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.6.0) (10-25-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.5.1...v3.6.0)

### Release Summary

* NEW: Code Analyzer v3.x is now the default version for all users.
* NEW \[SFGE\]: Use the --rule-disable-warning-violation flag with scanner:run:dfa to disable warnings on StripInaccessible calls with AccessType.READABLE.
* CHANGE \[SFGE\]: We removed the `--ignore-parse-errors` option from `scanner:run:dfa`.
* CHANGE: We've updated Code Analyzer and Graph Engine help text and user interface messages.
* NEW: We've updated PMD to version 6.50.0.

**Merged pull requests**

- CHANGE: @W-11606055@: SFCA v3.x will now publish as both latest and latest-pilot. [\#803](https://github.com/forcedotcom/sfdx-scanner/pull/803)
- CHANGE: @W-11733210@: We made SFCA source config data from Config.json instead of Config-pilot.json. [\#829](https://github.com/forcedotcom/sfdx-scanner/pull/829)
- NEW: @W-11261216@: SFCA exposes SFGE_RULE_DISABLE_WARNING_VIOLATION through flag and env-var. [\#832](https://github.com/forcedotcom/sfdx-scanner/pull/832)
- FIX: @W-11723805@: Fixed null pointer exception when parsing comments with empty commas. [\#835](https://github.com/forcedotcom/sfdx-scanner/pull/835)
- DOC: @W-11733245@: Fixed inline documentation for `scanner:run:dfa`. [\#839](https://github.com/forcedotcom/sfdx-scanner/pull/839)
- CHANGE: @W-11790341@: Compilation errors now give more digestible message.[\#840](https://github.com/forcedotcom/sfdx-scanner/pull/840)
- NEW: @W-11689621@: Updated PMD to 6.50.0. [\#841](https://github.com/forcedotcom/sfdx-scanner/pull/841)
- FIX: @W-11733245@ Fixed spacing and fullstop on SFGE violation messages. [\#844](https://github.com/forcedotcom/sfdx-scanner/pull/844)
- CHANGE: @W-11919914@ Removed `--ignore-parse-errors` option from `scanner:run:dfa` [\#845](https://github.com/forcedotcom/sfdx-scanner/pull/845)
- CHANGE: @W-11733226@: Updated messages to conform to guidelines. [\#846](https://github.com/forcedotcom/sfdx-scanner/pull/846)
- FIX: @W-11733226@ Applied minor fixes to UI message. [\#848](https://github.com/forcedotcom/sfdx-scanner/pull/848)

## [v3.5.1](https://github.com/forcedotcom/sfdx-scanner/tree/v3.5.1) (09-26-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.5.0...v3.5.1)

### Release Summary

* NEW: We made some updates to the RetireJS vulnerability database.

**Merged pull requests**

- CHANGE: @W-11261182@: Added SFGE-side telemetry events. [\#828](https://github.com/forcedotcom/sfdx-scanner/pull/828)

## [v3.5.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.5.0) (09-14-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.4.0...v3.5.0)

### Release Summary

* NEW: We made some updates to the RetireJS vulnerability database.
* NEW: To provide more memory while running `scanner:run:dfa`, use the `--sfgejvmargs` parameter.
See [Understand `OutOfMemory` Java Heap Space Error](./en/v3.x/salesforce-graph-engine/working-with-sfge/#outofmemory-java-heap-space-error) for more detail.
* FIX: If Salesforce Graph Engine errors out, partial results are now captured.

**Merged pull requests:**

- FIX: @W-11645973@remove noisy CliAppender warning logs when SFGE_LOG_WARNINGS_ON_VERBOSE is false [\#805](https://github.com/forcedotcom/sfdx-scanner/pull/805)
- NEW: @W-11646074@ Added --sfgejvmargs paramter to `scanner:run:dfa` command [\#806](https://github.com/forcedotcom/sfdx-scanner/pull/806)
- @W-11581708@: CHANGE: scanner:run:dfa command logs more constructively with --verbose. [\#807](https://github.com/forcedotcom/sfdx-scanner/pull/807)
- FIX: @W-11651888@ Salvage results collected by SFGE even if the analysis encounters an error [\#808](https://github.com/forcedotcom/sfdx-scanner/pull/808)
- CHANGE: @W-11261182@: SFCA can now emit telemetry events based on messages passed from SFGE. [\#811](https://github.com/forcedotcom/sfdx-scanner/pull/811)
- CHORE: @W-11732835@: Release activities for v3.5.0 release. [\#819](https://github.com/forcedotcom/sfdx-scanner/pull/819)
  
## [v3.4.0](https://github.com/forcedotcom/sfdx-scanner/tree/v3.4.0) (08-17-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v3.3.0...v3.4.0)

### Release Summary

* NEW \[SFGE\]: We now display progress information of Salesforce Graph Engine's analysis while executing `scanner:run:dfa` command.
* NEW: If a JavaScript target file is analyzed by both `eslint` and `eslint-lwc`, we throw a warning about duplicate violations to alert you that you should modify your configuration.
* NEW: We updated the RetireJS Vulnerability Repository.
* NEW: We upgraded PMD to 6.48.0.
* CHANGE: We replaced eslint's parser with `@babel/eslint-parser`.
* FIX: We removed the survey request banner's stylization.
* FIX \[SFGE\]: When Salesforce Graph Engine (SFGE) is unable to resolve a method call or a variable passed to a database operation, it no longer throws an internal error. Instead, SFGE creates a violation to let you know that you need to verify the CRUD/FLS access of the operation manually.

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
- @W-10459671@ Provide progress information of SFGE on verbose mode [\#792](https://github.com/forcedotcom/sfdx-scanner/pull/792)
- @W-11567651@: Updating PMD to v6.48.0. [\#794](https://github.com/forcedotcom/sfdx-scanner/pull/794)
- @W-10459671@ Redirecting ApexPathExpander logs to log file [\#796](https://github.com/forcedotcom/sfdx-scanner/pull/796)
- @W-11606553@ 3.4.0 Release updates [\#799](https://github.com/forcedotcom/sfdx-scanner/pull/799)




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

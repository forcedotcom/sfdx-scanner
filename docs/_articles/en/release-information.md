---
title: Release Information
lang: en
---

### To update the plugin, please follow the linked [instructions](./en/getting-started/install/#upgrade-plug-in)

## [v2.13.1](https://github.com/forcedotcom/sfdx-scanner/tree/v2.13.1) (03-23-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.13.0...v2.13.1)

### Release Summary

* Updated PMD to 6.43.0
* Upgrades to local RetireJS Vulnerability Repository


**Closed issues:**

- update to use PMD v6.40 or higher? [\#575](https://github.com/forcedotcom/sfdx-scanner/issues/575)
- Allow plug-in to leverage xml metadata rules [\#571](https://github.com/forcedotcom/sfdx-scanner/issues/571)

**Merged pull requests:**

- @W-10857132@: \[2.x\] Updating PMD to v6.43.0. [\#607](https://github.com/forcedotcom/sfdx-scanner/pull/607)
- @W-10771486@: \[2.x\] Updated retirejs vulnerability catalog to capture Feb-Mar 2022 changes. [\#603](https://github.com/forcedotcom/sfdx-scanner/pull/603)
- @W-10793013@ \[2.x\] Changing the name of the plugin [\#599](https://github.com/forcedotcom/sfdx-scanner/pull/599)
- @W-10459699@: \[2.x\] PR verification github action now expects a version indicator for PRs intended to be merged to dev. [\#594](https://github.com/forcedotcom/sfdx-scanner/pull/594)
- @W-10459699@: Added smoke test execution against dev-3 pilot branch [\#589](https://github.com/forcedotcom/sfdx-scanner/pull/589)
- @W-10777806@: \[2.x\] Bump nokogiri from 1.12.5 to 1.13.3 in /docs [\#588](https://github.com/forcedotcom/sfdx-scanner/pull/588)
- @W-10743341@ Bump pathval from 1.1.0 to 1.1.1 [\#579](https://github.com/forcedotcom/sfdx-scanner/pull/579)


## [v2.13.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.13.0) (02-23-2022)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.12.0...v2.13.0)

### Release Summary

* Updated PMD to 6.42.0
	* Note: EagerlyLoadedSObjectDescribeResult rule is skipped
* Upgrades to local RetireJS Vulnerability Repository


**Closed issues:**

- \[BUG\] Cannot find Config.json after installing sfdx-scanner in an Ubuntu machine [\#581](https://github.com/forcedotcom/sfdx-scanner/issues/581)
- \[BUG\] JavaScript heap out of memory while invoking RetireJS engine [\#570](https://github.com/forcedotcom/sfdx-scanner/issues/570)
- \[BUG\] [\#569](https://github.com/forcedotcom/sfdx-scanner/issues/569)
- \[RESOLVED\] Salesforce Scanner CLI Plugin - Heap Out of Memory Issue [\#564](https://github.com/forcedotcom/sfdx-scanner/issues/564)
- \[Feature Request\] Compatibility with lint-staged [\#551](https://github.com/forcedotcom/sfdx-scanner/issues/551)
- Windows install results in "Invalid Character" Windows Host Script error [\#539](https://github.com/forcedotcom/sfdx-scanner/issues/539)
- \[BUG\] Unable to install sfdx-scanner: FailedDigitalSignatureVerification [\#453](https://github.com/forcedotcom/sfdx-scanner/issues/453)

**Merged pull requests:**

- @W-10731711@: Updated RetireJS to latest version. [\#583](https://github.com/forcedotcom/sfdx-scanner/pull/583)
- @W-8885529@ Excluding transitive dependency on junit and hamcrest [\#582](https://github.com/forcedotcom/sfdx-scanner/pull/582)
- @W-10477402@ Recreate symlinks to work around nvm issue [\#580](https://github.com/forcedotcom/sfdx-scanner/pull/580)
- @W-10667880@: Updated documentation to include information about standalone XML files. [\#578](https://github.com/forcedotcom/sfdx-scanner/pull/578)
- @W-10530556@: Updated locally-stored RetireJS vulnerability catalog. [\#577](https://github.com/forcedotcom/sfdx-scanner/pull/577)
- @W-10488971@: Upgrading PMD from 6.38.0 to 6.42.0. [\#576](https://github.com/forcedotcom/sfdx-scanner/pull/576)
- @W-10488954@: Implemented hardcoded rule skipping mechanism. [\#574](https://github.com/forcedotcom/sfdx-scanner/pull/574)
- @W-10477617@: Switched to a circleci context for publishing and signing. [\#573](https://github.com/forcedotcom/sfdx-scanner/pull/573)
- @W-10302014@: Updated heartbeat GHA with support for env-vars to auto-fail script [\#568](https://github.com/forcedotcom/sfdx-scanner/pull/568)
- @W-10294218@: Fixed malformed JSON in PagerDuty alert creation code. [\#566](https://github.com/forcedotcom/sfdx-scanner/pull/566)
- @W-10165627@: Added retry logic to heartbeat script, and more nuance to alert severity. [\#562](https://github.com/forcedotcom/sfdx-scanner/pull/562)
- @W-10079694@: Remove dependency on NPX, thereby fixing failing tests [\#559](https://github.com/forcedotcom/sfdx-scanner/pull/559)
- @W-9971168@: Added coverage enforcement in both nyc and gradle. Cover… [\#557](https://github.com/forcedotcom/sfdx-scanner/pull/557)
- @W-9954706@: Parameterized unit test jobs, and added them to the dail… [\#556](https://github.com/forcedotcom/sfdx-scanner/pull/556)
- @W-9296556@: Moved engine execution to the inside of a try-catch, to … [\#554](https://github.com/forcedotcom/sfdx-scanner/pull/554)
- @W-9954706@: Changed smoke tests to daily run instead of weekly. [\#548](https://github.com/forcedotcom/sfdx-scanner/pull/548)


## [v2.12.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.12.0) (09-29-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.11.0...v2.12.0)

### Release Summary

* Updated PMD to 6.38.0 and RetireJS to 2.2.5

**Merged pull requests:**

- @W-9930749@: Reverting the heartbeat scheduling and timeout to the de… [\#546](https://github.com/forcedotcom/sfdx-scanner/pull/546)
- @W-9930749@: Fixing error in dedup key. [\#545](https://github.com/forcedotcom/sfdx-scanner/pull/545)
- @W-9930749@: Added dedup key to Github action. [\#544](https://github.com/forcedotcom/sfdx-scanner/pull/544)
- @W-9930749@: Cancelled, i.e. timed-out, heartbeat should now cause pa… [\#543](https://github.com/forcedotcom/sfdx-scanner/pull/543)
- @W-9930749@: Added a 60-minute timeout to the heartbeat action. [\#542](https://github.com/forcedotcom/sfdx-scanner/pull/542)
- @W-9930749@: Added a 60-minute timeout to the heartbeat action. [\#540](https://github.com/forcedotcom/sfdx-scanner/pull/540)
- @W-9846051@: Github Action heartbeat script now runs against release … [\#536](https://github.com/forcedotcom/sfdx-scanner/pull/536)
- @W-9879038@: Updated PMD to 6.38.0. [\#534](https://github.com/forcedotcom/sfdx-scanner/pull/534)
- @W-9791463@: Updated RetireJS to 2.2.5, and updated locally-stored vu… [\#533](https://github.com/forcedotcom/sfdx-scanner/pull/533)
- @W-9879056@: Publishing can now only happen on branches named v2-X-Y,… [\#532](https://github.com/forcedotcom/sfdx-scanner/pull/532)


## [v2.11.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.11.0) (08-18-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.10.1005...v2.11.0)

### Release Summary

* Support for PMD Copy Paste Detector engine

**Closed issues:**

- CPD Scan [\#378](https://github.com/forcedotcom/sfdx-scanner/issues/378)

**Merged pull requests:**

- @W-9729358@ Fixing bugs detected in QA [\#526](https://github.com/forcedotcom/sfdx-scanner/pull/526)
- @W-9764161@ 2.11 release notes [\#525](https://github.com/forcedotcom/sfdx-scanner/pull/525)
- @W-9106321@, @W-9729358@, Implementing CPD Engine for CLI Scanner [\#524](https://github.com/forcedotcom/sfdx-scanner/pull/524)
- @W-9729358@  Documentation for CPD Engine [\#523](https://github.com/forcedotcom/sfdx-scanner/pull/523)
- @W-9729358@ Make CPD engine production ready [\#522](https://github.com/forcedotcom/sfdx-scanner/pull/522)
- @W-9750680@ Bump path-parse from 1.0.6 to 1.0.7 [\#521](https://github.com/forcedotcom/sfdx-scanner/pull/521)
- @W-9710226@: Added better guardrails for publishing, more informative heartbeat, and more comprehensive gitignore. [\#520](https://github.com/forcedotcom/sfdx-scanner/pull/520)
- @W-9717770@: Added manual trigger option for heartbeat action. [\#519](https://github.com/forcedotcom/sfdx-scanner/pull/519)


## [v2.10.1005](https://github.com/forcedotcom/sfdx-scanner/tree/v2.10.1005) (08-05-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.10.0...v2.10.1005)

### Release Summary

* No new features or bug fixes
* Only operational changes to release management

**Merged pull requests:**

- @W-9544986@: Releases now key off of branches instead of tags. [\#505](https://github.com/forcedotcom/sfdx-scanner/pull/505)
- @W-9544986@: 2.10.1000, dev to release [\#504](https://github.com/forcedotcom/sfdx-scanner/pull/504)
- @W-9544986@ Updating fingerprint for CircleCI [\#503](https://github.com/forcedotcom/sfdx-scanner/pull/503)
- @W-9544986@: Renamed tasks, changed tag trigger regex. [\#502](https://github.com/forcedotcom/sfdx-scanner/pull/502)
- @W-9544986@: 2.10.1000, dev to release [\#501](https://github.com/forcedotcom/sfdx-scanner/pull/501)
- @W-9544986@: Removed github release creation from publishing job. [\#500](https://github.com/forcedotcom/sfdx-scanner/pull/500)
- @W-9544986@: 2.10.1000, dev to release [\#499](https://github.com/forcedotcom/sfdx-scanner/pull/499)
- @W-9698990@ Removing post install script from package.json [\#498](https://github.com/forcedotcom/sfdx-scanner/pull/498)
- @W-9544986@: 2.10.1000, merging dev to release [\#497](https://github.com/forcedotcom/sfdx-scanner/pull/497)
- @W-9544986@ Release Information for 2.10.1000 [\#496](https://github.com/forcedotcom/sfdx-scanner/pull/496)
- @W-9544986@: updated npm-release-management orb to v4, and automated promotion job. [\#495](https://github.com/forcedotcom/sfdx-scanner/pull/495)
- @W-9531479@: Added weekly smoke-test job, and parameterized the node … [\#493](https://github.com/forcedotcom/sfdx-scanner/pull/493)
- @W-9625690@: Added some documentation to explain how cron syntax works. [\#491](https://github.com/forcedotcom/sfdx-scanner/pull/491)
- @W-9668161@: Added tarball tests to CircleCI, refactored and renamed … [\#489](https://github.com/forcedotcom/sfdx-scanner/pull/489)
- @W-9531823@: Replaced packaged-sanity job with windows-rc-test and li… [\#484](https://github.com/forcedotcom/sfdx-scanner/pull/484)
- @W-9625690@: Bumping package.json and removing publishing mechanisms. [\#483](https://github.com/forcedotcom/sfdx-scanner/pull/483)
- @W-9625690@: Enabling PagerDuty alerts for heartbeat action. [\#481](https://github.com/forcedotcom/sfdx-scanner/pull/481)
- @W-9625690@: Added github action to run heartbeat script against production plugin. [\#478](https://github.com/forcedotcom/sfdx-scanner/pull/478)
- @W-9603774@ Update release-information.md to reflect correct 'Closed Issues' for 2.9.2 [\#458](https://github.com/forcedotcom/sfdx-scanner/pull/458)


## [v2.10.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.10.0) (07-21-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.9.2...v2.10.0)

### Release Summary

* New option to Normalize Severity across all engines
* New option to error out of the scan based on a Severity Threshold
* Underlying PMD Engine is upgraded to 6.36.0
* Bug fixes

**Closed issues:**

- Engine for scanner is not working if I specify pmd for Apex classes [\#442](https://github.com/forcedotcom/sfdx-scanner/issues/442)
- @W-8241107@ Specifying "--engine pmd" finds zero violations [\#260](https://github.com/forcedotcom/sfdx-scanner/issues/260)
- @W-8063535@ Define PMD min severity [\#308](https://github.com/forcedotcom/sfdx-scanner/issues/308)
- \[Feature\] Allow scanner:run to fail automated builds \#171 [\#374](https://github.com/forcedotcom/sfdx-scanner/issues/374)

**Merged pull requests:**

- @W-8063535@ Changes to severity-threshold message + fixing related tests [\#473](https://github.com/forcedotcom/sfdx-scanner/pull/473)
- @W-9627245@ New Business Stopping Bug template [\#469](https://github.com/forcedotcom/sfdx-scanner/pull/469)
- @W-9531390@: Windows smoke test should now exit with code 1 on error. [\#466](https://github.com/forcedotcom/sfdx-scanner/pull/466)
- @W-9613208@: Hardcode find-java-home to v1.1.0 [\#465](https://github.com/forcedotcom/sfdx-scanner/pull/465)
- @W-9531390@: Added smoke tests to linux build task, and made smoke te… [\#464](https://github.com/forcedotcom/sfdx-scanner/pull/464)
- @W-9603086@ Update the fingerprint and change how it is passed [\#463](https://github.com/forcedotcom/sfdx-scanner/pull/463)
- @W-9531823@: Changed the tag so the release is published as latest-rc… [\#462](https://github.com/forcedotcom/sfdx-scanner/pull/462)
- @W-9593996@ Upgrade PMD to 6.36.0 [\#461](https://github.com/forcedotcom/sfdx-scanner/pull/461)
- @W-8063535@ Severity normalizer changes [\#460](https://github.com/forcedotcom/sfdx-scanner/pull/460)
- @W-9603774@ Update release-information.md to reflect correct 'Closed Issues' for 2.9.2 [\#459](https://github.com/forcedotcom/sfdx-scanner/pull/459)
- @W-9531534@: Yarn is now installed as a separate run task instead of … [\#456](https://github.com/forcedotcom/sfdx-scanner/pull/456)
- @W-8063535@ Severity normalizer [\#454](https://github.com/forcedotcom/sfdx-scanner/pull/454)
- @W-8063535@ Documentation for severity threshold and normalization [\#448](https://github.com/forcedotcom/sfdx-scanner/pull/448)
- @W-9598768@ Bump addressable from 2.7.0 to 2.8.0 in /docs [\#443](https://github.com/forcedotcom/sfdx-scanner/pull/443)
- @W-9531534@: Added node orb and bash script to upgrade node to LTS. [\#436](https://github.com/forcedotcom/sfdx-scanner/pull/436)
- @W-9531390@: Renamed smoke test scripts for clarity. [\#435](https://github.com/forcedotcom/sfdx-scanner/pull/435)
- @W-9531390@: Replaced sanity test with a more robust and platform-agn… [\#429](https://github.com/forcedotcom/sfdx-scanner/pull/429)
- @W-8241107@: Non-RuleGroup filters are now ignored by the getRuleGrou… [\#426](https://github.com/forcedotcom/sfdx-scanner/pull/426)
- @W-9498589@: Update documentation to 2.9.2 version [\#425](https://github.com/forcedotcom/sfdx-scanner/pull/425)


## [v2.9.2](https://github.com/forcedotcom/sfdx-scanner/tree/v2.9.2) (06-23-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.9.1...v2.9.2)

**Merged pull requests:**

- @W-9344448@: Updated local RetireJS catalog to reflect recent changes. [\#418](https://github.com/forcedotcom/sfdx-scanner/pull/418)
- @W-9282230@ Changes to release information to include closed issue [\#415](https://github.com/forcedotcom/sfdx-scanner/pull/415)

## [v2.9.1](https://github.com/forcedotcom/sfdx-scanner/tree/v2.9.1) (05-21-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.8.0...v2.9.1)

### Release Summary

* RetireJS will now work offline
* Underlying PMD Engine is upgraded to 6.34.0 (from 6.33.0) 
* Bug fixes

**Closed issues:**

- \[BUG\] @W-9296240@ Commands result in error "Unable to determine a suitable edition, even after broadening." [\#412](https://github.com/forcedotcom/sfdx-scanner/issues/412)
- \[Feature Request\] - Suppress Violation [\#386](https://github.com/forcedotcom/sfdx-scanner/issues/386)
- Running scanner throws JAVA error\[BUG\] [\#381](https://github.com/forcedotcom/sfdx-scanner/issues/381)

**Merged pull requests:**

- @W-9296240@ Switch library used to detect binary files [\#411](https://github.com/forcedotcom/sfdx-scanner/pull/411)
- @W-9295472@: Moved vulnerability catalog into different folder, force… [\#408](https://github.com/forcedotcom/sfdx-scanner/pull/408)
- @W-9156805@ Disabling some features while XML parsing [\#399](https://github.com/forcedotcom/sfdx-scanner/pull/399)
- @W-9266722@ Update fingerprint used for certificate pinning [\#398](https://github.com/forcedotcom/sfdx-scanner/pull/398)
- @W-9230264@ Bump nokogiri from 1.11.0 to 1.11.4 in /docs [\#397](https://github.com/forcedotcom/sfdx-scanner/pull/397)
- @W-9219050@: Changed RetireJsEngine to use locally stored version of … [\#395](https://github.com/forcedotcom/sfdx-scanner/pull/395)
- @W-9230264@: Updated third-party dependencies. [\#393](https://github.com/forcedotcom/sfdx-scanner/pull/393)
- @W-9230027@: Update PMD to 6.34.0. [\#392](https://github.com/forcedotcom/sfdx-scanner/pull/392)
- @W-9230725@: Removed CodeCov and Appveyor. [\#389](https://github.com/forcedotcom/sfdx-scanner/pull/389)
- @W-9282442@ Bump rexml from 3.2.4 to 3.2.5 in /docs [\#385](https://github.com/forcedotcom/sfdx-scanner/pull/385)

## [v2.8.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.8.0) (04-13-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.7.0...v2.8.0)

**Closed issues:**

- \[Feature Request\] CHANGELOG [\#371](https://github.com/forcedotcom/sfdx-scanner/issues/371)

**Merged pull requests:**

- @W-9086306@ Upgrading to PMD 6.33.0 [\#379](https://github.com/forcedotcom/sfdx-scanner/pull/379)
- @W-9090976@ Bump y18n from 4.0.0 to 4.0.1 [\#377](https://github.com/forcedotcom/sfdx-scanner/pull/377)
- @W-9090976@ Bump kramdown from 2.3.0 to 2.3.1 in /docs [\#376](https://github.com/forcedotcom/sfdx-scanner/pull/376)

## [v2.7.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.7.0) (03-17-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.6.0...v2.7.0)

**Closed issues:**

- \[BUG\] [\#366](https://github.com/forcedotcom/sfdx-scanner/issues/366)
- eslint: Parsing error: Unexpected character '@' [\#360](https://github.com/forcedotcom/sfdx-scanner/issues/360)

**Merged pull requests:**

- @W-8902730@: ZIPs containing directories are now properly handled. [\#368](https://github.com/forcedotcom/sfdx-scanner/pull/368)
- @W-8675055@: Fixed some issues with the documentation. [\#367](https://github.com/forcedotcom/sfdx-scanner/pull/367)
- @W-8675055@ Changes to docs for supported languages \(PMD\) [\#365](https://github.com/forcedotcom/sfdx-scanner/pull/365)
- @W-895007@ Upgrade PMD from 6.31.0 to 6.32.0 [\#364](https://github.com/forcedotcom/sfdx-scanner/pull/364)
- @W-8956131@: Implemented versioning schema to allow easy upgrades. [\#363](https://github.com/forcedotcom/sfdx-scanner/pull/363)
- @W-8914422@: RetireJS is now better at identifying static resources and scanning the contents of ZIPs. [\#362](https://github.com/forcedotcom/sfdx-scanner/pull/362)
- @W-8902730@: RetireJS violations within a ZIP are now associated with… [\#361](https://github.com/forcedotcom/sfdx-scanner/pull/361)
- @W-8903300@ DOC: Remove warning 'not digitally signed' [\#359](https://github.com/forcedotcom/sfdx-scanner/pull/359)

## [v2.6.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.6.0) (02-17-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.5.1...v2.6.0)

### Release Summary

* Support for SARIF JSON Format
* Underlying PMD Engine is upgraded to 6.31.0 (from 6.30.0) 
* RetireJS upgraded to v2.2.4

**Closed issues:**

- Support SARIF JSON Format [\#309](https://github.com/forcedotcom/sfdx-scanner/issues/309)

**Merged pull requests:**

- @W-8629051@: trees and pmd-scala JARs are now properly removed from d… [\#352](https://github.com/forcedotcom/sfdx-scanner/pull/352)
- @W-8553897@ Add sarif formatter [\#350](https://github.com/forcedotcom/sfdx-scanner/pull/350)
- @W-8841422@: Updating RetireJS to v2.2.4. [\#349](https://github.com/forcedotcom/sfdx-scanner/pull/349)
- @W-8841422@: RetireJS now determines file eligibility by checking con… [\#348](https://github.com/forcedotcom/sfdx-scanner/pull/348)
- @W-8629051@: Pulled the trigger on PMD language deprecation, and excl… [\#344](https://github.com/forcedotcom/sfdx-scanner/pull/344)


## [v2.5.1](https://github.com/forcedotcom/sfdx-scanner/tree/v2.5.1) (01-20-2021)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.4.0...v2.5.1)

### Release Summary

* Changes to allow custom config for PMD and Eslint
* Added enhanced summary message to the end of all flows
* We now intercept all PMD errors and convert them into rule violations
* Underlying PMD Engine is upgraded to 6.30.0 (from 6.29.0)
* Bug fixes 

**Closed issues:**

- Enable eslintrc rules to be used by default for JS files [\#330](https://github.com/forcedotcom/sfdx-scanner/issues/330)
- Invalid Java Home [\#316](https://github.com/forcedotcom/sfdx-scanner/issues/316)
- XML validation throws warning at PMDException [\#277](https://github.com/forcedotcom/sfdx-scanner/issues/277)
- eslint-lwc engine not running by default on scanner:run [\#276](https://github.com/forcedotcom/sfdx-scanner/issues/276)
- Internal error running scanner commands from VS Code terminal with custom rules [\#261](https://github.com/forcedotcom/sfdx-scanner/issues/261)
- Let standard PMD rules be disabled by config file/args [\#246](https://github.com/forcedotcom/sfdx-scanner/issues/246)
- @W-8046146@ Digitally Sign the Plugin [\#241](https://github.com/forcedotcom/sfdx-scanner/issues/241)
- The Scanner should \(only\) use each engine default configs/rulesets [\#248](https://github.com/forcedotcom/sfdx-scanner/issues/248)

**Merged pull requests:**

- @W-8668445@: Removing PLSQL from the list of fully supported languages. [\#333](https://github.com/forcedotcom/sfdx-scanner/pull/333)
- @W-8668445@: Added soft warning for languages we plan on deprecating. [\#331](https://github.com/forcedotcom/sfdx-scanner/pull/331)
- @W-8115400@: Added new fields to Describe output. [\#328](https://github.com/forcedotcom/sfdx-scanner/pull/328)
- @W-8578721@: Updated PMD to 6.30.0. [\#327](https://github.com/forcedotcom/sfdx-scanner/pull/327)
- @W-8565689@: Updated error messages about javaHome identification fai… [\#326](https://github.com/forcedotcom/sfdx-scanner/pull/326)
- @W-8615780@: Changed the way parsing errors are handled so it is consistent across engines. [\#324](https://github.com/forcedotcom/sfdx-scanner/pull/324)
- @W-8559396@ Fixing engine selection bug and adding more end to end tests [\#321](https://github.com/forcedotcom/sfdx-scanner/pull/321)
- @W-8033718@: Rules that are extended by other rules are now excluded from catalog generation. Same with deprecated rules. [\#320](https://github.com/forcedotcom/sfdx-scanner/pull/320)
- @W-8517960@: Updated documentation for eslint custom configuration to… [\#311](https://github.com/forcedotcom/sfdx-scanner/pull/311)
- @W-8497246@: We now intercept arcane PMD errors and convert them into… [\#310](https://github.com/forcedotcom/sfdx-scanner/pull/310)
- @W-8388246@: Added enhanced summary message to the end of all flows. [\#306](https://github.com/forcedotcom/sfdx-scanner/pull/306)
- @W-7992418@ Allow negated category filtering [\#305](https://github.com/forcedotcom/sfdx-scanner/pull/305)
- @W-8266225@ Documentation for Custom config [\#301](https://github.com/forcedotcom/sfdx-scanner/pull/301)
- @W-8266225@ Changes to allow custom config for PMD and Eslint [\#297](https://github.com/forcedotcom/sfdx-scanner/pull/297)
- @W-8340323@: Added tentative documentation for PMD, and ESLint variants. [\#294](https://github.com/forcedotcom/sfdx-scanner/pull/294)
- @W-8332099@: Changed the way we handle errors from RetireJS. [\#284](https://github.com/forcedotcom/sfdx-scanner/pull/284)


## [v2.4.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.4.0) (11-04-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.3.0...v2.4.0)

### Release Summary

* RetireJS - Supports a new engine to detect vulnerable Javascript Library in the project
* Underlying PMD Engine is upgraded to 6.29.0 (from 6.28.0)
* Bug fixes 

**Closed issues:**

- @W-8118474@ specifying --category flag in scanner:run command has no effect for JavaScript and TypeScript files [\#224](https://github.com/forcedotcom/sfdx-scanner/issues/224)
- @W-8117190@ engines.eslint-lwc.disabled value in Config.json gets overwritten in Windows and Linux [\#220](https://github.com/forcedotcom/sfdx-scanner/issues/220)

**Merged pull requests:**

- @W-8277866@ Upgrade PMD 6.28.0 to 6.29.0 [\#273](https://github.com/forcedotcom/sfdx-scanner/pull/273)
- @W-8294938@: Added missing MIME types to StaticResourceHandler. [\#272](https://github.com/forcedotcom/sfdx-scanner/pull/272)
- @W-8273114@: Supplemental pull request to resolve issues across platforms, most notably Linux. [\#269](https://github.com/forcedotcom/sfdx-scanner/pull/269)
- @W-8273114@: Fixed errors that were preventing use of RetireJS on Windows. [\#268](https://github.com/forcedotcom/sfdx-scanner/pull/268)
- @W-8272961@: Fixed potential race conditions in the RetireJS file sym… [\#267](https://github.com/forcedotcom/sfdx-scanner/pull/267)
- chore\(build\): setup plugin signing @W-8046146@ [\#266](https://github.com/forcedotcom/sfdx-scanner/pull/266)
- @W-8246167@: Rewrote a few error messages and changed how we choose t… [\#263](https://github.com/forcedotcom/sfdx-scanner/pull/263)
- @W-7983263@: Implemented .zip and .resource support for RetireJsEngine. [\#259](https://github.com/forcedotcom/sfdx-scanner/pull/259)
- @W-8221285@: Upgraded RetireJS to v2.2.3 and switched the relevant us… [\#256](https://github.com/forcedotcom/sfdx-scanner/pull/256)
- @W-7978510@ and @W-7983101@: Implementation of RetireJS for scanner:run command. [\#255](https://github.com/forcedotcom/sfdx-scanner/pull/255)
- @W-7978510@ and @W-7983101@: Integration of RetireJS into sfdx-scanner. [\#254](https://github.com/forcedotcom/sfdx-scanner/pull/254)
- @W-7983101@: Implementation of `run` command for RetireJS. [\#249](https://github.com/forcedotcom/sfdx-scanner/pull/249)
- @W-7978510@: Initial partial implementation of RetireJS engine. Implementation of scanner:run command still lacking. [\#231](https://github.com/forcedotcom/sfdx-scanner/pull/231)

## [v2.3.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.3.0) (10-08-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.2.1...v2.3.0)

### Release Summary
* Underlying PMD Engine is upgraded to 6.28.0 (from 6.22.0)
* Release Information and release summary in our Documentation
* Bug fixes 

**Merged pull requests:**

- @W-8178014@ Remove old and redundant usage information from README.md [\#242](https://github.com/forcedotcom/sfdx-scanner/pull/242)
- @W-8101850@ Display a warning when targets don't match [\#237](https://github.com/forcedotcom/sfdx-scanner/pull/237)
- @W-8081789@ scanner:rule:list now omits rules for disabled engines u… [\#236](https://github.com/forcedotcom/sfdx-scanner/pull/236)
- @W-8141814@ Git action changes to validate PRs that start with d/ or r/ [\#235](https://github.com/forcedotcom/sfdx-scanner/pull/235)
- @W-8122073@ Fix csv output escaping issue [\#234](https://github.com/forcedotcom/sfdx-scanner/pull/234)
- @W-8121424@ Fix filter by category for extended configs [\#233](https://github.com/forcedotcom/sfdx-scanner/pull/233)
- @W-8117040@ Release Information in our developer docs [\#232](https://github.com/forcedotcom/sfdx-scanner/pull/232)
- @W-8019507@ Upgrade PMD to 6.28.0 [\#230](https://github.com/forcedotcom/sfdx-scanner/pull/230)
- @W-8117190@ Fix issue with default false values [\#222](https://github.com/forcedotcom/sfdx-scanner/pull/222)


## [v2.2.1](https://github.com/forcedotcom/sfdx-scanner/tree/v2.2.1) (09-28-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.2.0...v2.2.1)

### Release Summary
* Bug fix for a regression

**Merged pull requests:**

- @W-8129868@ Increment version in the docs [\#228](https://github.com/forcedotcom/sfdx-scanner/pull/228)
- @W-8129868@ Preparing for 2.2.1 release [\#226](https://github.com/forcedotcom/sfdx-scanner/pull/226)
- @W-8118474@ Fix filter by Category regression [\#223](https://github.com/forcedotcom/sfdx-scanner/pull/223)

## [v2.2.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.2.0) (09-23-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.1.1...v2.2.0)

### Release Summary
* New feature to scan Lightning Web Components using ESlint
* HTML report enhancements 
* Bug fixes



**Closed issues:**

- Allow scanner:run to fail automated builds [\#171](https://github.com/forcedotcom/sfdx-scanner/issues/171)

**Merged pull requests:**

- @W-8111353@ Switch path of init hook [\#216](https://github.com/forcedotcom/sfdx-scanner/pull/216)
- @W-8105926@ Config\#isEngineEnabled should use \#getConfigValue [\#215](https://github.com/forcedotcom/sfdx-scanner/pull/215)
- @W-8105674@ Move engine version info to a single yml file [\#214](https://github.com/forcedotcom/sfdx-scanner/pull/214)
- @W-8085631@: Refactored the way negative globs are dealt with, so the… [\#212](https://github.com/forcedotcom/sfdx-scanner/pull/212)
- @W-8095993@ Increment transitive bl dependency version [\#211](https://github.com/forcedotcom/sfdx-scanner/pull/211)
- @W-8089974@ Trim category filters [\#208](https://github.com/forcedotcom/sfdx-scanner/pull/208)
- @W-8062540@: Refactored code into PathMatcher class and added unit te… [\#206](https://github.com/forcedotcom/sfdx-scanner/pull/206)
- @W-7921470@ Change eslint-lwc engine to use eslint rules [\#192](https://github.com/forcedotcom/sfdx-scanner/pull/192)
- @W-8046080@ Adding Code of conduct and Security md files [\#190](https://github.com/forcedotcom/sfdx-scanner/pull/190)
- @W-7972245@ Change sfdx-scanner to Salesforce CLI Scanner [\#187](https://github.com/forcedotcom/sfdx-scanner/pull/187)
- @W-8035436@ Added --engine flag [\#185](https://github.com/forcedotcom/sfdx-scanner/pull/185)
- @W-7921470@ Add LWC Engine [\#184](https://github.com/forcedotcom/sfdx-scanner/pull/184)
- @W-7957489@ Fix some corner cases for the HTML report [\#181](https://github.com/forcedotcom/sfdx-scanner/pull/181)
- @W-7992570@ Minor doc changes to include build information [\#178](https://github.com/forcedotcom/sfdx-scanner/pull/178)
- @W-7957489@ HTML report enhancements [\#173](https://github.com/forcedotcom/sfdx-scanner/pull/173)
- @W-7972415@ Marking Rulesets column as deprecated in list command [\#168](https://github.com/forcedotcom/sfdx-scanner/pull/168)
- @W-7972415@ Marking ruleset as a deprecated option in run and list command [\#167](https://github.com/forcedotcom/sfdx-scanner/pull/167)
- @W-7971516@ Adding visualforce to default languages supported by PMD [\#166](https://github.com/forcedotcom/sfdx-scanner/pull/166)
- @W-7927194@ Added additional escaping found during testing [\#165](https://github.com/forcedotcom/sfdx-scanner/pull/165)
- @W-7905086@ Inversion of control to use an alternative directory for testing [\#164](https://github.com/forcedotcom/sfdx-scanner/pull/164)
- @W-7927194@: Added escaping for HTML characters in JUnit output. [\#163](https://github.com/forcedotcom/sfdx-scanner/pull/163)
- @W-7930419@ Add html output option [\#162](https://github.com/forcedotcom/sfdx-scanner/pull/162)
- @W-7927194@: Proper JUnit formatting of output. [\#160](https://github.com/forcedotcom/sfdx-scanner/pull/160)
- @W-7937995@ Bump kramdown from 2.2.1 to 2.3.0 in /docs [\#158](https://github.com/forcedotcom/sfdx-scanner/pull/158)
- @W-7905189@ Display configError, error, suppressedViolation [\#157](https://github.com/forcedotcom/sfdx-scanner/pull/157)
- Run scanner against itself during CI @W-7905201@ [\#156](https://github.com/forcedotcom/sfdx-scanner/pull/156)
- Add git2gus issueTypeLabels @W-7922008@ [\#155](https://github.com/forcedotcom/sfdx-scanner/pull/155)
- @W-7905107@ Updating versions of dependencies [\#153](https://github.com/forcedotcom/sfdx-scanner/pull/153)
- @W-7905028@ Add github action to validate PR titles [\#152](https://github.com/forcedotcom/sfdx-scanner/pull/152)
- @W-7907050@ Move ScannerCommand class to lib directory [\#151](https://github.com/forcedotcom/sfdx-scanner/pull/151)
- Bump lodash from 4.17.15 to 4.17.19 [\#149](https://github.com/forcedotcom/sfdx-scanner/pull/149)
- adding sfdx-scanner to the baseUrl [\#146](https://github.com/forcedotcom/sfdx-scanner/pull/146)

## [v2.1.1](https://github.com/forcedotcom/sfdx-scanner/tree/v2.1.1) (09-10-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.1.0...v2.1.1)

### Release Summary
* Bug fix for a regression

**Merged pull requests:**

- @W-8063136@: Incrementing version from 2.1.0 to 2.1.1 [\#201](https://github.com/forcedotcom/sfdx-scanner/pull/201)
- @W-8063398@ Add html-templates to 'files' node [\#199](https://github.com/forcedotcom/sfdx-scanner/pull/199)

## [v2.1.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.1.0) (09-09-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v2.0.0...v2.1.0)

### Release Summary
* Adding visualforce to default languages supported by PMD
* A non-zero exit code for CI/CD
* Bug fixes

**Closed issues:**

- Expectations for upgrades? [\#180](https://github.com/forcedotcom/sfdx-scanner/issues/180)
- Enable Automated Code Reviews \(using free Codacy.com\) on this repo [\#177](https://github.com/forcedotcom/sfdx-scanner/issues/177)
- Feature Request - Async Mode for the CICD purpose [\#169](https://github.com/forcedotcom/sfdx-scanner/issues/169)

**Merged pull requests:**

- @W-8063136@: Incrementing version to 2.1 for new release. [\#198](https://github.com/forcedotcom/sfdx-scanner/pull/198)
- @W-8056964@: Fixed issue with negated patterns not being properly com… [\#197](https://github.com/forcedotcom/sfdx-scanner/pull/197)
- @W-8058863@ @W-8058863@ Fix for unit tests failing on windows [\#195](https://github.com/forcedotcom/sfdx-scanner/pull/195)
- @W-8058687@ Adding troubleshoot section for VF parse error [\#194](https://github.com/forcedotcom/sfdx-scanner/pull/194)
- @W-8056964@: Bower components now excluded by default. Troubleshootin… [\#193](https://github.com/forcedotcom/sfdx-scanner/pull/193)
- @W-8002539@: Allowing violations to cause non-zero exit code for CI/CD purposes. [\#182](https://github.com/forcedotcom/sfdx-scanner/pull/182)
[\#175](https://github.com/forcedotcom/sfdx-scanner/pull/175)
- @W-7971516@ Adding visualforce to default languages supported by PMD [\#174](https://github.com/forcedotcom/sfdx-scanner/pull/174)
- updating the baseURL for docs [\#148](https://github.com/forcedotcom/sfdx-scanner/pull/148)
- Revert "Adding sfdx-scanner to the baseUrl" [\#144](https://github.com/forcedotcom/sfdx-scanner/pull/144)
- Adding sfdx-scanner to the baseUrl [\#141](https://github.com/forcedotcom/sfdx-scanner/pull/141)
- Updating release branch with the 2.0 dev [\#139](https://github.com/forcedotcom/sfdx-scanner/pull/139)
- Fixing error in packaged-sanity command. [\#138](https://github.com/forcedotcom/sfdx-scanner/pull/138)

## [v2.0.0](https://github.com/forcedotcom/sfdx-scanner/tree/v2.0.0) (07-13-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.30...v2.0.0)

### Release Summary
* Eslint Engine support to scan Javascript and Typescript
* Developer documentation
* Bug fixes

**Closed issues:**

- --target to become more like --path [\#123](https://github.com/forcedotcom/sfdx-scanner/issues/123)
- Question: Will scanner provide the same results as Checkmarx/Partner Security Portal [\#92](https://github.com/forcedotcom/sfdx-scanner/issues/92)
- Scanner does not show up in sfdx commands after installation [\#89](https://github.com/forcedotcom/sfdx-scanner/issues/89)

**Merged pull requests:**

- Incrementing version to 2.0.0. [\#137](https://github.com/forcedotcom/sfdx-scanner/pull/137)
- Update to add Google Tag Manager for analytics [\#136](https://github.com/forcedotcom/sfdx-scanner/pull/136)
- @W-7805991@ Add more defaults to DEFAULT\_ENV\_VARS [\#135](https://github.com/forcedotcom/sfdx-scanner/pull/135)
- @W-7802169@ Add eslint out of memory troubleshooter [\#134](https://github.com/forcedotcom/sfdx-scanner/pull/134)
- @w-7791882@ -TypeScript scanner throws "X is undefined" errors for elements of mocha/jasmine/chai etc. [\#133](https://github.com/forcedotcom/sfdx-scanner/pull/133)
- @W-7792085@ Filter out invalid catalog files [\#132](https://github.com/forcedotcom/sfdx-scanner/pull/132)
- @W-776022@ Give precedence to --tsconfig flag over cwd [\#131](https://github.com/forcedotcom/sfdx-scanner/pull/131)
- @W-7742718@ Filter PMD results before processing violations [\#130](https://github.com/forcedotcom/sfdx-scanner/pull/130)
- @W-7740484@ Config inconsistency fix [\#129](https://github.com/forcedotcom/sfdx-scanner/pull/129)
- @W-7760223@ Updated documentation for --tsconfig flag [\#128](https://github.com/forcedotcom/sfdx-scanner/pull/128)
- @W-7785414@ Catalog language fix [\#127](https://github.com/forcedotcom/sfdx-scanner/pull/127)
- @W-7786899@Update Readme.md with changes to the order & also a link to the git page [\#126](https://github.com/forcedotcom/sfdx-scanner/pull/126)
- @w-7760223@-Introduce --tsconfig to the run command \(possibly List command\) [\#125](https://github.com/forcedotcom/sfdx-scanner/pull/125)
- @W-7740890@ Adding documentation about category inconsistency [\#124](https://github.com/forcedotcom/sfdx-scanner/pull/124)
- @W-7740537@-Disable PMD for JS by default and throw a warning if the user changes this [\#122](https://github.com/forcedotcom/sfdx-scanner/pull/122)
- @W-7740484@ Fixing targetPatterns Config lookup and default values [\#121](https://github.com/forcedotcom/sfdx-scanner/pull/121)
- @W-7661074@ ESLint changes [\#120](https://github.com/forcedotcom/sfdx-scanner/pull/120)
- @w-7740890@-Custom PMD JARs/XMLs cannot be added for a language if the default PMD rules are not enabled for that language. [\#119](https://github.com/forcedotcom/sfdx-scanner/pull/119)
- @W-7661091@ More unit tests for BaseEslintEngine [\#118](https://github.com/forcedotcom/sfdx-scanner/pull/118)
- @W-7748199@ Strip comments from tsconfig.json before parsing [\#117](https://github.com/forcedotcom/sfdx-scanner/pull/117)
- @W-7736937@ Add Triggers to PMD glob list [\#116](https://github.com/forcedotcom/sfdx-scanner/pull/116)
- @W-7661099@: Added a few more lines of logging. [\#115](https://github.com/forcedotcom/sfdx-scanner/pull/115)
- @W-7396965@ Minor doc changes [\#114](https://github.com/forcedotcom/sfdx-scanner/pull/114)
- @W-7736591@ Use a temporary file to pass files to PMD [\#112](https://github.com/forcedotcom/sfdx-scanner/pull/112)
- @W-7732253@ Taking out the link for one of the questions [\#111](https://github.com/forcedotcom/sfdx-scanner/pull/111)
- @W-7716156@-Allow user to enable PMD's default rules for languages of their choice. [\#110](https://github.com/forcedotcom/sfdx-scanner/pull/110)
- @W-7732253@  Edit all \*.md files in the \_articles/en directory [\#109](https://github.com/forcedotcom/sfdx-scanner/pull/109)
- @W-7661051@ @W-7673639@ @W-7661091@ Eslint changes [\#108](https://github.com/forcedotcom/sfdx-scanner/pull/108)
- @W-7452168@-When two PMD rule JARs have files with the same relative paths, the rules defined in one are omitted from the cataloger [\#107](https://github.com/forcedotcom/sfdx-scanner/pull/107)
- Small formatting changes for FAQs [\#105](https://github.com/forcedotcom/sfdx-scanner/pull/105)
- Added helpful FAQ. [\#104](https://github.com/forcedotcom/sfdx-scanner/pull/104)
- @W-7660937@ --json option for Run command should hold usable data [\#103](https://github.com/forcedotcom/sfdx-scanner/pull/103)
- @W-7687751@: ESLint results are only reported if they are actually me… [\#102](https://github.com/forcedotcom/sfdx-scanner/pull/102)
- @@W-7396965@ Adding a logo and some small formatting of the header [\#101](https://github.com/forcedotcom/sfdx-scanner/pull/101)
- Fixing some grammer and also adding a line for PMD version during com… [\#100](https://github.com/forcedotcom/sfdx-scanner/pull/100)
- @W-7660921@: Switched to absolute paths and added tests. [\#99](https://github.com/forcedotcom/sfdx-scanner/pull/99)
- @@W-7396965@ - Demo gif and Doc updates [\#98](https://github.com/forcedotcom/sfdx-scanner/pull/98)
- Bump websocket-extensions from 0.1.3 to 0.1.4 [\#96](https://github.com/forcedotcom/sfdx-scanner/pull/96)
- Doc site updates with install and command references + update CLI plu… [\#95](https://github.com/forcedotcom/sfdx-scanner/pull/95)
- @W-75854542@ - Basic Structure for Sfdx Scanner Doc site [\#94](https://github.com/forcedotcom/sfdx-scanner/pull/94)
- @W-7559673@ Git2Gus config changes on dev branch [\#91](https://github.com/forcedotcom/sfdx-scanner/pull/91)
- Renamed code-samples to code-fixtures [\#88](https://github.com/forcedotcom/sfdx-scanner/pull/88)
- W-7443194: Phase 2 of multi-engine story.  Collection of fixes and better support for tsconfig. [\#87](https://github.com/forcedotcom/sfdx-scanner/pull/87)
- Eslint Engine support [\#85](https://github.com/forcedotcom/sfdx-scanner/pull/85)
- IOC part 2 [\#84](https://github.com/forcedotcom/sfdx-scanner/pull/84)
- IOC to abstract rule engines from rule manager code [\#83](https://github.com/forcedotcom/sfdx-scanner/pull/83)
- @W-7434792@ Delete a custom rule library I previously added [\#82](https://github.com/forcedotcom/sfdx-scanner/pull/82)
- Changed all spaces to tabs. [\#81](https://github.com/forcedotcom/sfdx-scanner/pull/81)
- Adding long descriptions to commands and flags - draft messages that'l… [\#80](https://github.com/forcedotcom/sfdx-scanner/pull/80)
- Missing log lines, additions to PrettyPrinter [\#79](https://github.com/forcedotcom/sfdx-scanner/pull/79)

## [v1.0.30](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.30) (04-03-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.29...v1.0.30)

**Merged pull requests:**

- Fix some missing doc [\#76](https://github.com/forcedotcom/sfdx-scanner/pull/76)

## [v1.0.29](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.29) (04-03-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.28...v1.0.29)

**Merged pull requests:**

- Temporarily replacing the postinstall ux invocations with console.log… [\#75](https://github.com/forcedotcom/sfdx-scanner/pull/75)
- We now untildify the target provided to scanner:run. [\#74](https://github.com/forcedotcom/sfdx-scanner/pull/74)

## [v1.0.28](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.28) (04-02-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.27...v1.0.28)

## [v1.0.27](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.27) (04-02-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.26...v1.0.27)

**Merged pull requests:**

- Wrapped JreSetupManager in a postinstall-only class to hopefully prop… [\#73](https://github.com/forcedotcom/sfdx-scanner/pull/73)
- Decouple format from outfile [\#72](https://github.com/forcedotcom/sfdx-scanner/pull/72)

## [v1.0.26](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.26) (04-02-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.25...v1.0.26)

## [v1.0.25](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.25) (04-02-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.24...v1.0.25)

**Merged pull requests:**

- Junit format [\#71](https://github.com/forcedotcom/sfdx-scanner/pull/71)

## [v1.0.24](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.24) (04-02-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.23...v1.0.24)

## [v1.0.23](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.23) (04-01-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.22...v1.0.23)

## [v1.0.22](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.22) (04-01-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.21...v1.0.22)

**Merged pull requests:**

- W-7398711: Fixing run command on-success behavior [\#70](https://github.com/forcedotcom/sfdx-scanner/pull/70)
- Windows CI [\#69](https://github.com/forcedotcom/sfdx-scanner/pull/69)
- Fixing Java version check to ignore minor version if needed and not a… [\#68](https://github.com/forcedotcom/sfdx-scanner/pull/68)
- @W-7388308@ scanner:rule:add incorrectly handles relative paths. [\#67](https://github.com/forcedotcom/sfdx-scanner/pull/67)
- New PrettyPrinter module +  Adding trace logs for CustomRulePathManager, PmdWrapper, PmdCatalogWrapper [\#66](https://github.com/forcedotcom/sfdx-scanner/pull/66)
- Jf add help [\#65](https://github.com/forcedotcom/sfdx-scanner/pull/65)
- Jf help text [\#64](https://github.com/forcedotcom/sfdx-scanner/pull/64)
- Rm/friendly errors [\#62](https://github.com/forcedotcom/sfdx-scanner/pull/62)

## [v1.0.21](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.21) (03-27-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.20...v1.0.21)

**Merged pull requests:**

- Now we use normalize-path to allow us to support Windows-formatted pa… [\#63](https://github.com/forcedotcom/sfdx-scanner/pull/63)
- Rm/friendly errors [\#56](https://github.com/forcedotcom/sfdx-scanner/pull/56)

## [v1.0.20](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.20) (03-25-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.19...v1.0.20)

**Merged pull requests:**

- Have to move globby from devDeps to deps in order for things to work … [\#61](https://github.com/forcedotcom/sfdx-scanner/pull/61)

## [v1.0.19](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.19) (03-25-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.18...v1.0.19)

**Merged pull requests:**

- Switched to soft-coded path delimiter instead of hardcoded. [\#60](https://github.com/forcedotcom/sfdx-scanner/pull/60)

## [v1.0.18](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.18) (03-25-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.17...v1.0.18)

**Merged pull requests:**

- Changed the inner quotes to single quotes, since Windows did not seem… [\#58](https://github.com/forcedotcom/sfdx-scanner/pull/58)

## [v1.0.17](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.17) (03-25-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.16...v1.0.17)

**Merged pull requests:**

- Replacing single-quotes with double-quotes, which could fix Windows c… [\#57](https://github.com/forcedotcom/sfdx-scanner/pull/57)

## [v1.0.16](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.16) (03-24-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.15...v1.0.16)

**Merged pull requests:**

- Incremented to v1.0.16. [\#55](https://github.com/forcedotcom/sfdx-scanner/pull/55)
- @W-7309176@ Support globs in file pattern arguments [\#54](https://github.com/forcedotcom/sfdx-scanner/pull/54)
- Java changes to replace exit code with messages + some unit tests [\#53](https://github.com/forcedotcom/sfdx-scanner/pull/53)

## [v1.0.15](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.15) (03-23-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.14...v1.0.15)

**Merged pull requests:**

- Removed Author column from the results of scanner:rule:list, since it… [\#52](https://github.com/forcedotcom/sfdx-scanner/pull/52)
- Managing Config in our own way instead of using ConfigFile [\#50](https://github.com/forcedotcom/sfdx-scanner/pull/50)

## [v1.0.14](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.14) (03-20-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.13...v1.0.14)

**Merged pull requests:**

- Renamed the --source flag to --target so it is clearer and can have a… [\#51](https://github.com/forcedotcom/sfdx-scanner/pull/51)
- Jf filter adjust [\#49](https://github.com/forcedotcom/sfdx-scanner/pull/49)

## [v1.0.13](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.13) (03-19-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.12...v1.0.13)

**Merged pull requests:**

- Fixing dependencies. [\#48](https://github.com/forcedotcom/sfdx-scanner/pull/48)

## [v1.0.12](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.12) (03-19-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.11...v1.0.12)

**Merged pull requests:**

- Adding dist folder to our package, and incrementing version. [\#47](https://github.com/forcedotcom/sfdx-scanner/pull/47)

## [v1.0.11](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.11) (03-19-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.10...v1.0.11)

**Merged pull requests:**

- Rm/update pmd version [\#46](https://github.com/forcedotcom/sfdx-scanner/pull/46)

## [v1.0.10](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.10) (03-19-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.9...v1.0.10)

**Merged pull requests:**

- Fixing dependencies, adjusting version number. [\#45](https://github.com/forcedotcom/sfdx-scanner/pull/45)

## [v1.0.9](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.9) (03-19-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/v1.0.8...v1.0.9)

**Merged pull requests:**

- Incrementing to version 1.0.9. [\#44](https://github.com/forcedotcom/sfdx-scanner/pull/44)
- Adding absolute path to run Java command [\#43](https://github.com/forcedotcom/sfdx-scanner/pull/43)
- Jf automated deploy2 [\#42](https://github.com/forcedotcom/sfdx-scanner/pull/42)

## [v1.0.8](https://github.com/forcedotcom/sfdx-scanner/tree/v1.0.8) (03-18-2020)

[Full Changelog](https://github.com/forcedotcom/sfdx-scanner/compare/tag-test-v2...v1.0.8)

**Merged pull requests:**

- JreSetupManager unit tests [\#41](https://github.com/forcedotcom/sfdx-scanner/pull/41)
- Incremented version number so deploy will work right, changed some he… [\#40](https://github.com/forcedotcom/sfdx-scanner/pull/40)
- Rm/verify jre [\#39](https://github.com/forcedotcom/sfdx-scanner/pull/39)
- Renamed package to @salesforce/sfdx-scanner, and incremented version … [\#38](https://github.com/forcedotcom/sfdx-scanner/pull/38)
- Adding junit tests for Json creation [\#36](https://github.com/forcedotcom/sfdx-scanner/pull/36)
- Refactor to honor classpath rules [\#35](https://github.com/forcedotcom/sfdx-scanner/pull/35)
- @W-7307977@ Ignore XML files that are not category/ruleset format [\#34](https://github.com/forcedotcom/sfdx-scanner/pull/34)
- Pretty print catalog.json [\#32](https://github.com/forcedotcom/sfdx-scanner/pull/32)
- Jf env vars [\#31](https://github.com/forcedotcom/sfdx-scanner/pull/31)
- Jf catalog relocation [\#30](https://github.com/forcedotcom/sfdx-scanner/pull/30)
- Add command: returning user information for non --json calls [\#29](https://github.com/forcedotcom/sfdx-scanner/pull/29)
- Added tests for RuleManager.ts that use a mockup of data. [\#28](https://github.com/forcedotcom/sfdx-scanner/pull/28)
- Rmohan/tests and cleanup [\#27](https://github.com/forcedotcom/sfdx-scanner/pull/27)
- Hid unused flags and removed defunct scanner:scan command. [\#26](https://github.com/forcedotcom/sfdx-scanner/pull/26)
- Changed tests so that they will always temporarily rename the custom … [\#25](https://github.com/forcedotcom/sfdx-scanner/pull/25)
- Typescript handoff and Java side changes [\#24](https://github.com/forcedotcom/sfdx-scanner/pull/24)
- Jf rm/revisit add [\#23](https://github.com/forcedotcom/sfdx-scanner/pull/23)
- Linting and updating circleci config for test reporting [\#21](https://github.com/forcedotcom/sfdx-scanner/pull/21)
- linting [\#20](https://github.com/forcedotcom/sfdx-scanner/pull/20)
- @W-7256572@: Implemented the scanner:rule:list tests. [\#19](https://github.com/forcedotcom/sfdx-scanner/pull/19)
- Add unit tests [\#18](https://github.com/forcedotcom/sfdx-scanner/pull/18)
- @W-7246358@: Refactored PmdSupport and subclasses to use child\_proces… [\#17](https://github.com/forcedotcom/sfdx-scanner/pull/17)
- @W-7159179@ Implement scanner plugin's 'describe' command. [\#15](https://github.com/forcedotcom/sfdx-scanner/pull/15)
- @W-7159188@ Implement scanner plugin's 'run' command. [\#13](https://github.com/forcedotcom/sfdx-scanner/pull/13)
- Refactoring pmd support [\#12](https://github.com/forcedotcom/sfdx-scanner/pull/12)
- PMD support: Moved java code under submodule, added gradle build, fixed tsc errors [\#11](https://github.com/forcedotcom/sfdx-scanner/pull/11)
- @W-7159171@: Reorganized packages once again, rebuilt JAR, changed pa… [\#10](https://github.com/forcedotcom/sfdx-scanner/pull/10)
- @W-7159171@ Implement scanner plugin's 'list' command. [\#9](https://github.com/forcedotcom/sfdx-scanner/pull/9)
- Fixing PMD download [\#8](https://github.com/forcedotcom/sfdx-scanner/pull/8)
- @W-7109964@: Relocating list and describe commands as per feedback fr… [\#7](https://github.com/forcedotcom/sfdx-scanner/pull/7)
- @W-7109964@ Static Analyzer CLI plugin [\#6](https://github.com/forcedotcom/sfdx-scanner/pull/6)
- @W-7109964@: Fixed one bit of documentation. [\#5](https://github.com/forcedotcom/sfdx-scanner/pull/5)
- @W-7109964@: Changed some stuff to fix type errors and runtime errors. [\#4](https://github.com/forcedotcom/sfdx-scanner/pull/4)
- @W-7109964@ Static Analyzer CLI plugin [\#3](https://github.com/forcedotcom/sfdx-scanner/pull/3)
- @W-7109964@ Static Analyzer CLI plugin [\#2](https://github.com/forcedotcom/sfdx-scanner/pull/2)
- Auto-generated SFDC CLI Plugin + PMD integration draft [\#1](https://github.com/forcedotcom/sfdx-scanner/pull/1)



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*

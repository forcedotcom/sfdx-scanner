---
title: Install Salesforce Scanner CLI Plugin
lang: en
---

## Install the plug-in

Like all the Salesforce CLI plug-ins, you install the Scanner CLI plug-in with one simple step. NOTE: Be sure you've completed the [prerequisites](./en/getting-started/prerequisites/) before you install this plug-in.


```bash
$ sfdx plugins:install @salesforce/sfdx-scanner
This plugin is not digitally signed and its authenticity cannot be verified. Continue installation y/n?: y
Finished digital signature check.
Installing plugin @salesforce/sfdx-scanner...
installed v1.0.30 
```

#### Check that the scanner plug-in is installed
```bash
$ sfdx plugins
@salesforce/sfdx-scanner 1.0.30
```

#### Display the usage and help for the scanner commands
```bash
$ sfdx scanner --help
Scan code to detect code quality issues and security vulnerabilities.

USAGE
  $ sfdx scanner:COMMAND

COMMANDS
  scanner:run  Evaluate a selection of rules against a codebase.

TOPICS
  Run help for each topic below to view subcommands

  scanner:rule  View/add rules that are used to scan code.
  
```

## CI/CD

To insure code quality rules are respected, even if developers did not install or run sfdx-scanner locally, you can setup sfdx-scanner in your CI pipelines (Github actions, Circle CI, Travis, Jenkins...)

### Mega-Linter

[sfdx-scanner](https://nvuillam.github.io/mega-linter/descriptors/salesforce_sfdx_scanner/) is natively included in [Mega-Linter](https://nvuillam.github.io/mega-linter/) (with 70 other linters). Add it in your pipeline, and it will be automatically run.

### Manually

Add `sfdx scanner:run --violations-cause-error` in your pipeline definition

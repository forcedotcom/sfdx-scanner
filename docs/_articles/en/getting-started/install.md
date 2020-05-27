---
title: Install Salesforce Scanner CLI Plugin
lang: en
---

## Install the plugin

Similar to all the SFDX CLI plugins, the Scanner CLI plugin can be installed with one simple step. Please note: you must have installed the [prerequisites](./en/getting-started/prerequisites/) prior to the installation of this plugin.


```bash
$ sfdx plugins:install @salesforce/sfdx-scanner
This plugin is not digitally signed and its
authenticity cannot be verified. Continue
installation y/n?: y
Finished digital signature check.
Installing plugin @salesforce/sfdx-scanner...
installed v1.0.30 
```

#### Check to make sure that the scanner plugin is installed
```bash
$ sfdx plugins
@salesforce/sfdx-scanner 1.0.30
```

#### To see the usage and help for the scanner commands
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
## Demo

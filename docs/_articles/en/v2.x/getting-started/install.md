---
title: Install Salesforce Code Analyzer
lang: en
---

## Install Salesforce Code Analyzer

**NOTE**: Complete the [prerequisites](./en/v2.x/getting-started/prerequisites/) before you install this plug-in

Install Salesforce Code Analyzer (Code Analyzer) with this simple line of code.

```bash
$ sfdx plugins:install @salesforce/sfdx-scanner
Installing plugin @salesforce/sfdx-scanner...
installed v{{ site.data.versions-v2.scanner }} 
```

#### To check that Code Analyzer is installed, run this command.
```bash
$ sfdx plugins
@salesforce/sfdx-scanner {{ site.data.versions-v2.scanner }}
```
#### To install a specific Code Analyzer version, run this command.
```bash
$ sfdx plugins:install @salesforce/sfdx-scanner@{{ site.data.versions-v2.scanner }}
Installing plugin @salesforce/sfdx-scanner... 
installed v{{ site.data.versions-v2.scanner }}
```

#### To display Code Analyzer usage and help, run this command.
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

## Upgrade Code Analyzer
To update Code Analyzer to the latest version, run this command.

```bash
$ sfdx plugins:update
sfdx-cli: Updating plugins... done

$ sfdx plugins:update --help
update installed plugins

USAGE
  $ sfdx plugins:update

OPTIONS
  -h, --help     show CLI help
  -v, --verbose

```

## See Also

- [Salesforce CLI Setup Guide: Update Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_update_cli.htm#sfdx_setup_update_cli)
- [Salesforce CLI Setup Guide: Troubleshoot Salesforce  CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_troubleshoot.htm)



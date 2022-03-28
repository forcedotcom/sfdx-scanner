---
title: Install Salesforce Code Analyzer Plugin
lang: en
---

## Install the plug-in

Like all the Salesforce CLI plug-ins, you install the Salesforce Code Analyzer CLI plug-in with one simple step. NOTE: Be sure you've completed the [prerequisites](./en/getting-started/prerequisites/) before you install this plug-in.


```bash
$ sfdx plugins:install @salesforce/sfdx-scanner
Installing plugin @salesforce/sfdx-scanner...
installed v{{ site.data.versions.scanner }} 
```

#### Check that the Analyzer plug-in is installed
```bash
$ sfdx plugins
@salesforce/sfdx-scanner {{ site.data.versions.scanner }}
```
#### Install or upgrade to a specific version using the following command
```bash
$ sfdx plugins:install @salesforce/sfdx-scanner@{{ site.data.versions.scanner }}
Installing plugin @salesforce/sfdx-scanner... 
installed v{{ site.data.versions.scanner }}
```

#### Display the usage and help for the Analyzer commands
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

## Upgrade plug-in
To update the Analyzer plug-in to the latest version, you can follow the next step.

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

For more information on how to manage the installed plugins or Auto Update, visit [CLI help](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_update_cli.htm#sfdx_setup_update_cli)


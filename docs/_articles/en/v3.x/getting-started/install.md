---
title: Install and Update Salesforce Code Analyzer
lang: en
redirect_from: /en/getting-started/install
---

## Install Salesforce Code Analyzer v3.x

**NOTE**: Complete the [prerequisites](./en/v3.x/getting-started/prerequisites/) before you install Salesforce Code Analyzer. 
	
Install Salesforce Code Analyzer (Code Analyzer) with this simple line of code.

```bash
$ sf plugins install @salesforce/sfdx-scanner
```
By default, `latest` tag is installed: {{ site.data.versions-v3.scanner }}. 

#### To check that Code Analyzer is installed, run this command.

```bash
$ sf plugins
@salesforce/sfdx-scanner {{ site.data.versions-v3.scanner }}
```

#### To install a specific Code Analyzer version, run this command.

```bash
$ sf plugins install @salesforce/sfdx-scanner@latest-pilot
Installing plugin @salesforce/sfdx-scanner... 
installed v{{ site.data.versions-v3.scanner }}
``` 

#### To display Code Analyzer usage and help, run this command.

```bash
$ sf scanner --help
Scan code to detect code quality issues and security vulnerabilities.

USAGE
  $ sf scanner COMMAND

COMMANDS
  scanner run  scan a codecase with a selection of rules.
  scanner rule  View/add rules that are used to scan code.

```
## Update Code Analyzer v3.x

To update Code Analyzer, run this command.

```bash
$ sf plugins update
@salesforce/cli: Updating plugins... done
```
## Uninstall Code Analyzer v3.x

To uninstall Code Analyzer, run this command.

```bash
sf plugins uninstall @salesforce/sfdx-scanner
```

## See Also

[Salesforce CLI Setup Guide: Update Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_update_cli.htm#sfdx_setup_update_cli)

[Salesforce CLI Setup Guide: Troubleshoot Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_troubleshoot.htm)

---
title: Install Salesforce Code Analyzer
lang: en
---

## Install Salesforce Code Analyzer v3.x

**NOTE**: Complete the [prerequisites](./en/getting-started/prerequisites/) before you install this plug-in. Automatic plug-in upgrades don't work with the pilot version 3.x.

<div class="slds-notify slds-notify_alert slds-theme_alert-texture slds-text-heading_small slds-theme_warn" role="alert">
  <span class="slds-assistive-text">warn</span>
The new major version {{ site.data.versions-v3.scanner }}  is a pilot and is not installed by default.
</div>
	
The first time you execute a ```v2.x``` command, a ```config.json``` file is automatically
created. When you upgrade to a new version 2.x or to pilot version 3.x, your original ```v2.x config.json``` persists. 

#### To install the `latest-pilot`, use these instructions:
	
Install Salesforce Code Analyzer (Code Analyzer) with this simple line of code.

```bash
$ sfdx plugins:install @salesforce/sfdx-scanner
Installing plugin @salesforce/sfdx-scanner...
installed v{{ site.data.versions-v2.scanner }} 
```
By default, `latest` tag is installed: {{ site.data.versions-v2.scanner }}. Install the ```v3.x``` pilot version by pointing to the `latest-pilot` tag. 

#### To install or upgrade to a specific version of Code Analyzer:

```bash
$ sfdx plugins:install @salesforce/sfdx-scanner@latest-pilot
Installing plugin @salesforce/sfdx-scanner... 
installed v{{ site.data.versions-v3.scanner }}
``` 

#### To check that Code Analyzer is installed, run this command:

```bash
$ sfdx plugins
@salesforce/sfdx-scanner {{ site.data.versions-v3.scanner }}
```

#### To display usage and help for Code Analyzer commands, run this command:

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

#### To upgrade Code Analyzer:

Because Code Analyzer ```v3.x``` is a pilot version, you must uninstall Code Analyzer and reinstall.

#### To uninstall Code Analyzer:

```bash
sfdx plugins:uninstall @salesforce/sfdx-scanner
```

#### To revert to Code Analyzer version 2.x:

Uninstall version 3.x and follow the [installation steps](./en/v2.x/getting-started/install/#install-the-plug-in).

If you made any manual changes to the ```{{ site.data.versions-v3.configfile }}``` file,
and you wish for those changes to apply in ```v2.x```, you'll need to replicate them in the ```Config.json``` file after reverting.

---
title: Install Salesforce Code Analyzer Plugin
lang: en
---

## Install v3.x

Like all the Salesforce CLI plug-ins, you install the Salesforce Code Analyzer CLI plug-in with one simple step. NOTE: Be sure you've completed the [prerequisites](.v3.x/getting-started/prerequisites/) before you install this plug-in.


By default, `latest` tag gets installed and as of today, it still points to {{ site.data.versions.scanner }}. You can install the pilot version by pointing to the `latest-pilot` tag. Also, note that automatic plugin upgrades don't work here.

<div class="slds-notify slds-notify_alert slds-theme_alert-texture slds-text-heading_small slds-theme_warn" role="alert">
  <span class="slds-assistive-text">warn</span>
The new major version {{ site.data.versions-v3.scanner }}  is still a pilot and is not installed by default.
<br>
Please be aware that there could be minor issues that we are still fixing.
</div>
<br>


Steps to install the `latest-pilot` version are outlined below.

#### Uninstall any existing Salesforce Code Analyzer plugin
```bash
sfdx plugins:uninstall @salesforce/sfdx-scanner
```

#### Install or upgrade to a specific version using the following command
```bash
$ sfdx plugins:install @salesforce/sfdx-scanner@latest-pilot
Installing plugin @salesforce/sfdx-scanner... 
installed v{{ site.data.versions-v3.scanner }}
``` 

#### Check that the Analyzer plug-in is installed
```bash
$ sfdx plugins
@salesforce/sfdx-scanner {{ site.data.versions-v3.scanner }}
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

#### Upgrade plug-in
Automatic steps don't apply here since this is a pilot version. Follow steps from above to uninstall and install directly again.

#### Reverting to GA version
Uninstall existing plugin and follow installation steps [here](./en/getting-started/install/#install-the-plug-in).
---
title: What's new in 3.x?
lang: en
---

## What’s new in 3.x?

TODO: (Section on changes to existing engines)



<!--todo: make this text pop out -->
## Installing version 3.x
By default, latest tag gets installed and as of today, it still points to {{ site.data.versions.scanner }}.

To specifically install the latest {{ site.data.versions-v3.scanner }} version:

1. Uninstall existing Salesforce Code Analyzer plugin: 
```
sfdx plugins:uninstall @salesforce/sfdx-scanner
```
2. Install the pilot tag of the same plugin: 
```
sfdx plugins:install @salesforce/sfdx-scanner@pilot
```
3. Verify correct version was installed: sfdx plugins
    a. @salesforce/sfdx-scanner should point to {{ site.data.versions-v3.scanner }}


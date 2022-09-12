---
title: Salesforce Code Analyzer Plug-In
permalink: /
lang: en
---
<div class="slds-notify slds-notify_alert slds-theme_alert-texture slds-text-heading_small slds-theme_info" role="alert">
  <span class="slds-assistive-text">success</span>
    We're constantly improving Salesforce Code Analyzer. Tell us what you think!
    &nbsp;&nbsp;
	<a href="https://research.net/r/SalesforceCA" target="_blank"><button class="slds-button slds-button_brand">Give Feedback</button></a>
</div>
<br>
<div class="slds-notify slds-notify_alert slds-theme_alert-texture slds-text-heading_small slds-theme_success" role="alert">
  <span class="slds-assistive-text">success</span>
  	{{ site.data.versions-v3.releasedon }}: New pilot version {{ site.data.versions-v3.scanner }} of Salesforce Code Analyzer is available
	&nbsp;&nbsp;
	<a href="./en/v3.x/whats-new-v3/">Check out what's new</a>
</div>
<br>
<div class="slds-notify slds-notify_alert slds-theme_alert-texture slds-text-heading_small slds-text-align_center slds-theme_warning" role="alert">
  <span class="slds-assistive-text">warning</span>
  	{{ site.data.versions.releasedon }}: The latest version v{{ site.data.versions.scanner }} is available&nbsp;&nbsp;<a href="./en/release-information/">Release Information</a>
</div>
<br>
<div class="slds-notify slds-notify_alert slds-theme_alert-texture slds-text-heading_small slds-text-align_center slds-theme_warning" role="alert">
  <span class="slds-assistive-text">warning</span>
<p>
  	Support and updates for Code Analyzer v2.x are scheduled to end as of the October 26, 2022 release.
  	You can continue to use v2.x after the October release; however, we recommend using Code Analyzer v3.x instead.
 	For more information, see <a href="./en/v3.x/getting-started/prerequisites/">Getting Started with v3.x.</a>
</p>
</div>
<br>

## Salesforce Code Analyzer Plug-in

The Salesforce Code Analyzer plug-in is a unified tool for static analysis of source code, in multiple languages
(including Apex), with a consistent command-line interface and report output. We currently support the
[PMD rule engine](https://pmd.github.io/), [PMD Copy Paste Detector](https://pmd.github.io/latest/pmd_userdocs_cpd.html), [ESLint](https://eslint.org/), and [RetireJS](https://retirejs.github.io/retire.js/).
We may add support for more rule engines in the future.

The Salesforce Code Analyzer Plug-in creates "Rule Violations" when the it identifies issues. Developers use this information as feedback to fix their code. 

You can integrate this plug-in into your CI/CD solution to enforce the rules and expect high-quality code.

## Additional Resources

- Trailhead: [Get Started with Salesforce DX](https://trailhead.salesforce.com/trails/sfdx_get_started)
- [Salesforce DX Setup Guide](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup)
- [Salesforce DX Developer Guide](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev)

### Open Source

- [Github Repository](https://github.com/forcedotcom/sfdx-scanner)

## Bugs and Feedback

To report issues with Salesforce Code Analyzer, open a [bug on GitHub](https://github.com/forcedotcom/sfdx-scanner/issues/new?template=Bug_report.md). If you want to suggest a feature, create a [feature request on GitHub](https://github.com/forcedotcom/sfdx-scanner/issues/new?template=Feature_request.md).



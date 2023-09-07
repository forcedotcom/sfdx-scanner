---
title: Salesforce Code Analyzer Visual Studio Code Extension (Beta)
lang: en
redirect_from: /en/code-analyzer-vs-code-extension
---

The Salesforce Code Analyzer (Code Analyzer) Visual Studio (VS) Code extension is an extension that integrates many of Code Analyzer’s most useful features into VS Code, allowing them to be easily run with clicks instead of terminal commands.

> **_NOTE:_** If you’re listing a managed package on AppExchange, it must pass security review. You’re also required to upload your Salesforce Code Analyzer scan reports. Run Code Analyzer via the VS Code extension and update your code. Next, to produce the required scan reports for your AppExchange listing, you must run Code Analyzer via the command line either within VS Code or as standalone. Attach your scan reports to your submission in the AppExchange Security Review Wizard. Read [Scan Your Solution with Salesforce Code Analyzer](https://developer.salesforce.com/docs/atlas.en-us.packagingGuide.meta/packagingGuide/security_review_code_analyzer_scan.htm) for details.

## Using Code Analyzer VS Code Extension (beta)

Use Code Analyzer VS Code extension (beta) to scan multiple languages:

* [PMD rule engine](https://pmd.github.io/)
* [RetireJS](https://retirejs.github.io/retire.js/)
* [Salesforce Graph Engine](./en/v3.x/salesforce-graph-engine/introduction/) (Generally Available rules only)

You can also [enable](./en/v3.x/faq/#q-how-do-i-enable-engine-xs-default-rules-for-language-y) these languages in Salesforce Code Analyzer settings:

* Java
* XML code

## Set Up

* Install [Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_install_cli.htm).
* In your terminal, run `sfdx plugins:install @salesforce/sfdx-scanner`. Make sure that you're running Code Analyzer version {{site.data.versions-v3.extensioncompatiblescanner}} or later.
* Install [Salesforce Code Analyzer VS Code extension(beta)](https://marketplace.visualstudio.com/items?itemName=salesforce.sfdx-code-analyzer-vscode).

## Contribute to Salesforce Code Analyzer VS Code Extension

To report issues with the Salesforce Code Analyzer VS Code Extension, create a [bug on Github](https://github.com/forcedotcom/sfdx-code-analyzer-vscode/issues/new?assignees=&labels=&projects=&template=bug_report.md&title=%5BBUG%5D). To suggest a feature enhancement, create a [request on Github](https://github.com/forcedotcom/sfdx-code-analyzer-vscode/issues/new?assignees=&labels=&projects=&template=feature_request.md&title=%5BFeature+Request%5D).

## Launch Code Analyzer Extension and Scan Your Code

Complete these steps to launch the Code Analyzer extension and scan your code.

1. Open your project in VS Code.
2. Scan your code with Code Analyzer.
3. Update your code based on the findings.
4. Rescan your code. 
5. Scan individual methods within your code with Code Analyzer’s Graph Engine path-based analysis. 
6. Rescan your code with Graph Engine.
7. If you’re listing a managed package on AppExchange, follow the instructions in [Scan Your Solution with Code Analyzer](https://developer.salesforce.com/docs/atlas.en-us.packagingGuide.meta/packagingGuide/security_review_code_analyzer_scan.htm) to produce the required scan reports.

### Code Analyzer Scans

Complete one of these options to perform a Code Analyzer scan.

* To scan selected files or folders:

1. Select a group of files or folders.
2. Right click in the VS Code Explorer and select **SFDX: Scan selected files or folders with Code Analyzer**.

* To scan a single code file:

1. Open a code file in the VS Code Editor.
2. From the VS Code Command Palette, choose **SFDX: Scan current file with Code Analyzer**.
3. Alternatively, right click in the VS Code Editor and select **SFDX: Scan current file with Code Analyzer**.

Regardless of which option you chose, the progress bar notifies you that the scan of your current file is active.

![The VS Code progress bar displaying a Code Analyzer is analyzing targets message.](./assets/images/vscode-images/AnalyzingTargets.png)

After your scan is complete, note how many files were scanned and how many violations were produced.

![The VS Code progress bar displaying a Scanned 1 files, 7 violations found in 1 files completion message.](./assets/images/vscode-images/CodeAnalyzerViolationsProgressBar.png)

#### Address Your Code Analyzer Results and Rescan Your Code

When your scan is complete, click the scan summary in the progress bar (1). You see a scrollable list of violations that Code Analyzer found (2).

![alt text: Sample VS Code code and Salesforce Code Analyzer scan results](./assets/images/vscode-images/ScanSummary.png)

Each violation message reveals the violation severity and details about the violation found in this pattern: `SevX: [Violation message]`

*Example:*

```Sev3. Validate CRUD permission before SOQL/DML operation or enforce user mode. (PMD via Code Analyzer)```

To address the violations found and to rescan your code, follow these steps:

1. Scroll through the results that Code Analyzer found.
2. Update your code directly in VS Code.
3. When your edits are complete, rescan your code using your preferred method.

#### Produce Code Analyzer Reports for AppExchange Security Review

If you’re an AppExchange partner submitting your managed package for security review, you must scan it with Salesforce Code Analyzer and provide test results in your solution’s AppExchange Security Review submission. To produce the required reports, follow the instructions in [Scan Your Solution with Salesforce Code Analyzer](https://developer.salesforce.com/docs/atlas.en-us.244.0.packagingGuide.meta/packagingGuide/security_review_code_analyzer_scan.htm).

#### Use a Quick Fix to Suppress a Code Analyzer PMD Violation

After you scan your code with Code Analyzer, there can be situations where you want to suppress a PMD violation that was identified. 

To use a quick fix to suppress a PMD violation on a line of code, complete these steps.

1. Hover over the identified problem.
2. Click **Quick Fix** in the pop-up.
3. Click **Suppress violations on this line**.

#### Replace Code Analyzer’s PMD Config File with a Custom PMD Configg

By default, Code Analyzer runs all of PMD's default rules against your Apex and VisualForce files. However, if you have a custom PMD configuration that better suits your needs, you can substitute your configuration in place of ours.

To use your custom PMD ruleset in Code Analyzer, complete these steps.

1. In VS Code, click **Extensions**.
2. Select **Salesforce Code Analyzer VS Code Extension**.
3. Click **Settings**.
4. Click **Extension Settings**. 
5. Click the **User** or **Workspace** tab (1).

	* To override the configuration on the current project, choose **Workspace**.
	* To override the configuration in all projects, select **User**.

6. In Code Analyzer > PMD > Custom Config File (2), enter the absolute path to your custom PMD configuration.

	*Example*: /Users/MyUsername/Code/sfdx-scanner/

![alt text: Salesforce Code Analyzer Settings with Code Analyzer > PMD Custom Config File section and a sample file location](./assets/images/vscode-images/SettingsTwoBubbles.png)

### Salesforce Graph Engine

To perform a Graph Engine path-based analysis on a single method complete these steps.

1. Open a file in the VS Code Editor.
2. Right-click on the method that you want to scan.
3. Select **SFDX: Scan selected method with Graph Engine path-based analysis**.

The progress bar notifies you that the scan of your current file is active.

![alt text: The VS Code progress bar displaying a Running Graph Engine analysis notification.](./assets/images/vscode-images/GraphEngineRunningAnalysis.png)

#### Address Your Graph Engine Results and Rescan Your Code

When your scan is complete, a new tab opens with an HTML display of the violations found.

![alt text: A sample Salesforce Graph Engine pop-up window with an html list of violations found](./assets/images/vscode-images/GraphEngineResultsBlur.png)

Each violation message reveals the violation severity and details about the violation.

**Examples**:

> Multiple expensive schema lookups are invoked. `[Schema.describeSObjects at AuraEnabledFls:27]`

> A database operation occurred inside a loop. `[%s at %s:%d]`

To address the violations found and to rescan your code, complete these steps.

1. On the VS Code tab with your Graph Engine results, review the violations that Graph Engine found.
2. On the VS Code tab with your code file open, update your code.
3. When your edits are complete, rescan your code.

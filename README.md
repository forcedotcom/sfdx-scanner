[![License](https://img.shields.io/npm/l/scanner.svg)](https://github.com/forcedotcom/code-analyzer/blob/master/package.json)

# Salesforce Code Analyzer
Salesforce Code Analyzer is a unified tool for static analysis of source code.
Code Analyzer analyzes multiple languages including Apex, JavaScript, HTML, and CSS, and Salesforce metadata such as Flows.
It relies on a consistent command-line interface and produces a results file of rule violations.
Use the results to review and improve your code.

If you're listing a managed package on AppExchange, it must pass security review.
You're also required to upload your Salesforce Code Analyzer scan reports.
Attach your Code Analyzer reports to your submission in the AppExchange Security Review Wizard.
For more info, read [Scan Your Code with Salesforce Code Analyzer](https://developer.salesforce.com/docs/atlas.en-us.packagingGuide.meta/packagingGuide/security_review_code_analyzer_scan.htm) and [AppExchange Security Review](https://developer.salesforce.com/docs/atlas.en-us.packagingGuide.meta/packagingGuide/security_review_overview.htm).

Integrate Code Analyzer into your Continuous Integration/Continuous Development (CI/CD) process to enforce rules that you define and to produce high-quality code. Salesforce provides an official [Github Action](https://github.com/marketplace/actions/run-salesforce-code-analyzer) to assist with this.

# Salesforce Code Analyzer Documentation
Read [Salesforce Code Analyzer](https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/overview) documentation to learn:
* how to install Code Analyzer
* what is included in the Code Analyzer command reference
* what rules are included from code-scanning engines such as PMD and ESLint
* how to write and manage custom rules
* how to set up your CI/CD process with Code Analyzer
* how to see review code violations with the Code Analyzer VS Code Extension
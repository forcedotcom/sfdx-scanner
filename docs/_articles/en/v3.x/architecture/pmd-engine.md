---
title: 'PMD'
lang: en
redirect_from: /en/architecture/pmd-engine
---
## What is PMD?
PMD is a source code analyzer that allows for static analysis of code written in a number of supported languages, including Java, Apex, and Visualforce. Its built-in rules detect common flaws in code, such as empty catch blocks or unused variables.

## How does Salesforce Code Analyzer (code Analyzer) use PMD?
By default, the Code Analyzer ```scanner run``` command executes PMD’s default Apex and Visualforce rules against compatible files. 

You can change which rules are executed by using the flags described in the Code Analyzer Command Reference.

Refer to our [FAQ](./en/v3.x/faq/#q-how-do-i-enable-engine-xs-default-rules-for-language-y) for info on how to enable PMD’s built-in rules for other languages.

## How do I use pmd-appexchange to prepare my solution for an AppExchange security review?#

In addition to the base PMD engine, Code Analyzer also includes a custom PMD variant, `pmd-appexchange`. The rules included in `pmd-appexchange` help AppExchange partners prepare their managed packages for security review.

The `pmd-appexchange` engine is disabled by default. To run a PMD scan with the AppExchange-specific ruleset, run `sf scanner run` with the `--engine pmd-appexchange` flag.

**Example:**

`sf scanner run --engine pmd-appexchange --target ./`

For more information on the `pmd-appexchange` rules, read the [pmd-appexchange command reference](https://github.com/forcedotcom/sfdx-scanner/tree/dev/pmd-appexchange).

If you’re an AppExchange partner submitting your managed package for security review, you must scan it with Salesforce Code Analyzer and provide test results in your solution’s AppExchange Security Review submission. To run the required PMD scan with the AppExchange-specific ruleset, and produce the required scan report, run `sf scanner run --engine pmd-appexchange`, and name the output file CodeAnalyzerPmdAppExchange.csv.

**Example:**

`sf scanner run --engine pmd-appexchange --format=csv --outfile=CodeAnalyzerPmdAppExchange.csv --target="./"`

For full instructions on preparing for the AppExchange security review with Code Analyzer, read [Scan Your Solution with Salesforce Code Analyzer](https://developer.salesforce.com/docs/atlas.en-us.packagingGuide.meta/packagingGuide/security_review_code_analyzer_scan.htm) in the ISVforce Guide.

## See Also

- [PMD](https://pmd.github.io/#home)
- [pmd-appexchange command reference](https://github.com/forcedotcom/sfdx-scanner/tree/dev/pmd-appexchange)
- [Salesforce Code Analyzer: Authoring Custom Rules](./en/v3.x/custom-rules/author/)
- [Salesforce Code Analyzer: Managing Custom Rules](./en/v3.x/custom-rules/manage/)
- [Salesforce Code Analyzer Command Reference](./en/v3.x/scanner-commands/run/#options)
- [Scan Your Solution with Salesforce Code Analyzer](https://developer.salesforce.com/docs/atlas.en-us.packagingGuide.meta/packagingGuide/security_review_code_analyzer_scan.htm)

---
title: 'PMD'
lang: en
---
## What is PMD?
PMD is a source code analyzer that allows for static analysis of code written in a number of supported languages, including Java, Apex, and Visualforce. Its built-in rules detect common flaws in code, such as empty catch blocks or unused variables.

## How does Salesforce Code Analyzer (code Analyzer) use PMD?
By default, the Code Analyzer ```scanner:run``` command executes PMD’s default Apex and Visualforce rules against compatible files. 

You can change which rules are executed by using the flags described in the Code Analyzer Command Reference.

Refer to our [FAQ](https://forcedotcom.github.io/sfdx-scanner/en/architecture/pmd-engine/) for info on how to enable PMD’s built-in rules for other languages.

## See Also

- [PMD](https://pmd.github.io/#home)
- [Salesforce Code Analyzer: Authoring Custom Rules](https://forcedotcom.github.io/sfdx-scanner/en/custom-rules/author/)
- [Salesforce Code Analyzer: Managing Custom Rules](https://forcedotcom.github.io/sfdx-scanner/en/custom-rules/manage/)
- [Salesforce Code Analyzer Command Reference](https://forcedotcom.github.io/sfdx-scanner/en/scanner-commands/run/#options)

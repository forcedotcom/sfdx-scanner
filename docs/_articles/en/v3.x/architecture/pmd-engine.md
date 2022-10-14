---
title: 'PMD'
lang: en
---
## What is PMD?
PMD is a source code analyzer that allows for static analysis of code written in a number of supported languages, including Java, Apex, and Visualforce. Its built-in rules detect common flaws in code, such as empty catch blocks or unused variables.

## How does Salesforce Code Analyzer (code Analyzer) use PMD?
By default, the Code Analyzer ```scanner:run``` command executes PMD’s default Apex and Visualforce rules against compatible files. 

You can change which rules are executed by using the flags described in the Code Analyzer Command Reference.

Refer to our [FAQ](./en/v3.x/faq/#q-how-do-i-enable-engine-xs-default-rules-for-language-y) for info on how to enable PMD’s built-in rules for other languages.

## See Also

- [PMD](https://pmd.github.io/#home)
- [Salesforce Code Analyzer: Authoring Custom Rules](./en/v3.x/custom-rules/author/)
- [Salesforce Code Analyzer: Managing Custom Rules](./en/v3.x/custom-rules/manage/)
- [Salesforce Code Analyzer Command Reference](./en/v3.x/scanner-commands/run/#options)

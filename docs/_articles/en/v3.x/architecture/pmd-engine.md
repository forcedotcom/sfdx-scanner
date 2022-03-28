---
title: 'PMD'
lang: en
---
## What is PMD?
[PMD](https://pmd.github.io/#home) is a source code analyzer that allows for static analysis of code written in a number
of [supported languages](./en/troubleshooting/#supported-languages-for-pmd), including Java, Apex, and Visualforce. It's built-in rules detect common flaws in code, such as empty catch
blocks or unused variables.

## How does Salesforce Code Analyzer use PMD?
By default, Salesforce Code Analyzer's `scanner:run` command will execute PMD's default Apex and Visualforce rules against
compatible files. It is possible to change which rules are executed by using the flags described in the
[command's documentation](./en/scanner-commands/run/#options).

[These steps](./en/faq/#questions-about-adding-and-removing-rules) outline how to enable PMD's built-in rules for other
languages.

It is also possible to [write](./en/custom-rules/author) and [manage](./en/custom-rules/manage) custom PMD rules for any
supported language.

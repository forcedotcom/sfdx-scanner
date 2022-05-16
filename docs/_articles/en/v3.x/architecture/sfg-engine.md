---
title: 'Salesforce Graph Engine'
lang: en
---
## What is the Salesforce Graph Engine?
[Salesforce Graph Engine (SFGE)](./en/v3.x/salesforce-graph-engine/introduction/) is an open-source Salesforce tool that can perform complex analysis on Apex language to identify security vulnerabilities and code issues.

The engine is currently under active development. It is available as an open pilot through the Salesforce Code Analyzer (v3.x). Please [try it out](./en/v3.x/salesforce-graph-engine/try-it-yourself/) and send us your feedback.

## How does Salesforce Code Analyzer use SFGE?
A new command, `scanner:run:dfa`, has been added to invoke the [data-flow-based](./en/v3.x/salesforce-graph-engine/working-with-sfge/) rules in SFGE. You can learn more about it in the [command reference](./en/v3.x/scanner-commands/dfa/).


---
title: 'Salesforce Graph Engine'
lang: en
---
## What is the Salesforce Graph Engine?
[Salesforce Graph Engine (SFGE)](./en/v3.x/salesforce-graph-engine/introduction/) is an open-source Salesforce project that can perform complex analysis on Apex language to identify security vulnerabilities and code issues.

The engine is currently under active development. It is available as an open pilot only through the Salesforce Code Analyzer plugin. Please be aware of its [limitations](./en/v3.x/salesforce-graph-engine/features/#limitations-of-salesforce-graph-engine), but do try it out and send us your feedback.

## How does Salesforce Code Analyzer use SFGE?
[`scanner:run:dfa`](./../scanner-commands/dfa.md) command invokes the data-flow-analysis-based rules in SFGE. Please note that these rules require longer time to finish execution. Also, the analysis time depends on the complexity of conditionals and method invocations of the target code.

### Environment-variable-based Controls

#### *SFGE-RULE-THREAD-COUNT*
Default value is 4. Modify this variable to adjust the number of threads that will each execute DFA-based rules. Equivalent flag on `scanner:run:dfa` command is `--rule-thread-count`.

#### *SFGE-RULE-THREAD-TIMEOUT*
Default value is 900,000ms (15 minutes). Modify this variable to adjust how long DFA-based rules can execute before timing out. You can use this to allow SFGE to run for longer to analyze more complex code. Equivalent flag on `scanner:run:dfa` command is `--rule-thread-timeout`.

#### *SFGE-IGNORE-PARSE-ERRORS*
By default, this value is true. Set this variable to false to force SFGE to ignore parse errors. This is not recommended since the analysis results will be incorrect. Equivalent flag on `scanner:run:dfa` command is `--ignore-parse-errors`.
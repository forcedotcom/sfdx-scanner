---
title: 'Introduction to SFGE'
lang: en
---
## Introduction to SFGE
Salesforce Graph Engine (SFGE) is an open-source Salesforce project used to detect security and quality issues on Salesforce languages. It has the capability to perform more complex checks than an average static analysis tool since it executes [Data Flow Analysis](https://en.wikipedia.org/wiki/Data-flow_analysis).

Salesforce Graph Engine identifies the entry point methods in your specified target, and builds code flow paths across your source code. As SFGE walks the paths, it builds context on how data is transformed at each node of the path.

Rules express interest at specific nodes and are invoked when these nodes are visited while walking the path. Rules execute their checks and use the data context to identify issues. 

Once the engine completes walking all the paths, it returns the issues collected as rule violations.

SFGE uses Apex Jorje Compiler jar to compile Apex code. It uses [Apache TinkerPop Graph Database](https://tinkerpop.apache.org/) to store vertices and [Gremlin Query Language](https://tinkerpop.apache.org/gremlin.html) to help with creating paths.

-------

![Steps run by SFGE](./assets/images/SFGE_flow_overview.png)

-------
As a starting point, SFGE supports one language and one rule: Apex and the [ApexFlsViolationRule](./en/v3.x/salesforce-graph-engine/rules/#dfa-rules) rule.
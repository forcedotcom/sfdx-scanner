---
title: 'Introduction to Salesforce Graph Engine'
lang: en
---
## Introduction to Salesforce Graph Engine
Salesforce Graph Engine is an open-source Salesforce tool that developers use to detect security and quality issues in their code. Graph Engine also performs more complex checks than an average static analysis tool because Graph Engine uses [data flow analysis](https://en.wikipedia.org/wiki/Data-flow_analysis).

During this pilot, Graph Engine supports one Salesforce language and one rule: Apex and the `ApexFlsViolationRule` rule. `ApexFlsViolationRule` helps detect [Create, Read, Update, and Delete and Field-Level Security (CRUD/FLS)](https://www.youtube.com/watch?v=1ZYjpjPTIn8) violations.

## How does Salesforce Graph Engine’s Data Flow Analysis work?

Data flow analysis is a multi-step process.

-------

![Steps run by SFGE](./assets/images/SFGE_flow_overview.png)

-------

* The Apex Jorje compiler analyzes your code and returns a parse tree. 
* Graph Engine translates the parse tree into vertices and adds them to Apache TinkerPop graph database.
* Graph Engine builds code paths starting from each identified entry point.
* Graph Engine walks each code path and applies the selected rules at every vertex along with contextual data. The rule evaluates this information and, if applicable, creates violations.

After Graph Engine completes walking the paths, it returns all issues collected as rule violations.

## How are rules invoked?
Rules register interest in specific types of vertices. For example, a CRUD/FLS rule expresses interest in all vertices that perform Data Manipulation Language (DML) operations.

## What languages and rules does Graph Engine support?
Currently, Graph Engine supports one language and one rule: Apex and the `ApexFlsViolationRule` rule. We’re working on expanding the Graph Engine ruleset.

## See Also:
* [Apex Developer Guide](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_dev_guide.htm)
* [Apache TinkerPop Graph Database](https://tinkerpop.apache.org/)
* [Gremlin Query Language](https://tinkerpop.apache.org/gremlin.html)


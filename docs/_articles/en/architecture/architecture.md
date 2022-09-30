---
title: Architecture
lang: en
---

## Salesforce Code Analyzer Architecture

Salesforce Code Analyzer (Code Analyzer) is an open-source [Salesforce CLI plug-in](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm). Most of the code is written in Typescript in the Node.js framework. Some of the code is written in Java.

## Salesforce Code Analyzer Internals

Code Analyzer is powered by multiple static analyzers, also known as rule engines, which provide data flow analysis. These rule engines specialize in different aspects of static analysis and support multiple languages. Each rule engine has its own unique set of rules, input parameters, and formats for reporting the results. Code Analyzer unifies these rules engines as a single static analyzer and provides a common user experience to benefit.

Version {{ site.data.versions.scanner }} of Code Analyzer uses PMD v{{ site.data.versions.pmd }}, ESLint v{{ site.data.versions.eslint }}, and RetireJS v{{ site.data.versions.retirejs }}.

-------

![Plugin Design](./assets/images/ScannerPlugin.jpeg)

-------

## Rule Engine Unification

To provide a uniform experience while using multiple rule engines, Code Analyzer has two bridging blocks: a rule catalog and a rule engine bridge. 

### Rule Catalog

Each rule engine has a different set of rules and different formats for representing them. Code Analyzer communicates with each rule engine separately to pull the default rules that they offer and to populate them together into a rule catalog. This catalog contains the name of a rule, a short description, its classification category, and the code source language that the rule can analyze.

Run the ```scanner:rule:list``` command to view the rule catalog. The command’s output includes the rule engine name that a particular rule belongs to.

Example:

|NAME								|LANGUAGES		|CATEGORIES				|RULESETS [DEP]								|ENGINE	  		|
| ------- 							| ---------     | ---------				| ---------									| ---------		|
|MyRule1							|visualforce	|Security				|Basic VF									|pmd			|
|MyRule2							|apex			|design					|Complexity,Default ruleset...,quickstart 	|pmd			|
|FormalParameterNamingConventions	|apex			|Code Style				|quickstart									|pmd			|
|constructor-super					|javascript		|problem				|problem									|eslint			|
|ApexFlsViolationRule				|apex			|Security				|											|sfge			|

### Rule Engine Bridge

By unifying the representation of the rules into a rule catalog, Code Analyzer accepts a uniform set of input parameters. Here’s how it works.

1. Using rules that you select, the bridge detects the related engine and determines the target files based on the file types defined in ```~/.sfdx-scanner/Config.json```. 
2. Next, the bridge tailors the input for each relevant rule engine and hands the input to the rule engine for the actual scan.
3. After the scan completes and the rule engine provides the results, the bridge surfaces the rule violation results in a normalized output format.

## Source Code and Contribution

The code for Code Analyzer is in [this GitHub repo](https://github.com/forcedotcom/sfdx-scanner). Salesforce is working on expanding and improving the tool.

## See Also
[Salesforce CLI Plug-In Developer Guide: Salesforce CLI Architecture](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm)

---
title: Design
lang: en
---

## sfdx-scanner Architecture

sfdx-scanner is built as an open-source Sfdx Plugin in the [Salesforce CLI framework](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm). Most of the code is written in Typescript in NodeJS framework and holds a small portion of Java code. 

## sfdx-scanner Internals

sfdx-scanner is powered by multiple static analyzers that we refer to as Rule Engines. These Rule Engines specialize in different aspects of static analysis as well as support different languages. Also, each Rule Engine has its own unique set of rules, input parameters, and different formats of reporting the results. sfdx-scanner unifies these as a single static analyzer and provides a common user experience to benefit from the different specialties.

For now, sfdx-scanner version 2.0 has incorporated PMD v6.23.0 and Eslint v6.8.0.

-------

![Plugin Design](./assets/images/ScannerPlugin.jpeg) 

-------

## Rule Engine Unification

In order to provide a uniform experience while using multiple Rule Engines, sfdx-scanner has two bridging blocks.

### Rule Catalog

Since each Rule Engine has a different set of rules and different formats for representing them, we talk to each engine separately to pull the default rules they offer and populate them together in the Rule Catalog. This contains information on the name of a rule, a short description, the category they can be classified under, and the language of the code source the rule can analyze. You can view this catalog as the output of ```scanner:rule:list``` command. Notice that this list includes the name of the Rule Engine a particular rule belongs to.

### Rule Engine Bridge

By unifying the representation of the rules into a Rule Catalog, sfdx-scanner has the capability to take a uniform set of input parameters. The bridge then detects the selected engines through the rules selected for input. It also figures out the target files based on the file types defined in ```~/.sfdx-scanner/Config.json```. From the selected rules and target files, it tailors the input for each relevant Rule Engine and hands off the input to the Rule Engine for the actual scan.

Once the scan completes and the Rule Engine provides the results, the bridge surfaces the results in a normalized format.

## Source Code and Contribution

sfdx-scanner code resides [here](https://github.com/forcedotcom/sfdx-scanner). At the moment, a Salesforce team is actively working on expanding and improving the tool. The project will soon be open for external contributions.




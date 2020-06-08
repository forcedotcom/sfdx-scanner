---
title: SFDX Scanner Command Reference
lang: en
---

## sfdx scanner:rule:describe
Provides detailed information about a rule. Use this command to better understand a particular rule.

For each rule, you can find information about the language it works on, the violation it detects as well as an example code of how the violation looks. The description also includes the categories and rulesets that the rule belongs to.

## Usage

```bash
$ sfdx scanner:rule:describe -n <string> [--verbose] [--json]
```
  
## Options

```bash
  -n, --rulename=rulename	(required) The name of a rule.
  --json			format output as json
  --verbose 			emit additional command output to stdout

```
  
## Example

```bash
  $ sfdx scanner:rule:describe --rulename ExampleRule
     name:        ExampleRule
     categories:  ExampleCategory
     rulesets:    Ruleset1
                  Ruleset2
                  Ruleset3
     languages:   apex
     description: Short description of rule
     message:     ExampleRule Violated.
```  

## Demo
![Describe Example](./assets/images/describe.gif) 

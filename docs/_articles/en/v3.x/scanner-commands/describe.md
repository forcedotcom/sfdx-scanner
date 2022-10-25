---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/describe
---

## sfdx scanner:rule:describe
Provides detailed information about a rule. Information includes its description, language (such as Apex or Java) and the error message that user can expect if this rule violation occurs. The command output also includes the rule's categories and rulesets.

## Usage

```bash
$ sfdx scanner:rule:describe -n <string> [--verbose] [--json]
```
  
## Options

```bash
  -n, --rulename=rulename	(required) The name of the rule.
  --json			Formats output as json.
  --verbose 			Emits additional command output to stdout.

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

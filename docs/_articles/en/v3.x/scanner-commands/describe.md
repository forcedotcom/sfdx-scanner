---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/describe
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/describe.html
---

## sf scanner rule describe
Provides detailed information about a rule. Information includes the rule’s language (such as Apex or Java), the violation it detects, and example code of the violation. The command output also includes the rule's categories and rulesets.

## Usage

```bash
$ sf scanner rule describe -n <string> [--verbose] [--json]
```
  
## Options

```bash
  -n, --rulename=rulename	(required) The name of the rule.
  --json			Formats output as JSON.
  --verbose 			Emits additional command output to stdout.

```
  
## Example

```bash
  $ sf scanner rule describe --rulename ExampleRule
     name:        ExampleRule
     categories:  ExampleCategory
     rulesets:    Ruleset1
                  Ruleset2
                  Ruleset3
     languages:   apex
     description: Short description of rule
     message:     ExampleRule Violated.
```

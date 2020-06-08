---
title: SFDX Scanner Command Reference
lang: en
---

## sfdx scanner:rule:list
Lists all the rules available in the catalog. To look at a smaller set of rules, use the filter options available. To get more information about a specific rule, you can use the scanner:rule:describe command.

## Usage

```bash
$ sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--verbose] [--json] 
```
  
## Options

```bash
  -c, --category=category 	Select rules by category. Enter multiple values as a comma-separated list.
  -l, --language=language 	Select rules by language. Enter multiple values as a comma-separated list.
  -r, --ruleset=ruleset 	Select rules by ruleset. Enter multiple values as a comma-separated list.
  --json 			format output as json
  --verbose 			emit additional command output to stdout

```
  
## Example
Invoking with no filter criteria returns all rules.
```bash
$ sfdx scanner:rule:list
```
The values supplied to a single filter are handled with a logical OR.
```bash
$ sfdx scanner:rule:list --language apex,javascript
```

Different filters are combined with a logical AND.
Returns all rules that target Apex OR Javascript, AND are members of the Braces OR Security rulesets.

```bash
$ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
```

## Demo
![Describe Example](./assets/images/list.gif) 

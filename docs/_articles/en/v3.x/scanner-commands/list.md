---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/list
---

## sfdx scanner:rule:list
Lists all the rules available in the catalog. You can filter the output to view a smaller set of rules. To get more information about a specific rule, use the ```scanner:rule:describe``` command.

## Usage

```bash
  $ sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [-e <array>] 
  [--verbose] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]
```
  
## Options

```bash
OPTIONS
  -c, --category=category
      Selects rules by category. Enter multiple values as a comma-separated list.

  -e, --engine=engine
      Selects rules by engine. Enter multiple values as a comma-separated list.

  -l, --language=language
      Selects rules by language. Enter multiple values as a comma-separated list.

  -r, --ruleset=ruleset
      [deprecated] Selects rules by ruleset. Enter multiple values as a comma-separated list.

  --json
      Formats output as JSON.

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATA
  L)
      [default: warn] Logging level for this command invocation.

  --verbose
      Emits additional command output to stdout.
```
  
## Additional Notes

--ruleset The `ruleset` command parameter is deprecated. Use `category` instead.


## Example
Invoking without filter criteria returns all rules.
This example returns a table containing all rules.
```bash
$ sfdx scanner:rule:list
```

This example returns all rules for Apex OR Javascript. The values supplied to a single filter are handled with a logical OR.
```bash
$ sfdx scanner:rule:list --language apex,javascript
```

This example returns all rules that target Apex or Javascript, and are members of the Braces or Security rulesets. Different filters are combined with a logical AND.
```bash
$ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
```

This example returns all rules except those in the Design or Best Practices categories. Exclude categories by specifying the negation operator and enclose the values in single quotes.
```bash
$ sfdx scanner:rule:list --category '!Design,!Best Practices'
```

## Demo
![List Example](./assets/images/list.gif) 

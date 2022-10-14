---
title: Salesforce Code Analyzer Command Reference
lang: en
---

## sfdx scanner:rule:list
Lists all the rules available in the catalog. Filter the output to view a smaller set of rules. To get more information about a specific rule, use the ```scanner:rule:describe``` command.

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
      categories to filter list by

  -e, --engine=engine
      engine(s) to filter list by

  -l, --language=language
      language(s) to filter list by

  -r, --ruleset=ruleset
      [deprecated] ruleset(s) to filter list by

  --json
      format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATA
  L)
      [default: warn] logging level for this command invocation

  --verbose
      emit additional command output to stdout
```
  
## Additional Notes

--ruleset option is deprecated and will be removed soon. Please use --category instead.


## Example
To see all rules, run the command without any filters. 
```bash
$ sfdx scanner:rule:list
```

When you specify multiple values for a single filter, the values are combined with a logical OR. 
```bash
$ sfdx scanner:rule:list --language apex,javascript
```

When you specify multiple filters, they are combined with a logical AND. This example returns all rules that target Apex OR Javascript AND are members of the Braces OR Security rulesets.

```bash
$ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
```

When you negate a category, the category is excluded. This example returns all rules except those in the Design or Best Practices categories. The values must be enclosed in single quotes.
```bash
$ sfdx scanner:rule:list --category '!Design,!Best Practices'
```

## Demo
![List Example](./assets/images/list.gif) 

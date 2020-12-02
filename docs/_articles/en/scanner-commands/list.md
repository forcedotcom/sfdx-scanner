---
title: Salesforce CLI Scanner Plug-In Command Reference
lang: en
---

## sfdx scanner:rule:list
Lists all the rules available in the catalog. You can filter the output to view a smaller set of rules. To get more information about a specific rule, use the ```scanner:rule:describe``` command.

## Usage

```bash
$ sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--verbose] [--json] 
```
  
## Options

```bash
  -c, --category=category 	Select rules by category. Enter multiple values as a comma-separated list.
  -l, --language=language 	Select rules by language. Enter multiple values as a comma-separated list.
  -r, --ruleset=ruleset 	[Deprecated] Select rules by ruleset. Enter multiple values as a comma-separated list.
  --json 			Format output as json
  --verbose 			Emit additional command output to stdout

```
  
## Additional Notes

--ruleset option is deprecated and will be removed soon. Please use --category instead.


## Example
To see all rules, run the command without any filters. 
```bash
$ sfdx scanner:rule:list
```
If you specify multiple values for a single filter, the values are combined with a logical OR. 
```bash
$ sfdx scanner:rule:list --language apex,javascript
```

If you specify multiple filters, they are combined with a logical AND. This example returns all rules that target Apex OR Javascript AND are members of the Braces OR Security rulesets.

```bash
$ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
```

When you negate a category, the category is excluded. This example returns all rules except those in the Design or Best Practices categories. The values must be enclosed in single quotes.
```bash
$ sfdx scanner:rule:list --category '!Design,!Best Practices'
```

## Demo
![List Example](./assets/images/list.gif) 

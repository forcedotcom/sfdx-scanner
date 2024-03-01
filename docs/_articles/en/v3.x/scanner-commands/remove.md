---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/remove
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/remove.html
---

## sf scanner rule remove
Removes custom rules from the registry of available rules. Use the ```-p|--path``` parameter to specify one or more paths to remove. If you don't specify any parameters, the command lists all valid custom paths but doesn't remove any.

## Usage

```bash
$ sf scanner rule remove [-f] [-p <array>] [--verbose] [--json]
```
  
## Options

```bash
  -f, --force		Bypasses the confirmation prompt and immediately removes the rules.
  -p, --path=path	One or more paths to remove. Specify multiple values with a comma-separated list.
  --json      		Formats output as JSON.
  --verbose      	Emits additional command output to stdout.
```
  
## Example

This example runs the command without arguments to see a list of registered custom paths.
```bash
$ sf scanner rule remove
```

This example uses the `--path` parameter to deregister the rules defined in `somerules.jar` and any JARs/XMLs contained in the rules folder.
```somerules.jar``` and ```myrules.xml```, and all JARs/XMLs contained in the ```rules``` folder.
  
```bash
$ sf scanner rule remove --path "~/path/to/somerules.jar,~/path/to/category/apex/myrules.xml,~/path/to/folder/containing/rules"
```  
  		
This example uses the `--force` flag to bypass the confirmation prompt, removing all rules defined in `somerules.jar`. By default, a list of all the rules that will be deregistered is displayed, and the action must be confirmed. To bypass that confirmation, use the `--force` flag.
```bash
$ sf scanner rule remove --force --path "~/path/to/somerules.jar"
```

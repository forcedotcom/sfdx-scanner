---
title: Salesforce CLI Scanner Plug-In Command Reference
lang: en
---

## sfdx scanner:run
Scan a codebase with a selection of rules. You can scan the codebase with all the rules in the registry, or use parameters to filter the rules based on rulename, category, or ruleset. 

You can specify the format of the output as XML, Junit, CSV or table. You can print the output to the console (default) or to a file using the ```--outfile``` parameter. 

## Usage

```bash
$ sfdx scanner:run [-c <array>] [-r <array>] [-t <array> | undefined] [-f xml|junit|csv|table] [-o <string>] [--verbose] [--json]
```
  
## Options

```bash
  -c, --category=category		One or more categories of rules to run. Specify multiple values as a comma-separated list.
  -f, --format=(xml|junit|csv|table) 	Specifies output format with results written directly to the console.
  -o, --outfile=outfile			Write output to a file
  -r, --ruleset=ruleset			One or more rulesets to run. Specify multiple values as a comma-separated list.
  -t, --target=target			Source code location. May use glob patterns. Specify multiple values as a comma-separated list
  --json				Format output as json
  --verbose				Emit additional command output to stdout
```
  
## Example

This example evaluates all rules against ```somefile.js```.

```bash
$ sfdx scanner:run --format xml --target "somefile.js"
```

When you specify multiple categories or rulesets, the results are combined with a logical OR. This example evaluates all rules in the Design and Best Practices categories and all rules in the Braces ruleset.
```bash
$ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices" --ruleset "Braces"
```       

Wrap globs in quotes.  This example evaluates rules against all ```*.js``` files in the current directory, except for ```IgnoreMe.js```.

Unix example:
```bash    
$ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
````
Windows example: 
```DOS
> sfdx scanner:run --target ".\**\*.js,!.\**\IgnoreMe.js" ...
```

## Demo
![Run Example](./assets/images/run.gif) 



---
title: SFDX Scanner Command Reference
lang: en
---

## sfdx scanner:run
Evaluate a selection of rules against a codebase. Scan codebase with all rules by default or with a chosen set of rules if using rulename/category/ruleset filters.

You can choose the format of output and decide between printing the results directly or as contents of a file that you provide with --outfile flag.

## Usage

```bash
$ sfdx scanner:run [-c <array>] [-r <array>] [-t <array> | undefined] [-f xml|junit|csv|table] [-o <string>] [--verbose] [--json]
```
  
## Options

```bash
  -c, --category=category		One or more categories of rules to run. Multiple values can be specified as a comma-separated list.
  -f, --format=(xml|junit|csv|table) 	Specifies output format with results written directly to the console.
  -o, --outfile=outfile			write output to a file
  -r, --ruleset=ruleset			One or more rulesets to run. Multiple values can be specified as a comma-separated list.
  -t, --target=target			Source code location. May use glob patterns. Multiple values can be specified as a comma-separated list
  --json				format output as json
  --verbose				emit additional command output to stdout
```
  
## Example

Invoking without specifying any rules causes all rules to be run. Following evaluates all rules against somefile.js.

```bash
$ sfdx scanner:run --format xml --target "somefile.js"
```

Specifying multiple categories or rulesets is treated as a logical OR. Following example evaluates all rules in the Design and Best Practices categories, and all rules in the Braces ruleset.
```bash
$ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices" --ruleset "Braces"
```       

Wrap globs in quotes.  Following example evaluates rules against all .js files below the current directory, except for IgnoreMe.js.

Unix example:
```bash    
$ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
````
Windows example: 
```DOS
> sfdx scanner:run --target ".\**\*.js,!.\**\IgnoreMe.js" ...
```

## Demo

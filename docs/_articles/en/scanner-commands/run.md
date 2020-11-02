---
title: Salesforce CLI Scanner Plug-In Command Reference
lang: en
---

## sfdx scanner:run
Scan a codebase with a selection of rules. You can scan the codebase with all the rules in the registry, or use parameters to filter the rules based on rulename, category, or ruleset. 

You can specify the format of the output as XML, Junit, CSV or table. You can print the output to the console (default) or to a file using the ```--outfile``` parameter. 

## Usage

```bash
$ sfdx scanner:run [-c <array>] [-r <array>] [-e <array>] [-t <array> | undefined] [-f xml|junit|csv|table] [-o <string>] [--verbose] [--json]
```
  
## Options

```bash
  -c, --category=category		One or more categories of rules to run. Specify multiple values as a comma-separated list.
  -e, --engine=engine 			One or more engines to run. Multiple values can be specified as a comma-separated list.
  -f, --format=(xml|junit|csv|table) 	Specifies output format with results written directly to the console.
  -o, --outfile=outfile			Write output to a file
  -r, --ruleset=ruleset			[Deprecated] One or more rulesets to run. Specify multiple values as a comma-separated list.
  -t, --target=target			Source code location. May use glob patterns. Specify multiple values as a comma-separated list
  -v, --violations-cause-error				When violations are detected, exit with code equal to the severity of the most severe violation.
  --tsconfig				tsconfig.json location. Required if the current working directory does not contain the tsconfig.json that corresponds to the TypeScript files being scanned.
  --env 				JSON-formatted string that overrides ESLint's default environmental variables.
  --json				Format output as json
  --verbose				Emit additional command output to stdout
```

## Additional Notes

--ruleset option is deprecated and will be removed soon. Please use --category instead.
  
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

Specify tsconfig.json if the current working directory does not contain the ```tsconfig.json``` that corresponds to the TypeScript files being scanned. This example demonstrates scanning the project contained in ```/my-project``` if the current working directory is ```/my-home-directory```.
```bash
$ cd /my-home-directory
$ sfdx scanner:run --target "/my-project/**/*.ts" --tsconfig "/my-project/tsconfig.json"
```
Use --env to override the default ESLint environment variables to add frameworks.
```bash
$ sfdx scanner:run --target "somefile.js" --env '{"jasmine": true}'
```

Use --violations-cause-error to throw a non-zero exit code when rule violations are found.
In this example, if any rules are violated, the exit code will be equal to the severity of the most severe violation.
```bash
$ sfdx scanner:run --target "somefile.js" --violations-cause-error
```

Use --engine to include or exclude engines. Regardless of their current 'disabled' attribute, any specified engine will run, and all others will not.
In this example, ESLint and RetireJS will run even if they're disabled, and no other engines will run.
```bash
$ sfdx scanner:run --target "somedirectory" --engine "eslint,retire-js"
```

## Demo
![Run Example](./assets/images/run.gif) 



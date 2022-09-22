---
title: Salesforce Code Analyzer Plug-In Command Reference
lang: en
---

## sfdx scanner:run
Scan a codebase with a selection of rules. You can scan the codebase with all the rules in the registry, or use parameters to filter the rules based on rulename, category, or ruleset.

**Note**: To run Salesforce Graph Engine on Code Analyzer version 3.x, you must run a separate command: ​​scanner:run:dfa. Learn more in [Introduction to Salesforce Graph Engine](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/introduction/).

You can specify the format of the output as XML, JUnit, CSV, or table. You can print the output to the console (default) or to a file using the `--outfile` parameter.

## Usage

```bash
sfdx scanner:run -t <array> [-c <array>] [-r <array>] [-e <array>] [-f 
 csv|html|json|junit|sarif|table|xml] [-o <string>] [--tsconfig <string>] [--eslintconfig <string>] [--pmdconfig <string>] [--env <string>] [-s <integer> | undefined | [-v | --json]] [--normalize-severity] [--verbose] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]
```
  
## Options

```bash
-c, --category=_category_
 categor(ies) of rules to run

 -e, --engine=_engine_
 engine(s) to run

 -f, --format=(csv|html|json|junit|sarif|table|xml)
 format of results

 -o, --outfile=_outfile_
 location of output file

 -r, --ruleset=_ruleset_
 [deprecated] ruleset(s) of rules to run

 -s, --severity-threshold=_severity-threshold_
 throws an error when violations of specific severity (or more severe) are detected, invokes 

 --normalize-severity

 -t, --target=_target_
 (required) location of source code

 --env=_env_
 JSON-formatted string, overrides ESLint\'s default environment variables

 --eslintconfig=_eslintconfig_
 location of eslintrc config to customize eslint engine

 --json
 format output as json

 --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)
 [default: warn] logging level for this command invocation

 --normalize-severity
 A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine 
 specific severity

 --pmdconfig=_pmdconfig_
 location of PMD rule reference XML file to customize rule selection

 --tsconfig=_tsconfig_
 location of tsconfig.json file

 --verbose
 emit additional command output to stdout

 --verbose-violations
retire-js violation messages include more details

```

## Additional Notes

- `--ruleset` option is deprecated and will be removed soon. Please use --category instead.
  
## Example

This example evaluates all rules against ```somefile.js```.

```bash
$ sfdx scanner:run --format xml --target "somefile.js"
```

When you specify multiple categories, the categories are combined with a logical OR. This example evaluates all rules in the Design or Best Practices categories.
```bash
$ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices"
```

When you negate a category, the category is excluded. This example evaluates all rules except those in the Design or Best Practices categories. The values must be enclosed in single quotes.
```bash
$ sfdx scanner:run --format xml --target "somefile.js" --category '!Design,!Best Practices'
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

Use --engine to include or exclude engines. Regardless of their current 'disabled' attribute, any specified engine will run, and all others will not.

In this example, ESLint and RetireJS will run even if they're disabled, and no other engines will run.
```bash
$ sfdx scanner:run --target "somedirectory" --engine "eslint,retire-js"
```

In another example, Analyzer executes CPD engine against known file extensions in "/some/dir". CPD helps detect blocks of code duplication in selected languages. 

```bash
$ sfdx scanner:run --target "/some/dir" --engine cpd
 ```
 
To use PMD with your own rule reference file, use --pmdconfig. Note that rule filters are not applied.
```bash
$ sfdx scanner:run --target "src" --pmdconfig "pmd_rule_ref.xml"
```

To use Eslint with your own .eslintrc.json file, use --eslintconfig. Make sure that the directory you run the command from has all the NPM dependencies installed.
```bash
$ sfdx scanner:run --target "src" --eslintconfig "/home/my/setup/.eslintrc.json"
```

Use `--normalize-severity` to output a normalized (across all engines) severity (1 [high], 2 [moderate], and 3 [low]) in addition to the engine specific severity (when shown). 
```bash
$ sfdx scanner:run --target "/some-project/" --format csv --normalize-severity
```

Use `--severity-threshold` to throw a non-zero exit code when rule violations of a specific severity (or greater) are found. For this example, if there are any rule violations with a severity of 2 or more (which includes 1-high and 2-moderate), the exit code will be equal to the severity of the most severe violation
```bash
$ sfdx scanner:run --target "/some-project/" --severity-threshold 2
```

## Demo
![Run Example](./assets/images/run.gif)

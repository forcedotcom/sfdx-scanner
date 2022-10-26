---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/run
---

## sfdx scanner:run

Scans a codebase with a selection of rules. Scan the codebase with all the rules in the registry, or use parameters to filter the rules based on rulename, category, or ruleset. Specify the format of the output, such as XML or JUnit. Print the output to the console (default) or to a file using the ```--outfile``` parameter. 

**Note**: To run Salesforce Graph Engine, you must run a separate command: `scanner:run:dfa`. Learn more in [Introduction to Salesforce Graph Engine](./en/v3.x/salesforce-graph-engine/introduction/).


## Usage

```bash
sfdx scanner:run -t <array> [-c <array>] [-r <array>] [-e <array>] [-f 
 csv|html|json|junit|sarif|table|xml] [-o <string>] [--tsconfig <string>] [--eslintconfig <string>] [--pmdconfig <string>] [--env <string>] [-s <integer> | undefined | [-v | --json]] [--normalize-severity] [--verbose] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]
```
  
## Options

```bash
-c, --category=_category_
One or more categories of rules to run. Specify multiple values as a comma-separated list.

 -e, --engine=_engine_
Specifies one or more engines to run. Submit multiple values as a comma-separated list.
specify the location of eslintrc config to customize eslint engine

 -f, --format=(csv|html|json|junit|sarif|table|xml)
 Specifies output format with results written directly to the console.

 -o, --outfile=_outfile_
 Writes output to a file.

 -r, --ruleset=_ruleset_
 [deprecated] One or more rulesets to run. Specify multiple values as a comma-separated list.

 -s, --severity-threshold=_severity-threshold_
 Throws an error when violations are found with equal or greater severity than the provided value. –normalize-severity is invoked and severity levels are reset to the baseline. Normalized severity values are: 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation.

 --normalize-severity
 Returns normalized severity 1 (high), 2 (moderate), and 3 (low) and the engine-specific severity. For the html option, the normalized severity is displayed instead of the engine severity.

 -t, --target=_target_
 (required) Source code location. May use glob patterns. Specify multiple values as a comma-separated list.

 --env=_env_
 Overrides ESLint’s default environment variables, in JSON-formatted string.

 --eslintconfig=_eslintconfig_
 Specifies the location of eslintrc config to customize eslint engine.

 --json
 Formats output as JSON.

 --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)
 [default: warn] Logging level for this command invocation.

 --normalize-severity
 Returns normalized severity in addition to the engine specific severity. Normalized severity is: 1 (high), 2 (moderate), and 3 (low).

 --pmdconfig=_pmdconfig_
 Specifies the location of PMD rule reference XML file to customize rule selection.

 --tsconfig=_tsconfig_
 Location of tsconfig.json file used by eslint-typescript engine.

 --verbose
 Emits additional command output to stdout.

 --verbose-violations
Returns retire-js violation messages details about each vulnerability, including summary, Common Vulnerabilities and Exposures (CVE), and URLs.

```

## Additional Notes

- The ```--ruleset``` command parameter is deprecated. Use ```category``` instead.
  
## Example

This example evaluates all rules against ```somefile.js```. Invoking code analyzer without specifying any rules causes all rules to be run.

```bash
$ sfdx scanner:run --format xml --target "somefile.js"
```

This example evaluates all rules in the Design and Best Practices categories. When you specify multiple categories or rulesets, the results are combined with a logical OR.
```bash
$ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices"
```

This example evaluates all rules except those in the Design or Best Practices categories. Exclude categories by specifying the negation operator and enclosing the values in single quotes.
```bash
$ sfdx scanner:run --format xml --target "somefile.js" --category '!Design,!Best Practices'
```

These examples evaluate rules against all .js files in the current directory, except for IgnoreMe.js. Wrap globs in quotes. 

Unix example:
```bash
$ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
````
Windows example:
```DOS
> sfdx scanner:run --target ".\**\*.js,!.\**\IgnoreMe.js" ...
```

This example scans the project contained in '/my-project' if the current working directory is another directory. Specify tsconfig.json if the current working directory does not contain the tsconfig.json that corresponds to the TypeScript files being scanned.
```bash
$ cd /my-home-directory
$ sfdx scanner:run --target "/my-project/**/*.ts" --tsconfig "/my-project/tsconfig.json"
```

This example evaluates rules against somefile.js, including Jasmine in the environment variables. Uses --env to override the default ESLint environment variables to add frameworks.
```bash
$ sfdx scanner:run --target "somefile.js" --env '{"jasmine": true}'

This example evaluates rules aginst somefile.js using eslint-lwc and pmd engines. Use --engine to include or exclude engines. Any engine listed will be run, regardless of its current 'disabled' attribute.

```bash
$ sfdx scanner:run --target "somefile.js" --engine "eslint-lwc,pmd"
```

In this example, ESLint and RetireJS will run even if they’re disabled, and no other engines will run. Use --engine to include or exclude engines. Regardless of their current ‘disabled’ attribute, any specified engine will run, and all others will not. 
```bash
$ sfdx scanner:run --target "somedirectory" --engine "eslint,retire-js"
```

Use --engine to invoke engines that are not enabled by default.
This example executes CPD engine against known file extensions in "/some/dir". CPD helps detect blocks of code duplication in selected languages.

```bash
$ sfdx scanner:run --target "/some/dir" --engine cpd
 ```
 
This example executes rules defined in pmd_rule_ref.xml against the files in 'src'. To use PMD with your own rule reference file, use --pmdconfig. Note that rule filters are not applied.

```bash
$ sfdx scanner:run --target "src" --pmdconfig "pmd_rule_ref.xml"
```

This example uses a custom config to scan the files in 'src'. To use ESLint with your own .eslintrc.json file, use --eslintconfig. Make sure that the directory you run the command from has all the NPM dependencies installed.

```bash
$ sfdx scanner:run --target "src" --eslintconfig "/home/my/setup/.eslintrc.json"
```

This example uses --normalize-severity to output normalized severity and engine-specific severity across all engines. Normalized severity is: 1 (high), 2 (moderate), and 3 (low). 

```bash
$ sfdx scanner:run --target "/some-project/" --format csv --normalize-severity
```

This example uses --severity-threshold to throw a non-zero exit code when rule violations of normalized severity 2 or greater are found. If any violations with the specified severity (or greater) are found, the exit code equals the severity of the most severe violation.

```bash
$ sfdx scanner:run --target "/some-project/" --severity-threshold 2
```

## Demo
![Run Example](./assets/images/run.gif)

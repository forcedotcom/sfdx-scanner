[![CircleCI](https://circleci.com/gh/forcedotcom/sfdx-scanner/tree/master.svg?style=shield)](https://circleci.com/gh/forcedotcom/sfdx-scanner/tree/master)
[![Codecov](https://codecov.io/gh/forcedotcom/sfdx-scanner/branch/master/graph/badge.svg)](https://codecov.io/gh/forcedotcom/sfdx-scanner)
[![License](https://img.shields.io/npm/l/scanner.svg)](https://github.com/forcedotcom/sfdx-scanner/blob/master/package.json)

# Salesforce CLI Scanner Plug-in
The Salesforce CLI Scanner plug-in is a unified tool for static analysis of source code, in multiple languages (including Apex), with a consistent command-line interface and report output. We currently support the PMD rule engine and ESLint. We may add support for more rule engines in the future.

The Salesforce CLI Scanner Plug-in creates "Rule Violations" when the scanner identifies issues. Developers use this information as feedback to fix their code.

You can integrate this plug-in into your CI/CD solution to enforce the rules and expect high-quality code.

# Official Documentation
All the official documentation on the Salesforce CLI Scanner plug-in is hosted on [GitHub Pages](https://forcedotcom.github.io/sfdx-scanner/). These documents include instructions on how to install the plug-in, the command reference, writing and managing custom rules and an overview of the architecture of the plug-in.

### Document Link
https://forcedotcom.github.io/sfdx-scanner/

### Contributing
Instructions [here](CONTRIBUTING.MD).

=======

# Usage
<!-- usage -->
```sh-session
$ npm install -g @salesforce/sfdx-scanner
$ sfdx COMMAND
running command...
$ sfdx (-v|--version|version)
@salesforce/sfdx-scanner/1.0.30 darwin-x64 node-v14.1.0
$ sfdx --help [COMMAND]
USAGE
  $ sfdx COMMAND
...
```
<!-- usagestop -->
# Commands
<!-- commands -->
* [`sfdx scanner:rule:add -l <string> -p <array> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerruleadd--l-string--p-array---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:rule:describe -n <string> [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerruledescribe--n-string---verbose---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerrulelist--c-array--r-array--l-array---verbose---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:rule:remove [-f] [-p <array>] [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerruleremove--f--p-array---verbose---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:run [-c <array>] [-r <array>] [-t <array> | undefined] [-f json|xml|junit|csv|table] [-o <string>] [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerrun--c-array--r-array--t-array--undefined--f-jsonxmljunitcsvtable--o-string---verbose---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:scannerCommand [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerscannercommand---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)

## `sfdx scanner:rule:add -l <string> -p <array> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

add custom rules to the scanner's registry

```
USAGE
  $ sfdx scanner:rule:add -l <string> -p <array> [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -l, --language=language                                                           (required) language against which
                                                                                    the custom rules will evaluate

  -p, --path=path                                                                   (required) one or more paths to
                                                                                    custom rule definitions

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

EXAMPLE
  PMD: Custom PMD rules should be in JARs. Adhere to PMD conventions, including defining rules in XMLs under a /category 
  directory.
  Refer to PMD's documentation for information on writing rules: 
  https://pmd.github.io/latest/pmd_userdocs_extending_writing_pmd_rules.html

  	You may specify one or more JARs directly.
  		E.g., $ sfdx scanner:rule:add --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/Jar2.jar"
  			Successfully added rules for apex.
  			2 path(s) added:
  			/Users/me/rules/SomeJar.jar,/Users/me/rules/AnotherJar.jar

  	You may also specify a directory containing one or more JARs, all of which will be added.
  		E.g., $ sfdx scanner:rule:add --language apex --path "/Users/me/rules"
  			Successfully added rules for apex.
  			2 path(s) added:
  			/Users/me/rules/SomeJar.jar,/Users/me/rules/AnotherJar.jar
```
_See code: [lib/commands/scanner/rule/add.js](https://github.com/forcedotcom/sfdx-scanner/blob/v1.0.30/lib/commands/scanner/rule/add.js)_

## `sfdx scanner:rule:describe -n <string> [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

provide detailed information about a rule

```
USAGE
  $ sfdx scanner:rule:describe -n <string> [--verbose] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -n, --rulename=rulename                                                           (required) the name of a rule
  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

  --verbose                                                                         emit additional command output to
                                                                                    stdout

EXAMPLE
  $ sfdx scanner:rule:describe --rulename ExampleRule
  	name:        AvoidWithStatement
  	categories:   Best Practices
  	rulesets:    Controversial Ecmascript
  	languages:   javascript
  	description: Avoid using with - it's bad news
  	message:     Avoid using with - it's bad news
```
_See code: [lib/commands/scanner/rule/describe.js](https://github.com/forcedotcom/sfdx-scanner/blob/v1.0.30/lib/commands/scanner/rule/describe.js)_

## `sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

lists basic information about all rules matching provided criteria

```
USAGE
  $ sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--verbose] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -c, --category=category                                                           categories to filter list by
  -l, --language=language                                                           language(s) to filter list by
  -r, --ruleset=ruleset                                                             ruleset(s) to filter list by
  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

  --verbose                                                                         emit additional command output to
                                                                                    stdout

EXAMPLE
  Invoking with no filter criteria returns all rules.
  	E.g., $ sfdx scanner:rule:list
  		Returns a table containing all rules.

  The values supplied to a single filter are handled with a logical OR.
  	E.g., $ sfdx scanner:rule:list --language apex,javascript
  		Returns all rules for Apex OR Javascript.

  Different filters are combined with a logical AND.
  	E.g., $ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
  		Returns all rules that:
  		1) Target Apex OR Javascript,
  		AND...
  		2) Are members of the Braces OR Security rulesets.
```
_See code: [lib/commands/scanner/rule/list.js](https://github.com/forcedotcom/sfdx-scanner/blob/v1.0.30/lib/commands/scanner/rule/list.js)_

## `sfdx scanner:rule:remove [-f] [-p <array>] [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

removes custom rules from the registry of available rules

```
USAGE
  $ sfdx scanner:rule:remove [-f] [-p <array>] [--verbose] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -f, --force                                                                       bypass the confirmation prompt and
                                                                                    immediately unregister the rules

  -p, --path=path                                                                   one or more paths to deregister

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

  --verbose                                                                         emit additional command output to
                                                                                    stdout

EXAMPLE
  Run the command with no arguments to see a list of all currently registered custom paths.
  	E.g., $ sfdx scanner:rule:remove
  		Returns all registered custom paths.

  You may use the --path parameter to specify one or more paths to remove.
  	E.g., $ sfdx scanner:rule:remove --path "~/path/to/somerules.jar,~/path/to/folder/containing/rules"
  		Deregisters the rules defined in somerules.jar and any JARs contained in the rules folder.

  By default, a list of all the rules that will be deregistered is displayed, and the action must be confirmed.
  The --force flag may be used to bypass that confirmation.
  	E.g., $ sfdx scanner:rule:remove --force --path "~/path/to/somerules.jar"
  		Deregisters somerules.jar without requiring confirmation.
```
_See code: [lib/commands/scanner/rule/remove.js](https://github.com/forcedotcom/sfdx-scanner/blob/v1.0.30/lib/commands/scanner/rule/remove.js)_

## `sfdx scanner:run [-c <array>] [-r <array>] [-t <array> | undefined] [-f json|xml|junit|csv|table] [-o <string>] [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

evaluate a selection of rules against a codebase

```
USAGE
  $ sfdx scanner:run [-c <array>] [-r <array>] [-t <array> | undefined] [-f json|xml|junit|csv|table] [-o <string>] 
  [--verbose] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -c, --category=category                                                           categor(ies) of rules to run
  -f, --format=(json|xml|junit|csv|table)                                           format of results
  -o, --outfile=outfile                                                             location of output file
  -r, --ruleset=ruleset                                                             ruleset(s) of rules to run
  -t, --target=target                                                               location of source code
  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

  --verbose                                                                         emit additional command output to
                                                                                    stdout

EXAMPLE
  Invoking without specifying any rules causes all rules to be run.
  	E.g., $ sfdx scanner:run --format xml --target "somefile.js"
  		Evaluates all rules against somefile.js.

  Specifying multiple categories or rulesets is treated as a logical OR.
  	E.g., $ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices" --ruleset "Braces"
  		Evaluates all rules in the Design and Best Practices categories, and all rules in the Braces ruleset.

  Wrap globs in quotes.
  	Unix example:    $ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
  	Windows example: > sfdx scanner:run --target ".\**\*.js,!.\**\IgnoreMe.js" ...
  		Evaluate rules against all .js files below the current directory, except for IgnoreMe.js.
```
_See code: [lib/commands/scanner/run.js](https://github.com/forcedotcom/sfdx-scanner/blob/v1.0.30/lib/commands/scanner/run.js)_

## `sfdx scanner:scannerCommand [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

```
USAGE
  $ sfdx scanner:scannerCommand [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation
```
_See code: [lib/commands/scanner/scannerCommand.js](https://github.com/forcedotcom/sfdx-scanner/blob/v1.0.30/lib/commands/scanner/scannerCommand.js)_

<!-- commandsstop -->


Static code scanner that applies quality and security rules to Apex code, and provides feedback.

[![Version](https://img.shields.io/npm/v/scanner.svg)](https://npmjs.org/package/scanner)
[![CircleCI](https://circleci.com/gh/forcedotcom/sfdx-scanner/tree/master.svg?style=shield)](https://circleci.com/gh/forcedotcom/sfdx-scanner/tree/master)
[![Appveyor CI](https://ci.appveyor.com/api/projects/status/github/forcedotcom/sfdx-scanner?branch=master&svg=true)](https://ci.appveyor.com/project/heroku/sfdx-scanner/branch/master)
[![Codecov](https://codecov.io/gh/forcedotcom/sfdx-scanner/branch/master/graph/badge.svg)](https://codecov.io/gh/forcedotcom/sfdx-scanner)
[![Greenkeeper](https://badges.greenkeeper.io/forcedotcom/sfdx-scanner.svg)](https://greenkeeper.io/)
[![Known Vulnerabilities](https://snyk.io/test/github/forcedotcom/sfdx-scanner/badge.svg)](https://snyk.io/test/github/forcedotcom/sfdx-scanner)
[![Downloads/week](https://img.shields.io/npm/dw/scanner.svg)](https://npmjs.org/package/scanner)
[![License](https://img.shields.io/npm/l/scanner.svg)](https://github.com/forcedotcom/sfdx-scanner/blob/master/package.json)

<!-- toc -->
* [Debugging your plugin](#debugging-your-plugin)
* [sfdx-scanner](#sfdx-scanner)
<!-- tocstop -->
<!-- install -->
<!-- usage -->
```sh-session
$ npm install -g scanner
$ sfdx COMMAND
running command...
$ sfdx (-v|--version|version)
scanner/0.0.0 darwin-x64 node-v12.16.1
$ sfdx --help [COMMAND]
USAGE
  $ sfdx COMMAND
...
```
<!-- usagestop -->
<!-- commands -->
* [`sfdx scanner:rule:add -l <string> -p <array> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerruleadd--l-string--p-array---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:rule:describe -n <string> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerruledescribe--n-string---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerrulelist--c-array--r-array--l-array---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:run [-c <array>] [-r <array>] [-s <array> | undefined] [-f xml|csv|table | -o <string>] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerrun--c-array--r-array--s-array--undefined--f-xmlcsvtable---o-string---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:scannerCommand [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerscannercommand---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)

## `sfdx scanner:rule:add -l <string> -p <array> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

Add custom rules to use while scanning. Rules should have been compiled and tested separately. Users can refer to PMD’s documentation on information and examples to write your own custom rules: https://pmd.github.io/latest/pmd_userdocs_extending_writing_pmd_rules.html

```
USAGE
  $ sfdx scanner:rule:add -l <string> -p <array> [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -l, --language=language
      (required) Programming language for which custom rules are added.

  -p, --path=path
      (required) Comma-separated list to paths that lead to custom rule definitions. These paths could be one or more of:
      1. Jar file with compiled rule classes and one or more Rule definition XML file(s)
      2. Directory to multiple jar files, each with compiled rule classes. Rule definition XML file(s) could be part of 
      the jar files or directly placed under the directory
      3. Directory with package-structured rule classes and Rule definition XML file(s) at some level

      To distinguish Rulesets XML vs Category XML:
      1. Ensure that rulesets XML have “rulesets” in the directory path
      2. XMLs that do not have “rulesets” in the directory path would be handled as Category XML by default

  --json
      format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)
      [default: warn] logging level for this command invocation

EXAMPLE
  $ sfdx scanner:rule:add --language "apex" --path "/dir/to/jar/lib"
           (todo: add sample output here)

           $ sfdx scanner:rule:add --language "apex" --path "/file/path/to/customrule.jar,/dir/to/jar/lib"
           (todo: add sample output here)
```

_See code: [lib/commands/scanner/rule/add.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/rule/add.js)_

## `sfdx scanner:rule:describe -n <string> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

[Description of 'describe' command]

```
USAGE
  $ sfdx scanner:rule:describe -n <string> [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -n, --rulename=rulename                                                           (required) [Description of
                                                                                    'rulename' parameter]

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

EXAMPLES
  $ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
     Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
     My hub org id is: 00Dxx000000001234
  
  $ sfdx hello:org --name myname --targetusername myOrg@example.com
     Hello myname! This is org: MyOrg and I will be around until Tue Mar 20 2018!
```

_See code: [lib/commands/scanner/rule/describe.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/rule/describe.js)_

## `sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

[Description of 'list' command]

```
USAGE
  $ sfdx scanner:rule:list [-c <array>] [-r <array>] [-l <array>] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -c, --category=category                                                           [Description of 'category'
                                                                                    parameter]

  -l, --language=language                                                           [Description of 'language'
                                                                                    parameter]

  -r, --ruleset=ruleset                                                             [Description of 'ruleset' parameter]

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

EXAMPLES
  $ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
     Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
     My hub org id is: 00Dxx000000001234
  
  $ sfdx hello:org --name myname --targetusername myOrg@example.com
     Hello myname! This is org: MyOrg and I will be around until Tue Mar 20 2018!
```

_See code: [lib/commands/scanner/rule/list.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/rule/list.js)_

## `sfdx scanner:run [-c <array>] [-r <array>] [-s <array> | undefined] [-f xml|csv|table | -o <string>] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

[Description of 'run' command]

```
USAGE
  $ sfdx scanner:run [-c <array>] [-r <array>] [-s <array> | undefined] [-f xml|csv|table | -o <string>] [--json] 
  [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -c, --category=category                                                           [Description of 'category'
                                                                                    parameter]

  -f, --format=(xml|csv|table)                                                      [Description of 'format' parameter]

  -o, --outfile=outfile                                                             [Description of 'outfile' parameter]

  -r, --ruleset=ruleset                                                             [Description of 'ruleset' parameter]

  -s, --source=source                                                               [Description of 'source' parameter]

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

EXAMPLE
  $ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
     Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
     My hub org id is: 00Dxx000000001234
```

_See code: [lib/commands/scanner/run.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/run.js)_

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

_See code: [lib/commands/scanner/scannerCommand.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/scannerCommand.js)_
<!-- commandsstop -->
<!-- debugging-your-plugin -->
# Debugging your plugin
We recommend using the Visual Studio Code (VS Code) IDE for your plugin development. Included in the `.vscode` directory of this plugin is a `launch.json` config file, which allows you to attach a debugger to the node process when running your commands.

To debug the `hello:org` command: 
1. Start the inspector
  
If you linked your plugin to the sfdx cli, call your command with the `dev-suspend` switch: 
```sh-session
$ sfdx hello:org -u myOrg@example.com --dev-suspend
```
  
Alternatively, to call your command using the `bin/run` script, set the `NODE_OPTIONS` environment variable to `--inspect-brk` when starting the debugger:
```sh-session
$ NODE_OPTIONS=--inspect-brk bin/run hello:org -u myOrg@example.com
```

2. Set some breakpoints in your command code
3. Click on the Debug icon in the Activity Bar on the side of VS Code to open up the Debug view.
4. In the upper left hand corner of VS Code, verify that the "Attach to Remote" launch configuration has been chosen.
5. Hit the green play button to the left of the "Attach to Remote" launch configuration window. The debugger should now be suspended on the first line of the program. 
6. Hit the green play button at the top middle of VS Code (this play button will be to the right of the play button that you clicked in step #5).
<br><img src=".images/vscodeScreenshot.png" width="480" height="278"><br>
Congrats, you are debugging!
=======
# sfdx-scanner

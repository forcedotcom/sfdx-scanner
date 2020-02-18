[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-Ready--to--Code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/forcedotcom/sfdx-scanner) 


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
scanner/0.0.0 darwin-x64 node-v12.14.1
$ sfdx --help [COMMAND]
USAGE
  $ sfdx COMMAND
...
```
<!-- usagestop -->
<!-- commands -->
* [`sfdx scanner:rule:describe -n <string> [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerruledescribe--n-string---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:rule:list [-c <array>] [-r <array>] [-s <string>] [-l <array>] [--standard | --custom] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerrulelist--c-array--r-array--s-string--l-array---standard----custom---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:run [-n <string> | -c <array> | -r <array> | -s <string> | --exclude-rule <array>] [-a <string> | -f <array> | -d <array> | -x <array>] [--suppress-warnings] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerrun--n-string---c-array---r-array---s-string----exclude-rule-array--a-string---f-array---d-array---x-array---suppress-warnings---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)
* [`sfdx scanner:scan [-R <string>] [-d <string>] [-r <string>] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`](#sfdx-scannerscan--r-string--d-string--r-string---json---loglevel-tracedebuginfowarnerrorfataltracedebuginfowarnerrorfatal)

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

## `sfdx scanner:rule:list [-c <array>] [-r <array>] [-s <string>] [-l <array>] [--standard | --custom] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

[Description of 'list' command]

```
USAGE
  $ sfdx scanner:rule:list [-c <array>] [-r <array>] [-s <string>] [-l <array>] [--standard | --custom] [--json] 
  [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -c, --category=category                                                           [Description of 'category'
                                                                                    parameter]

  -l, --language=language                                                           [Description of 'language'
                                                                                    parameter]

  -r, --ruleset=ruleset                                                             [Description of 'ruleset' parameter]

  -s, --severity=severity                                                           [Description of 'severity'
                                                                                    parameter]

  --custom                                                                          [Description of 'custom' parameter]

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

  --standard                                                                        [Description of 'standard'
                                                                                    parameter]

EXAMPLES
  $ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
     Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
     My hub org id is: 00Dxx000000001234
  
  $ sfdx hello:org --name myname --targetusername myOrg@example.com
     Hello myname! This is org: MyOrg and I will be around until Tue Mar 20 2018!
```

_See code: [lib/commands/scanner/rule/list.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/rule/list.js)_

## `sfdx scanner:run [-n <string> | -c <array> | -r <array> | -s <string> | --exclude-rule <array>] [-a <string> | -f <array> | -d <array> | -x <array>] [--suppress-warnings] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

[Description of 'run' command]

```
USAGE
  $ sfdx scanner:run [-n <string> | -c <array> | -r <array> | -s <string> | --exclude-rule <array>] [-a <string> | -f 
  <array> | -d <array> | -x <array>] [--suppress-warnings] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -a, --org=org                                                                     [Description of 'org' parameter]

  -c, --category=category                                                           [Description of 'category'
                                                                                    parameter]

  -d, --directory=directory                                                         [Description of 'directory'
                                                                                    parameter]

  -f, --file=file                                                                   [Description of 'file' parameter]

  -n, --rulename=rulename                                                           [Description of 'rulename'
                                                                                    parameter]

  -r, --ruleset=ruleset                                                             [Description of 'ruleset' parameter]

  -s, --severity=severity                                                           [Description of 'severity'
                                                                                    parameter]

  -x, --exclude=exclude                                                             [Description of 'exclude' parameter]

  --exclude-rule=exclude-rule                                                       [Description of 'exclude-rule'
                                                                                    parameter]

  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

  --suppress-warnings                                                               [Description of 'suppress-warnings'
                                                                                    parameter]

EXAMPLE
  $ sfdx hello:org --targetusername myOrg@example.com --targetdevhubusername devhub@org.com
     Hello world! This is org: MyOrg and I will be around until Tue Mar 20 2018!
     My hub org id is: 00Dxx000000001234
```

_See code: [lib/commands/scanner/run.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/run.js)_

## `sfdx scanner:scan [-R <string>] [-d <string>] [-r <string>] [--json] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]`

scan local code for violations of rules

```
USAGE
  $ sfdx scanner:scan [-R <string>] [-d <string>] [-r <string>] [--json] [--loglevel 
  trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]

OPTIONS
  -R, --ruleset=ruleset                                                             ruleset to use for scanning
  -d, --filepath=filepath                                                           file path to examine
  -r, --report=report                                                               file to save output report
  --json                                                                            format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)  [default: warn] logging level for
                                                                                    this command invocation

EXAMPLE
  $ sfdx scanner:scan --ruleset "/my/ruleset/file/location" --filepath "/my/code/files/to/be/scanned"
           (todo: add sample output here)

           $ sfdx scanner:scan -R "/my/ruleset/file/location" -d "/my/code/files/to/be/scanned"
           (todo: add sample output here)
```

_See code: [lib/commands/scanner/scan.js](https://github.com/forcedotcom/sfdx-scanner/blob/v0.0.0/lib/commands/scanner/scan.js)_
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

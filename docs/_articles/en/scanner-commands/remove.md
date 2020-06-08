---
title: SFDX Scanner Command Reference
lang: en
---

## sfdx scanner:rule:remove
Removes custom rules from the registry of available rules. Use the --path parameter to specify one or more paths to remove, or omit it to receive a list of all valid custom paths.

## Usage

```bash
$ sfdx scanner:rule:remove [-f] [-p <array>] [--verbose] [--json]
```
  
## Options

```bash
  -f, --force		bypass the confirmation prompt and immediately unregister the rules
  -p, --path=path	one or more paths to deregister
  --json      		format output as json
  --verbose      	emit additional command output to stdout
```
  
## Example

Run the command with no arguments to see a list of all currently registered custom paths.
```bash
$ sfdx scanner:rule:remove
```

You may use the --path parameter to specify one or more paths to remove. It deregisters the rules defined in somerules.jar and any JARs contained in the rules folder.
  
```bash
$ sfdx scanner:rule:remove --path "~/path/to/somerules.jar,~/path/to/folder/containing/rules"
```  
  		
  By default, a list of all the rules that will be deregistered is displayed, and the action must be confirmed. The --force flag may be used to bypass that confirmation. 
```bash
$ sfdx scanner:rule:remove --force --path "~/path/to/somerules.jar"
```

## Demo
![Describe Example](./assets/images/remove.gif) 

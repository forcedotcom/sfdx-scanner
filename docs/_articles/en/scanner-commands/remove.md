---
title: Salesforce CLI Scanner Plug-In Command Reference
lang: en
---

## sfdx scanner:rule:remove
Removes custom rules from the registry of available rules. Use the ```-p|--path``` parameter to specify one or more paths to remove. If you don't specify any parameters, the command lists all valid custom paths but doesn't remove any.

## Usage

```bash
$ sfdx scanner:rule:remove [-f] [-p <array>] [--verbose] [--json]
```
  
## Options

```bash
  -f, --force		Bypass the confirmation prompt and immediately remove the rules
  -p, --path=path	One or more paths to remove
  --json      		Format output as json
  --verbose      	Emit additional command output to stdout
```
  
## Example

Run the command with no parameters to see a list of all currently registered custom paths.
```bash
$ sfdx scanner:rule:remove
```

Use the ```-p|--path``` parameter to specify the path or paths you want to remove from the registry. This example removes the rules defined in ```somerules.jar``` and ```myrules.xml```, and all JARs/XMLs contained in the ```rules``` folder.
  
```bash
$ sfdx scanner:rule:remove --path "~/path/to/somerules.jar,~/path/to/category/apex/myrules.xml,~/path/to/folder/containing/rules"
```  
  		
By default, this command lists the rules that will be removed and prompts you for confirmation. Use the ```-f|--force``` flag to bypass that confirmation. 
```bash
$ sfdx scanner:rule:remove --force --path "~/path/to/somerules.jar"
```

## Demo
![Remove Example](./assets/images/remove.gif) 

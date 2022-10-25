---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/add
---

## sfdx scanner:rule:add
Add custom rules to the Salesforce Code Analyzer's registry to run them along with the built-in rules. Compile and test custom rules separately before adding them.

See [Authoring Custom Rules](./en/v3.x/custom-rules/author/) for more information.

## Usage

```bash
$ sfdx scanner:rule:add -l <string> -p <array> [--json]
```
  
## Options

```bash
  -l, --language=language	(required) Language that the custom rules are evaluated against.
  -p, --path=path		(required) One or more paths (such as a directory or JAR file) to custom rule definitions. Specify multiple values as a comma-separated list.
  --json			Format output as json.

```
  
## Example
Add XPath-only custom PMD rules as standalone XML files. Java-based rules must be bundled in JAR files. Be sure to adhere to PMD conventions, such as defining the custom rules in XML fils under a ```/category``` directory.

See the [PMD documentation](https://pmd.github.io/latest/pmd_userdocs_extending_writing_pmd_rules.html) for information about writing rules. 
  
This example shows how to specify two files directly.
```bash
$ sfdx scanner:rule:add --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/category/apex/MyRules.xml"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/Jar1.jar,/Users/me/rules/category/apex/MyRules.xml
```

This example shows how to specify a directory that contains one or more rule files, all of which are added to the registry.
```bash
$ sfdx scanner:rule:add --language apex --path "/Users/me/rules"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/SomeJar.jar,/Users/me/rules/category/apex/MyRules.xml
```

## Demo
![Add Example](./assets/images/add.gif) 

---
title: Salesforce CLI Scanner Plug-In Command Reference
lang: en
---

## sfdx scanner:rule:add
Add custom rules to the Salesforce CLI Scanner plug-in's registry so you can run them along with the built-in rules. Compile and test custom rules separately before adding them.

See [Authoring Custom Rules](./en/custom-rules/author/) for more information.

## Usage

```bash
$ sfdx scanner:rule:add -l <string> -p <array> [--json]
```
  
## Options

```bash
  -l, --language=language	(required) Language against which the custom rules will evaluate
  -p, --path=path		(required) One or more paths (such as a directory or JAR file) to custom rule definitions. Specify multiple values as a comma-separated list.
  --json			Format output as json

```
  
## Example
Bundle custom PMD rules in JAR files. Be sure to adhere to PMD conventions, such as defining the custom rules in XML files under a ```/category``` directory.

See the [PMD documentation](https://pmd.github.io/latest/pmd_userdocs_extending_writing_pmd_rules.html) for information about writing rules. 
  
This example shows how to specify two JAR files directly.
```bash
$ sfdx scanner:rule:add --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/Jar2.jar"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/SomeJar.jar,/Users/me/rules/AnotherJar.jar
```

This example shows how to specify a directory that contains one or more JAR files, all of which are added to the registry.
```bash
$ sfdx scanner:rule:add --language apex --path "/Users/me/rules"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/SomeJar.jar,/Users/me/rules/AnotherJar.jar
```

## Demo
![Add Example](./assets/images/add.gif) 

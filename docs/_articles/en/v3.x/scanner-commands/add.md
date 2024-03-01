---
title: Salesforce Code Analyzer Command Reference
lang: en
redirect_from: /en/scanner-commands/add
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/add.html
---

## sf scanner rule add
Adds custom rules to Salesforce Code Analyzer’s registry so that you can run them along with the built-in rules. Compile and test custom rules separately before adding them.

See [Authoring Custom Rules](./en/v3.x/custom-rules/author/) for more information.

## Usage

```bash
$ sf scanner rule add -l <string> -p <array> [--json]
```
  
## Options

```bash
  -l, --language=language	(required) Language that the custom rules are evaluated against.
  -p, --path=path		(required) One or more paths (such as a directory or JAR file) to custom rule definitions. Specify multiple values as a comma-separated list.
  --json			Formats output as JSON.

```
  
## Example
Bundle custom PMD rules in JAR files. Follow PMD conventions, such as defining the custom rules in XML files under a ```/category``` directory.

See PMD's documentation for more information on writing rules.

This example shows how to specify two JAR files directly. You can also specify a directory containing one or more JARs, all of which will be added.

```bash
$ sf scanner rule add --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/category/apex/MyRules.xml"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/Jar1.jar,/Users/me/rules/category/apex/MyRules.xml
```

This example shows how to specify a directory that contains one or more JAR files, all of which are added to the registry. 

```bash
$ sf scanner rule add --language apex --path "/Users/me/rules"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/SomeJar.jar,/Users/me/rules/category/apex/MyRules.xml
```

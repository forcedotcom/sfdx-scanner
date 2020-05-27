---
title: SFDX Scanner Command Reference
lang: en
---

## sfdx scanner:rule:add
Add custom rules to Sfdx Scanner's registry to run them along with built-in rules. Rules should have been compiled and tested separately.

Please refer to our Custom Rules [help page](./en/custom-rules/author/) for more information.

## Usage

```bash
$ sfdx scanner:rule:add -l <string> -p <array> [--json]
```
  
## Options

```bash
  -l, --language=language	(required) language against which the custom rules will evaluate
  -p, --path=path		(required) One or more paths to custom rule definitions. Specify multiple values with a comma-separated list.
  --json			format output as json

```
  
## Example
PMD: Custom PMD rules should be in JARs. Adhere to PMD conventions, including defining rules in XMLs under a /category directory.
  Refer to PMD's documentation for information on writing rules: [here](https://pmd.github.io/latest/pmd_userdocs_extending_writing_pmd_rules.html)
  
You may specify one or more JARs directly.
```bash
$ sfdx scanner:rule:add --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/Jar2.jar"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/SomeJar.jar,/Users/me/rules/AnotherJar.jar
```

You may also specify a directory containing one or more JARs, all of which will be added.
```bash
$ sfdx scanner:rule:add --language apex --path "/Users/me/rules"
         Successfully added rules for apex.
         2 path(s) added:
         /Users/me/rules/SomeJar.jar,/Users/me/rules/AnotherJar.jar
```

## Demo

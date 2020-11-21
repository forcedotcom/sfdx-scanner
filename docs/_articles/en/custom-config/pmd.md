---
title: PMD Custom Configuration
lang: en
---

## Introduction
PMD experts, who use the Scanner plugin, may prefer to apply their specialized knowledge to build their own rule reference file. [PMD's custom rule reference XML](https://pmd.github.io/latest/pmd_userdocs_making_rulesets.html) is most helpful when you wish to run a custom subset of rules or define property values for rules. You can invoke this feature by executing `scanner:run` command with `--pmdconfig` flag and passing the path to your rule reference file.
```bash
$ sfdx scanner:run —target “/path/to/your/target” —pmdconfig “/path/to/rule_reference.xml”
```

However, using this feature modifies some capabilities of the Scanner plugin.

## Restrictions to Scanner Plugin

1. Rule filters such as `--category` and `--ruleset` will not be evaluated.
2. Rules referenced by the XML should already be supported by the Scanner plugin. If you wish to reference to custom rules, make sure that these rules have already been added to the plugin through `scanner:rule:add` command
3. When `--pmdconfig` is passed in, default PMD rules will not be run. However default Eslint rules will continue to run if applicable.
5. You can use engine filters along with `--pmdconfig`. Here's a example situation:
```bash
$ sfdx scanner:run --engine "eslint-typescript,pmd" --pmdconfig "/path/to/ruleref.xml" --target "/path/to/target"
```
This would run `eslint-typescript` with default settings and `pmd` with the custom rule reference.

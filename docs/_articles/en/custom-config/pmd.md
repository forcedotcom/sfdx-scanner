---
title: PMD Custom Configuration
lang: en
---

## PMD Custom Configuration
Apply your PMD knowledge to build your own Salesforce Code Analyzer (Code Analyzer) rule reference file. Use PMD to run a custom subset of rules or define property values for rules. 

You can:

* Create rulesets, modify properties, or explicitly disable existing rules. 
* Create Xpath rules, and reference new rules in ruleset configurations.

Your new rules or rulesets must be in XML format. Add new rules or rulesets using ```scanner:rule:add```.

To invoke your rulesets in Code Analyzer, run ```scanner:run --pmdconfig```, and pass the path to your rule reference file.

```$ sfdx scanner:run —target “/path/to/your/target” —pmdconfig “/path/to/rule_reference.xml”```

You can specify the rules that you invoke on PMD using ```--pmdconfig```. Using ```–pmdconfig``` causes other filter parameters to be ignored, such as ```–category``` and ```–ruleset```. 

Example:

```$ sfdx scanner:run --engine "eslint-typescript,pmd" --pmdconfig "/path/to/ruleref.xml" --target "/path/to/target"```


## PMD Restrictions

PMD rulesets in Code Analyzer have these restrictions.

* Rule filters such as ```--category``` and ```--ruleset``` aren’t evaluated.
* If your PMD ruleset contains custom rules, first run ```scanner:rule:add``` to add your custom rules to Code Analyzer to ensure that your rules are supported by Code Analyzer.
* When ```--pmdconfig``` is used, default PMD rules don’t run. However, default ESLint rules continue to run.

## See Also

- [PMD Source Code Analyzer Project: Making rulesets](https://pmd.github.io/latest/pmd_userdocs_making_rulesets.html)
- [Xpath: Writing Xpath rules](https://pmd.github.io/latest/pmd_userdocs_extending_writing_xpath_rules.html)


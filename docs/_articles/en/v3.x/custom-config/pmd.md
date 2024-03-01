---
title: PMD Custom Configuration
lang: en
redirect_from: /en/custom-config/pmd
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/pmd-config.html
---

## PMD Custom Configuration
By default, Salesforce Code Analyzer's catalog of PMD rules includes PMD’s built-in rules and any custom rules that you added with `scanner rule add`. Filter which rules are run using flags like `--category`. 

To replace the catalog with your own custom catalog, use `--pmdconfig`. With this flag, you can:

* Indicate a specific set of rules to run without using filtering flags like `--category`.
* Modify the properties of existing rules to meet your needs.
* Define and run new XPath-based rules without permanently adding them to the catalog.

Create your custom rules file just like another other PMD category or [ruleset file](https://docs.pmd-code.org/latest/pmd_userdocs_making_rulesets.html#creating-a-ruleset). Keep these limitations in mind.

* PMD's built-in rules can be freely referenced with the ref property.
* Custom rules defined in other files can be referenced through the ref property only if they’re bundled into a JAR as described in [Authoring Custom Rules](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/custom-rules/author/) and registered via `scanner rule add` as described in [Managing Custom Rules](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/custom-rules/manage/). This constraint applies to XPath-based and Java-based rules.
* Custom XPath-based rules can be declared inline.

**Note**: Because `--pmdconfig` replaces our catalog with yours, rule filter flags such as `--category` and `--ruleset` aren’t applied to PMD rules, though they’re still applied to other engines. Only the PMD rules that you specify in your config file are evaluated.

To invoke your rulesets in Code Analyzer, run `scanner run --pmdconfig “filename”`, and pass the path to your rule reference file.

```$ sf scanner run —target “/path/to/your/target” —pmdconfig “/path/to/rule_reference.xml”```

You can specify the rules that you invoke on PMD using `--pmdconfig`. Using `–pmdconfig` causes other filter parameters to be ignored, such as `–category` and `–ruleset`.

**Example**:

```$ sf scanner run --engine "eslint-typescript,pmd" --pmdconfig "/path/to/ruleref.xml" --target "/path/to/target"```

## See Also

- [PMD Source Code Analyzer Project: Making rulesets](https://pmd.github.io/latest/pmd_userdocs_making_rulesets.html)
- [Xpath: Writing Xpath rules](https://pmd.github.io/latest/pmd_userdocs_extending_writing_xpath_rules.html)

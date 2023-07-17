---
title: Authoring Custom Rules
lang: en
redirect_from: /en/custom-rules/author
---

## What are Custom Rules?

Let’s say your codebase has specific and repetitive coding issues that you want to address. You use the built-in rules of Salesforce Code Analyzer (Code Analyzer) to find these rule violations. But sometimes the problems exist only in the context of your codebase, and sometimes Code Analyzer’s built-in rules don’t catch them. In this case, create your own custom rules to highlight these issues as rule violations when you scan your code.

Because PMD and ESLint custom rules work differently, Code Analyzer deals with each in distinct ways. 

---

## PMD Custom Rules

### Write PMD Custom Rules

To be compatible with Code Analyzer, your PMD custom rules must meet these guidelines.

* Declare your new XPath or Java-based rules in custom category XML files using this format: `<some base dir>/category/<language>/<filename>.xml`. For more info, read PMD’s [XML rule definition](https://docs.pmd-code.org/latest/pmd_userdocs_extending_writing_rules_intro.html#xml-rule-definition) documentation.
* Combine your custom rules and PMD's built-in rules into custom rulesets in XML files using this format: `<some base dir>/rulesets/<language>/<filename>.xml`. For more info, read PMD’s [Making rulesets](https://docs.pmd-code.org/latest/pmd_userdocs_making_rulesets.html#referencing-a-single-rule).
* Register custom rulesets or [XPath-only](https://docs.pmd-code.org/latest/pmd_userdocs_extending_writing_xpath_rules.html) custom categories with Code Analyzer as standalone XML files.
* Compile your Java-based rules, and bundle them into a JAR along with your rule declaration files, then register that JAR with Code Analyzer. We recommend using a build tool such as [Maven](https://maven.apache.org/plugins/maven-jar-plugin/) or [Gradle](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html) to manage the dependency on PMD and automate the process of building the JAR. Use our <link>example repo</link> as a scaffold to build your own project.

Refer to [Managing Custom Rules](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/custom-rules/manage/#pmd-custom-rules) for information about how to register and run your custom PMD rules.

### Bundle Java-Based PMD Custom Rules
If you haven’t already, create an XML rule definition file for your new rules. Add your rules to a directory structure like this: 

```
<some base dir>/category/<language>/yourRuleDefinition.xml
```

After your new Java files compile and your XML rule definition matches your new custom rules that you created, create a JAR file that contains the XML and the class files. Use the correct directory structure according to its package name and the XML file in the directory path. A single JAR file can contain multiple custom rule classes.

Example:

```
$ jar -cp <customRule.jar> <rule_package_base_dir> <xml_base_dir>
```
---

## See Also

- [PMD Source Code Analyzer Project: Introduction to writing PMD rules](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html)
- [PMD Source Code Analyzer Project: XML rule definition](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html#xml-rule-definition)


## ESLint Custom Rules

### Write ESLint Custom Rules

Writing custom ESLint rules requires creating a custom ESLint plug-in and defining rules within it. For help with writing ESLint rules, refer to ESLint’s [Working with Rules](https://eslint.org/docs/developer-guide/working-with-rules) documentation.

### Add Rules as an npm Dependency

ESLint rule definitions must contain documentation. Format your documentation like this:

```bash
// Rule definition in index.js or where you choose to store it
...
    meta: {
        docs: {
            description: "Information about the rule"
        },
		...
	}
...
```

After your rule is ready and tested, add it as a dependency to the ```npm``` setup in the same directory where Code Analyzer runs. Use the ```npm``` or ```yarn``` version of this command.

```bash
yarn add file:/path/to/ESLint-plugin-my-custom
npm install file:/path/to/ESLint-plugin-my-custom
```

After your rule is added, ensure that the ```node_modules``` directory:
* Has a child directory with your plug-in’s name. 
* Contains the ```package.json```, ```index.js``` and other files that you created.

## See Also
- [ESLint: Working with Rules](https://eslint.org/docs/latest/developer-guide/working-with-rules)

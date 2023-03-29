---
title: Authoring Custom Rules
lang: en
redirect_from: /en/custom-rules/author
---

## What are Custom Rules?

Let’s say your codebase has specific and repetitive coding issues that you want to address. You use the built-in rules of the Salesforce Code Analyzer (Code Analyzer) to find these rule violations. But sometimes the problems exist only in the context of your codebase, and sometimes Code Analyzer’s built-in rules don’t catch them. In this case, create your own custom rules to highlight these issues as rule violations when you scan your code.

Because PMD and ESLint custom rules work differently, Code Analyzer deals with each in distinct ways. 

---

## PMD Custom Rules

### Write PMD Custom Rules

To be compatible with Code Analyzer, your PMD custom rules must meet these criteria.

* PMD Rules must be XPath-based or Java-based. 
* Define rules in XML files with a path that matches this format: ```<some base dir>/category/<language>/<filename>.xml```.
* XPath-based rules can be contained in standalone XML files.
* Java-based rules must be compiled and bundled into a JAR.
* Custom rulesets consisting of references to existing rules can be contained in standalone XML files with a path that matches this format: ```<some base dir>/rulesets/<language>/<filename>.xml```.

### Compile Java-Based PMD Custom Rules
To compile your new Java-based PMD rules:

* Add ```$PMD_BIN_HOME/lib/*``` to your CLASSPATH. 
* Reflect the ```java-home path``` in your Java setup in ```<HOME_DIR>/.sfdx-scanner/Config.json```.
* Use {{ site.data.versions-v3.pmd }} to write your custom rules.
* If you’re using an integrated development environment (IDE), add ```$PMD_BIN_HOME/lib/*``` to its CLASSPATH. To compile from the command line, use the ```javac``` command. 

	Example:
	
	```
	$ javac -cp ".:$PMD_BIN_HOME/lib/*" /path/to/your/Rule.java
	```

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

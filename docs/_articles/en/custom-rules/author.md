---
title: Authoring Custom Rules
lang: en
---

## What are Custom Rules?

Let's say your codebase has specific and repetitive coding issues that you want to address and clean up. Ideally you'd use the built-in rules of the Salesforce CLI Scanner to find these rule violations. But sometimes the problems exist only in the context of your codebase, and the built-in rules may not catch them. In this case, create your own _custom rules_ to highlight these issues as rule violations when you scan your code.

PMD and Eslint's custom rules work very differently. This causes Scanner plugin to deal with both types in distinctly different ways. Please note that information related to PMD Custom Rules does not apply to Eslint Custom Rules.

---

## PMD Custom Rules

### Writing PMD Custom Rules

Here are the [instructions](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html) on how to write PMD Custom Rules.

### Compiling PMD Custom Rules
When you compile your new rule(s), make sure ```$PMD_BIN_HOME/lib/*``` is in your CLASSPATH. Also make sure that your Java setup reflects the java-home path in ```<HOME_DIR>/.sfdx-scanner/Config.json```.  

If you are using an IDE, add ```$PMD_BIN_HOME/lib/*``` to its CLASSPATH. To compile from the command line, use the ```javac``` command. For example:

```$ javac -cp ".:$PMD_BIN_HOME/lib/*" /path/to/your/Rule.java```

Use only [version {{ site.data.versions.pmd }}](https://github.com/pmd/pmd/releases/download/pmd_releases%2F{{ site.data.versions.pmd }}/pmd-bin-{{ site.data.versions.pmd }}.zip) of PMD for writing the custom rules. 

### Bundling PMD Custom Rules
If you haven't already, create the XML rule [definition file](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html#xml-rule-definition) for your new rule(s). Add them to a directory structure like this: 
```<some base dir>/category/<language>/yourRuleDefinition.xml```

When your new Java files compile and your XML rule definition matches your new custom rule(s) that you’ve created, create a JAR file that contains both the xml and the class files. Be sure to use the correct directory structure according to its package name and the XML file in the directory path as described in the previous step. A single JAR file can contain multiple custom rule classes.

Here's an example of using ```jar``` to create a JAR file: 

```$ jar -cp <customRule.jar> <rule_package_base_dir> <xml_base_dir>```

---

## Eslint Custom Rules

### Writing Eslint Custom Rules

Writing custom Eslint rules requires creating a custom Eslint plugin and defining rules within it. Here is [Eslint's official documentation on writing rules](https://eslint.org/docs/developer-guide/working-with-rules). Also, there are many tutorials and blogs that explain the process clearly. In this documentation, we'll focus the specific elements that help the rule work with the Scanner plugin.

### Adding Rule as NPM Dependency

While rule is written, please make sure the rule definition contains documentation. We are specifically looking for a format like this:
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

Once the rule is ready and tested, add it as a dependency to the NPM setup in the directory where you to plan to run the Scanner plugin from. You can use `npm` or `yarn` version of this command:
```bash
yarn add file:/path/to/eslint-plugin-my-custom
```

Once added, make sure the `node_modules` directory has child directory with your plugin's name and this directory contains the `package.json`, `index.js` and other files you had created.
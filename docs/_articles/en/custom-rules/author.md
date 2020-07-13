---
title: Authoring Custom Rules
lang: en
---

## What are Custom Rules?

Let's say your codebase has specific and repetitive coding issues that you want to address and clean up. Ideally you'd use the built-in rules of the Salesforce CLI Scanner to find these rule violations. But sometimes the problems exist only in the context of your codebase, and the built-in rules may not catch them. In this case, create your own _custom rules_ to highlight these issues as rule violations when you scan your code. 

The scanner plug-in currently supports only PMD custom rules.

## Writing PMD Custom Rules

Here are the [instructions](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html) on how to write PMD Custom Rules.

## Compiling PMD Custom Rules
When you compile your new rule(s), make sure ```$PMD_BIN_HOME/lib/*``` is in your CLASSPATH. Also make sure that your Java setup reflects the java-home path in ```<HOME_DIR>/.sfdx-scanner/Config.json```.  

If you are using an IDE, add ```$PMD_BIN_HOME/lib/*``` to its CLASSPATH. To compile from the command line, use the ```javac``` command. For example:

```$ javac -cp ".:$PMD_BIN_HOME/lib/*" /path/to/your/Rule.java```

Use only [version 6.22.0](https://github.com/pmd/pmd/releases/download/pmd_releases%2F6.22.0/pmd-bin-6.22.0.zip) of PMD for writing the custom rules. 

## Bundling PMD Custom Rules
If you haven't already, create the XML rule [definition file](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html#xml-rule-definition) for your new rule(s). Add them to a directory structure like this: 
```<some base dir>/category/<language>/yourRuleDefinition.xml```

When your new Java files compile and your XML rule definition matches your new custom rule(s) that you’ve created, create a JAR file that contains both the xml and the class files. Be sure to use the correct directory structure according to its package name and the XML file in the directory path as described in the previous step. A single JAR file can contain multiple custom rule classes.

Here's an example of using ```jar``` to create a JAR file: 

```$ jar -cp <customRule.jar> <rule_package_base_dir> <xml_base_dir>```

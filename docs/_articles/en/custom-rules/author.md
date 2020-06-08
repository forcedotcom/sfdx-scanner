---
title: Authoring Custom Rules
lang: en
---

## What are Custom Rules?

Any code base could have __specific__, repetitive coding issue that you would like to address and clean up. A Rule Violation from Sfdx Scanner would be ideal, but this is possibly a problem only in your codebase’s context.

You can create your own rules to highlight these issues as Rule Violations when you scan your code. We call these rules as __Custom Rules__. Currently the Sfdx Scanner only supports Custom Rules for PMD.

## Writing PMD Custom Rules

Each of the engines that are used to scan your code have different ways to write Custom Rules. You can find [instructions](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html) for PMD. 

## Compiling PMD Custom Rules
You can write the new rules according to PMD’s instructions and compile your new rule(s) with ```$PMD_BIN_HOME/lib/*``` in the classpath. Make sure that the Java setup that you use for compiling your rules is the same as the java-home path in ```<HOME_DIR>/.sfdx-scanner/Config.json.```

If you are using an IDE, add ```$PMD_BIN_HOME/lib/*``` to classpath. To compile from command line. 
```$ javac -cp ".:$PMD_BIN_HOME/lib/*" /path/to/your/Rule.java```

## Bundling PMD Custom Rules
Create the XML rule [definition file](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html#xml-rule-definition) for your new rule(s), if you haven’t yet. Place them in a directory structure like this: 
```<some base dir>/category/<language>/yourRuleDefinition.xml```

Once your new Java files compile and your XML rule definition matches the Custom Rule(s) that you’ve created, create a JAR file that contains your class files in the correct directory structure according to its package name and the XML file in the directory path as described in the previous step. A single JAR file could have multiple Rule classes.

Jar file creation command: 
```jar -cp <customRule.jar> <rule_package_base_dir> <xml_base_dir>```








---
title: Managing Custom Rules
lang: en
---

## Adding Custom Rule(s)
Use the ```scanner:rule:add``` [command](./en/scanner-commands/add/) to add a custom rule to the Salesforce CLI Scanner plug-in. Use the ```-p|--path``` parameter to specify the JAR file that contains your custom rule definitions. You can specify multiple JAR files to add multiple custom rules for a single language. You can also use the parameter to specify a directory that contains multiple JAR files. 

To add one or more custom rules to multiple languages, use a separate ```scanner:rule:add``` for each language. 

For example, to add a single JAR file for the Apex language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/File.jar"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/File.jar"
```

To add a directory path that contains multiple JAR files for the same language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/files"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/files"
```
To add multiple paths to JARs that are in different locations for the same language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
```

The command output indicates which JAR files were found and added to the plug-in.


## Running Custom Rule(s)

After you’ve added your rules to the Salesforce CLI Scanner plug-in with ```scanner:rule:add```, run the ```scanner:rule:list``` [command](./en/scanner-commands/list/) to make sure they show up. Your custom rules are displayed under the Category names you defined in your XML Rule definition file(s).

You can now run your custom rules just like you [run](./en/scanner-commands/run/) the built-in rules. 


## Removing Custom Rule(s)

Remove custom rules from the catalog with the ```scanner:rule:remove``` [command](./en/scanner-commands/remove/). The rules defined in the JAR file you specify with the ```-p|--path``` parameter are removed from the catalog. 

Use the ``` --force ``` parameter to bypass confirmation of the removal.

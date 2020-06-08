---
title: Managing Custom Rules
lang: en
---

## Adding Custom Rule(s)
Use the ```scanner:rule:add``` [command](./en/scanner-commands/add/) to add your Custom Rule(s). At this point, your JAR file should be ready. In your unique scenario, you may even have a directory that contains multiple JAR files with many Custom Rules.

Note that the add command can add multiple paths at once, but only takes information for a single language at a time. You can add multiple paths in a single add command run or through multiple calls. However, if you wish to add custom rules for multiple languages, please run the add command separately for each language for which you wish to add Custom Rules.

To add a single JAR file for a language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/File.jar"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/File.jar"
```

To add a path that contains multiple JAR files for the same language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/files"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/files"
```
To add multiple paths to JARs residing in different locations for the same language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
```

The feedback to each of these commands should show you which JAR files were found and added to the plugin.


## Running Custom Rule(s)

Once you’ve added your rules to the Sfdx Scanner plugin as mentioned in the previous section, run the ```scanner:rule:list``` [command](./en/scanner-commands/list/) to make sure your custom rules show up under the Category names you had given in your XML Rule definition file(s).

Now you can run your Custom Rules just like how you would [run](./en/scanner-commands/run/) the built-in rules. 


## Removing Custom Rule(s)

The custom rules can be removed from the catalog by executing the ```scanner:rule:remove``` [command](./en/scanner-commands/remove/). You can use the path of the jar file as a parameter and the rules in that jar will be removed from the catalog. 

Note: You can use to ``` --force ``` option to force to bypass that confirmation.

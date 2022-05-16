---
title: Managing Custom Rules
lang: en
---

## Different approaches for PMD and Eslint

Since PMD and Eslint are different engines, they require dissimilar handling of custom rules. The [author section](./en/v3.x/custom-rules/author/) explains the different ways to construct PMD and Eslint rules.

To manage rules, an important distinction is that while PMD custom rules can be integrated and executed as part of the default rule set, Eslint's custom rules cannot be mixed with the Code Analyzer's default rules. Read on for more specific instructions.

---

## PMD Custom Rules
### Adding Rule(s)

Use the ```scanner:rule:add``` [command](./en/v3.x/scanner-commands/add/) to add a custom rule to the Salesforce Code Analyzer plug-in. Use the ```-p|--path``` parameter to specify the XML file containing your XPath-based rules, or the JAR containing your Java-based rules. You can specify multiple files to add multiple custom rules for a single language. You can also use the parameter to specify a directory that contains multiple JAR/XML files.

To add one or more custom rules to multiple languages, use a separate ```scanner:rule:add``` for each language. 

For example, to add a single JAR file for the Apex language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/File.jar"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/File.jar"
```

To add a directory path that contains multiple JAR/XML files for the same language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/files"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/files"
```
To add multiple paths to files that are in different locations for the same language:
```bash
sfdx scanner:rule:add --language apex --path "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
(OR)
sfdx scanner:rule:add -l apex -p "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
```

The command output indicates which JAR files were found and added to the plug-in.


### Running Rule(s)

After you’ve added your rules to the Salesforce Code Analyzer plug-in with ```scanner:rule:add```, run the ```scanner:rule:list``` [command](./en/v3.x/scanner-commands/list/) to make sure they show up. Your custom rules are displayed under the Category names you defined in your XML Rule definition file(s).

You can now run your custom rules just like you [run](./en/v3.x/scanner-commands/run/) the built-in rules. 


### Removing Rule(s)

Remove custom rules from the catalog with the ```scanner:rule:remove``` [command](./en/v3.x/scanner-commands/remove/). The rules defined in the JAR/XML file you specify with the ```-p|--path``` parameter are removed from the catalog. 

Use the ``` --force ``` parameter to bypass confirmation of the removal.

---

## Eslint Custom Rules

Custom Rules on Eslint are handled through the Code Analyzer's [Custom Config capabilities](./en/v3.x/custom-config/eslint/).

### Adding Rule(s)

You can add your custom rule(s) to a custom `.eslintrc.json` file. This requires:

1. Adding your plugin:
```bash
//.eslintrc.json
...
		"plugins": [
                "my-custom",
				... // Other required plugins
        ],
...
```

2. Adding your rule(s):
```bash
//.eslintrc.json
...
		"rules": {
                "my-custom/my-new-rule": 1,
				... //Other rules
        },
...
```

3. Making sure your directory has all the required NPM dependencies including Eslint, your custom rule plugin and your parser.

### Running rules

Execute the `scanner:run` command in the directory with all the necessary NPM dependencies like this:

```bash
$ sfdx scanner:run --eslintconfig "/path/to/my/.eslintrc.json" --target "/path/to/target"
```


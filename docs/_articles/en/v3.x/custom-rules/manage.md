---
title: Managing Custom Rules
lang: en
redirect_from: /en/custom-rules/manage
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/custom-config.html
---

## Different approaches for PMD and ESLint

Because PMD and ESLint are different engines, their custom rules are handled differently. PMD custom rules can be integrated and executed as part of the default rule set, but ESLint’s custom rules can’t be mixed with Salesforce Code Analyzer (Code Analyzer) default rules. 

---

## PMD Custom Rules
### Add Rules

Use the ```scanner rule add``` [command](./en/v3.x/scanner-commands/add/) to add a custom rule to Salesforce Code Analyzer (Code Analyzer) catalog. Rules added in this way can then be invoked with ```scanner run``` the same way that PMD default rules are invoked. Use the ```-p|--path``` parameter to specify the XML file containing your XPath-based rules, or the JAR containing your Java-based rules. You can specify multiple files to add multiple custom rules for a single language. You can also use the ```-p|--path``` parameter to specify a directory that contains multiple JAR or XML files.

To add one or more custom rules to multiple languages, use a separate ```scanner rule add``` for each language.

For example, to add a single JAR file for the Apex language:

```bash
sf scanner rule add --language apex --path "path/to/your/File.jar"
(OR)
sf scanner rule add -l apex -p "path/to/your/File.jar"
```

To add a directory path that contains multiple JAR/XML files for the same language:

```bash
sf scanner rule add --language apex --path "path/to/your/files"
(OR)
sf scanner rule add -l apex -p "path/to/your/files"
```

To add multiple paths to files that are in different locations for the same language:

```bash
sf scanner rule add --language apex --path "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
(OR)
sf scanner rule add -l apex -p "path/to/your/files,/another/path/Custom.jar,/yet/another/jar/lib"
```

The command output indicates which JAR files were found and added to Code Analyzer.

### Run Rules

After you add your rules to Code Analyzer with ```scanner rule add```, run ```scanner rule list``` [command](./en/v3.x/scanner-commands/list/) to review the complete catalog of existing PMD rules and your latest additions. Your custom rules are displayed under the category names that you defined in your XML rule definition files, and they’re ready to use.

Run your custom rules using `scanner run` the same way you run PMD's built-in rules: `scanner run --category MyCustomCategory`. You don't need the `--pmdconfig` flag to run your custom rules.

### Remove Rules

Remove custom rules from the catalog with the ```scanner rule remove``` [command](./en/v3.x/scanner-commands/remove/). The rules defined in the JAR/XML file that you specify with the ```-p|--path``` parameter are removed from the catalog.

Use the ``` --force ``` parameter to bypass confirmation of the removal.

---

## ESLint Custom Rules

Custom rules on ESLint are handled through Code Analyzer’s [ESLint Custom Configuration](./en/v3.x/custom-config/eslint/).

### Add Rules

To add your custom rules to a custom `.eslintrc.json` file, follow these steps.

1. Add your plug-in:
```bash
//.eslintrc.json
...
		"plugins": [
                "my-custom",
				... // Other required plugins
        ],
...
```

2. Add your rule:
```bash
//.eslintrc.json
...
		"rules": {
                "my-custom/my-new-rule": 1,
				... //Other rules
        },
...
```

3. Make sure that your directory has all the required npm dependencies, including ESLint, your custom rule plug-in, and any other applicable dependencies, such as parsers or processors.

### Run rules

Execute the `scanner run` command in the directory with all the necessary npm dependencies. For example:

```bash
$ sf scanner run --eslintconfig "/path/to/my/.eslintrc.json" --target "/path/to/target"
```

---
title: ESLint Custom Configuration
lang: en
redirect_from: /en/custom-config/eslint
---

## ESLint Custom Configuration

To bring in the power of any ESLint capabilities that aren’t supported by default in Salesforce Code Analyzer (Code Analyzer), use your own customized ```.eslintrc.json```. You have the flexibility to use different parsers or plug-ins, and you can define your own custom ESLint ruleset. 

To invoke a customized ESLint file, pass in your config file filepath as a value to ```--eslintconfig``` flag in ```scanner:run command```. 

For example:

```$ sfdx scanner:run --target "/path/to/your/target" --eslintconfig "/path/to/.eslintrc.json"```

## Your ESLint Custom Configuration Responsibilities

Before setting up your custom configuration with ESLint through Code Analyzer, make sure that you understand your responsibilities.

* All npm dependencies, including ESLint, must be installed in the directory where you run the ```scanner:run``` command.
* Your custom configuration cascades into your other project configurations as though you ran ESLint directly with the ```-c | –config``` flag. Your custom configuration must perform all necessary overrides.
* Your ```.eslintrc.json``` file must be in ```.eslintrc``` format. However, there are no restrictions on the filename.
* The ```.eslintignore``` flag isn’t evaluated. Instead, use the ```--target``` flag to define the targets that you want Code Analyzer to scan. ```--target``` accepts a comma-separated list of any combination of files, directories, positive patterns, and negations.
* If your configuration executes Typescript, make sure that your ```tsconfig``` file is added to the configuration under ```parserOptions.project```. The ```--tsconfig``` flag can’t be used with ```--eslintconfig``` flag. 

	Example:

	```
	//.eslintrc.json
	 "parseOptions": {
	     "project": "/path/to/tsconfig.json"
	 },
	```

* Any ```package.json``` files with embedded ESLint configuration aren’t supported.

## ESlint Restrictions

ESLint usage in Code Analyzer has these restrictions.

* When ```--eslintconfig``` is provided, no other default ESLint engines run in Code Analyzer.
* Rule filters such as ```--category``` and ```--ruleset``` aren’t evaluated on rules selected from an ESLint custom configuration.
* Engine filters can be used with ```--eslintconfig``` flag. However, only the rules included in the config execute. PMD runs with the default set of rules, but ```eslint-lwc``` isn’t invoked. Instead, ESLint is invoked based on the configuration in ```/path/to/.eslintrc.json```.

	Example:

	```
	$ sfdx scanner:run --engine "pmd,eslint-lwc" --eslintconfig "/path/to/.eslintrc.json" --target "/target/path"
	```

## See Also

- [ESLint: Configuring ESLint](https://eslint.org/docs/latest/user-guide/configuring/)
- [ESLint: Cascading and Hierarchy](https://eslint.org/docs/latest/user-guide/configuring/configuration-files#cascading-and-hierarchy)

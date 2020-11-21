---
title: ESLint Custom Configuration
lang: en
---

## Introduction

If you are a Scanner plugin user who is already experienced with Eslint, you may feel restricted by the default Eslint flavors that Scanner plugin offer. Now you can use your own customized `.eslintrc.json` file with the Scanner plugin to bring in the power of any eslint capabilities that are not supported by default. This gives your the flexibility to use different parsers, plugins and defining your custom set of rules. You can find more information about Eslint's configuration [here](https://eslint.org/docs/user-guide/configuring).

To invoke this feature, pass in the file path to your config file as a value to `--eslintconfig` flag in `scanner:run` command.

```bash
$ sfdx scanner:run —target "/path/to/your/target" —eslintconfig "/path/to/.eslintrc.json"
```

However, while giving you the power, the Scanner also offloads some responsibilities to you. Please read the following sections fully before attempting to use custom configuration with Eslint through the Scanner.

## Users' Responsibilities

1. **IMPORTANT:** Make sure that you have all the NPM dependencies installed (including Eslint) in the directory where you run the `scanner:run` command.

2. Ensure correctness of `.eslintrc.json`. Only JSON format of `.eslintrc` is supported today. However, there are no restrictions on the filename.

3. `.eslintignore` is not evaluated today. Please use your target patterns in the `--target` flag that is passed in with `scanner:run` command. As a reminder, `--target` can take a comma separated list of any combination of files, directories, positive patterns and negations.

4. If you have written your configuration to execute Typescript, make sure your tsconfig file is added to the configuration under `parserOptions.project`. `--tsconfig` flag cannot be used with `--eslintconfig` flag.

5. `package.json` with embedded eslint configuration is not supported.
```bash
//.eslintrc.json
...
	"parseOptions": {
		"project": "/path/to/tsconfig.json"
	},
...
```


## Restrictions with Scanner Plugin

1. When `--eslintconfig` is provided, none of the other default Eslint engines with the Scanner plugin will be run.

2. Rule filters such as `--category` and `--ruleset` will not be evaluated on rules selected from custom configuration.


3. Engine filters can be used with `--eslintconfig` flag. However, only the rules included in the config will be executed. Consider this example:
```bash
$ sfdx scanner:run --engine "pmd,eslint-lwc" --eslintconfig "/path/to/.eslintrc.json" --target "/target/path"
```
While PMD will be run with the default set of rules, eslint-lwc will not be invoked. Instead, eslint will be invoked based on the configuration in `/path/to/.eslintrc.json` and all the responsibilities listed above should be addressed by the user.

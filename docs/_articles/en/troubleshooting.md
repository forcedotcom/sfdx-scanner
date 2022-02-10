---
title: Troubleshooting Common Issues
lang: en
---

Here are some troubleshooting tips to fix common issues when you use the Salesforce CLI Scanner plug-in.

## Using `scanner:run` command

### I sometimes see more rules violations when I specify the categories with `scanner:run` command than when I execute it for the same target without filters. What is this inconsistency?

This is working as designed. Some rules are default-enabled by eslint while some other rules are not. Executing `scanner:run` without filters causes only default-enabled rules to be invoked, and specifying a category filter with `scanner:run` includes all rules under the selected categories to be invoked irrespective of their default-enabled setting. As of today, we do not provide a way to modify default-enable settings for rules.

### The `scanner:run` command results in the error `JavaScript heap out of memory`.

The scanner's node process runs with a default limit of 2GB of memory. This limit can be changed by configuring the `max-old-space-size` node option. The required memory will depend on the files included in the `--target` parameter. The following example increases the memory value to 4GB for a single invocation of the scanner.

```bash
$ NODE_OPTIONS="--max-old-space-size=4096" sfdx scanner:run --target "./**/*.ts"
```

### The `scanner:run` command throws a ParseException when executing against my Visualforce files as target.

Please check if the affected Visualforce pages/components render correctly.

If it does, check if it has an HTML tag that has an attribute with a dot? PMD has an [open issue](https://github.com/pmd/pmd/issues/2765) and we are working with them to fix it.
If this is a new issue, please let us know.

---

## `scanner:run` command with eslint-typescript

### The `scanner:run` command output contains the message `'<file_name>' does not reside in a location that is included by your tsconfig.json 'include' attribute`.

The ESLint engine requires that any typescript files that are scanned must be included by the tsconfig. [More Information](https://github.com/typescript-eslint/typescript-eslint/releases/tag/v2.0.0)

Update your tsconfig's `include` attribute to include `<file_name>`


### The `scanner:run` command fails with the error `Unable to find 'tsconfig.json' in current directory X`, even though I'm not scanning any TypeScript files.

The most likely cause is that you're scanning TypeScript files without realizing it.

If you're using a dependency management framework like Yarn, Bower, or NPM, you should make sure that those folders are
being ignored by the scanner, since dependent modules may have TypeScript files that you don't know about.

Make sure that the entries for eslint and eslint-typescript in `${HOME}/.sfdx-scanner/Config.json` both exclude the folder
used by your framework. For example, to exclude Yarn/NPM dependencies, add `!**/node_modules/**` to the `targetPatterns`
property, and add `!**/bower_components/**` to exclude Bower dependencies.

---

## Using Custom Rules

### My custom rule Java file doesn’t compile.
* Make sure that you reference only PMD features and classes that are available in version {{ site.data.versions.pmd }}.
* Check that you're using correct syntax. 
* Check that the compilation CLASSPATH contains the correct version of the PMD binary.


### I successfully created my rule XML and added it using the `scanner:rule:add` command, but `scanner:rule:list` doesn't display my custom rules.
- Double-check that the rules in your XML are exclusively XPath-based. If any of the rules use custom Java, then a JAR is required.
- Ensure that the XML's path includes `category` as a directory.

### I successfully created my JAR file and added it using the `scanner:rule:add` command, but `scanner:rule:list` doesn't display my custom rules.

* Check that the XML Rule Definition file is included in the JAR. Run `jar tf /your/jar/file.JAR` to list the files in your JAR. 
* Make sure your XML file is in a PATH that includes `category` as a directory. 
* Check that your class files are included in the JAR. 
* Confirm that the PATH to the class files reflects the package structure in the Java file.


### The `scanner:rule:list` command displays my new custom rules in the catalog, but when I run them I get an error about the Java version.

One possible reason is that the Java version you used to build your code is different from the version the Salesforce CLI Scanner plug-in uses to invoke PMD. Make sure you compile your Java code with the same Java version and path that’s listed in the `java-home` key in `<HOME_DIR>/.sfdx-scanner/Config.json`.


### The `scanner:rule:list` command displays my new custom rules in the catalog, but when I run a rule, I get a `ClassNotFoundException`.

One possible reason is that you referenced a class in your custom rule Java code from the PMD library that's not available in version {{ site.data.versions.pmd }}. Make sure that you reference only PMD features and classes that are available in version {{ site.data.versions.pmd }}.

---

## Using Custom config

### When using `--eslintconfig` flag, I get a `Cannot find module <some_module>` error

In the directory where you execute the `scanner:run` command, install the required eslint dependencies using `npm install <some_module>`

---

## Common to multiple commands

### Commands display `Javascript is not currently supported by the PMD engine`.

Version 6.x of PMD has a [Known Issue](https://github.com/pmd/pmd/issues/2081) that causes a Java OutOfMemoryError when scanning some Javascript files. Scanning Javascript with PMD has been removed from the current version of the Salesforce CLI Scanner plug-in. We plan to restore this feature in a future version.

Make the following changes to the PMD engine node in your `${HOME}/.sfdx-scanner/Config.json` file to resolve this error.

1. Remove the `**/*.js` element from the PMD engine's `targetpatterns` array
2. Remove the `javascript` element from PMD engine's `supportedLanguages` array

The annotated JSON below shows you what you should remove
```json
{
    "engines": [
        {
            "name": "pmd",
            "targetPatterns": [
                "**/*.cls",
                "**/*.trigger",
                "**/*.java",
                "**/*.js",          // REMOVE THIS LINE
                "**/*.page",
                "**/*.component",
                "**/*.xml",
                "!**/node_modules/**",
                "!**/*-meta.xml"
            ],
            "supportedLanguages": [
                "apex",             // REMOVE THIS TRAILING COMMA
                "javascript"        // REMOVE THIS
            ]
        },
... Rest of file removed for clarity ...
```

---

## Supported languages for PMD.

We have removed the PMD support for all languages except the following:
- Apex
- Java
- Visualforce
- XML


If this would present you hardship, please create an Issue on our [Github repo](https://github.com/forcedotcom/sfdx-scanner).
Otherwise, remove any languages besides those listed above from the PMD engine's `supportedLanguages` array in your
`${HOME}/.sfdx-scanner/Config.json` file.

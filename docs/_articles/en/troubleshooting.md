---
title: Troubleshooting Common Issues
lang: en
---

Here are some troubleshooting tips to fix common issues when you use the Salesforce CLI Scanner plug-in.


### My custom rule Java file doesn’t compile.
* Make sure that you reference only PMD features and classes that are available in version 6.22.0.
* Check that you're using correct syntax. 
* Check that the compilation CLASSPATH contains the correct version of the PMD binary.


### I successfully created the JAR file and added it using the `scanner:rule:add` command, but `scanner:rule:list` doesn't display my custom rules.

* Check that the XML Rule Definition file is included in the JAR. Run `jar tf /your/jar/file.JAR` to list the files in your JAR. 
* Make sure your XML file is in a PATH that includes `category` as a directory. 
* Check that your class files are included in the JAR. 
* Confirm that the PATH to the class files reflects the package structure in the Java file.


### The `scanner:rule:list` command displays my new custom rules in the catalog, but when I run them I get an error about the Java version.

One possible reason is that the Java version you used to build your code is different from the version the Salesforce CLI Scanner plug-in uses to invoke PMD. Make sure you compile your Java code with the same Java version and path that’s listed in the `java-home` key in `<HOME_DIR>/.sfdx-scanner/Config.json`.


### The `scanner:rule:list` command displays my new custom rules in the catalog, but when I run a rule, I get a `ClassNotFoundException`.

One possible reason is that you referenced a class in your custom rule Java code from the PMD library that's not available in version 6.22.0. Make sure that you reference only PMD features and classes that are available in version 6.22.0.


### I sometimes see more rules violations when I specify the categories with `scanner:run` command than when I execute it for the same target without filters. What is this inconsistency?

This is working as designed. Some rules are default-enabled by eslint while some other rules are not. Executing `scanner:run` without filters causes only default-enabled rules to be invoked, and specifying a category filter with `scanner:run` includes all rules under the selected categories to be invoked irrespective of their default-enabled setting. As of today, we do not provide a way to modify default-enable settings for rules.


### Commands display `Javascript is not currently supported by the PMD engine`.

Version 6.22.0 of PMD has a [Known Issue](https://github.com/pmd/pmd/issues/2081) that causes a Java OutOfMemoryError when scanning some Javascript files. Scanning Javascript with PMD has been removed from the current version of the Salesforce CLI Scanner plug-in. We plan to restore this feature in a future version.

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

### The `scanner:run` command output contains the message `'<file_name>' does not reside in a location that is included by your tsconfig.json 'include' attribute`.

The ESLint engine requires that any typescript files that are scanned must be included by the tsconfig. [More Information](https://github.com/typescript-eslint/typescript-eslint/releases/tag/v2.0.0)

Update your tsconfig's `include` attribute to include `<file_name>`

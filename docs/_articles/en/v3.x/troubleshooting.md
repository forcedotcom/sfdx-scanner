---
title: Troubleshooting Common Issues
lang: en
redirect_from: /en/troubleshooting
---

Follow these tips to fix common Salesforce Code Analyzer (Code Analyzer) issues.

## Issues with `scanner run`

### I sometimes see more rules violations when I specify categories with `scanner run` command than when I execute it for the same target without filters. What is this inconsistency?

It’s actually working as designed. Some rules are default-enabled by ESLint, while some other rules aren’t. Executing `scanner run` without filters causes only default-enabled rules to be invoked, and specifying a category filter with `scanner run` includes all rules under the selected categories to be invoked irrespective of their default-enabled setting. As of today, we don’t provide a way to modify default-enable settings for rules.

### The scanner run command results in the `JavaScript heap out of memory` error.

Code Analyzer’s node process runs with a default limit of 2 GB of memory. This limit can be changed by configuring the `max-old-space-size` node option. The required memory depends on the files included in the `--target` parameter. This example increases the memory value to 4 GB for a single invocation of the Code Analyzer.

```bash
$ NODE_OPTIONS="--max-old-space-size=4096" sf scanner run --target "./**/*.ts"
```

### The `scanner run` command throws a ParseException when executing against my Visualforce files as target.

Check to see if the affected Visualforce pages or components render correctly.

If your Visualforce pages or components include HTML tags that include PMD attributes, review [PMD-Visualforce open issues](https://github.com/pmd/pmd/issues/2765). PMD can provide the fix for you.

### The `scanner run` command throws a warning about duplicate violations between ESLint and ESLint-LWC.

If you invoke the `eslint` and `eslint-lwc` engines in the same run, and execute them on the same files, Code Analyzer returns duplicate violations.

You see this warning:

`At least one file was processed by both ESLint and ESLint-LWC simultaneously, which could result in duplicate violations. Customize targetPatterns properties for eslint and eslint-lwc engines in ~/.sfdx-scanner/{{ site.data.versions-v3.configfile }} to remove overlap on the following file(s):`

To prevent duplication, use target patterns glob syntax and edit your config file.

* For `eslint-lwc` `targetPatterns`, replace `"**/*.js"` with the pattern of your LWC files
* For `eslint` `targetPatterns`, add the converse of your `estlint-lwc` files

For example, your LWC files are stored in a directory called `lwc`. Include this information in your `{{ site.data.versions-v3.configfile }}` file.

```
{
    "name": "eslint",
    "targetPatterns": [
        "**/*.js",
        "!**/lwc/**/*.js",
        "!**/node_modules/**",
        "!**/bower_components/**"
    ],
    "disabled": false
},
{
    "name": "eslint-lwc",
    "targetPatterns": [
        "**/lwc/**/*.js",
        "!**/node_modules/**"
    ],
    "disabled": true
},
```

---

### I’m seeing errors on my scans. Why isn’t Code Analyzer scanning all of my paths?

Your path names likely contain characters that cause issues. We recommend that you don’t use these characters in path names.

| Character | Workaround                                                                                          |
|:---|:----------------------------------------------------------------------------------------------------|
| Space 		  | 	Use underscores instead of spaces<br/>or enclose a path name that includes spaces fully in quotes. |
| {}        | None                                                                                                |
| []        | None                                                                                                |
| ()        | None                                                                                                |

## Issues with `scanner run` and `eslint-typescript`

### The `scanner run` command output contains the message `'<file_name>' doesn’t reside in a location that is included by your tsconfig.json 'include' attribute`.

The ESLint engine requires that any scanned TypeScript files must be included by the `tsconfig`. Read more in the `typescript-eslint` [GitHub repo](https://github.com/typescript-eslint/typescript-eslint/releases/tag/v2.0.0).

Update your `tsconfig`’s `include` attribute to include `<file_name>`.


### The `scanner run` command fails with the error `Unable to find 'tsconfig.json' in current directory X`, even though I'm not scanning any TypeScript files.

The most likely cause is that you’re scanning TypeScript files without realizing it.

If you’re using a dependency management framework like Yarn, Bower, or npm, make sure that SFCA ignores those folders because dependent modules can have TypeScript files that you don’t know about.

Make sure that the entries for ESLint and `eslint-typescript` in `${HOME}/.sfdx-scanner/Config.json` exclude the folder used by your framework. For example, to exclude Yarn and npm dependencies, add `!**/node_modules/**` to the `targetPatterns` property, and add `!**/bower_components/**` to exclude Bower dependencies.

---

## Issues with Custom Rules

### My custom rule Java file doesn’t compile.

* Make sure that you reference only PMD features and classes that are available in version 6.43.0.
* Check that you’re using correct syntax.
* Check that the compilation CLASSPATH contains the correct version of the PMD binary.


### I successfully created my rule XML and added it using the `scanner rule add` command, but `scanner rule list` doesn’t display my custom rules.

* Double-check that the rules in your XML are exclusively XPath-based. If any of the rules use custom Java, then a JAR is required.
* Ensure that the XML’s path includes `category` as a directory.

### I successfully created my JAR file and added it using the `scanner rule add` command, but `scanner rule list` doesn’t display my custom rules.

* Check that the XML rule definition file is included in the JAR. Run `jar tf /your/jar/file.JAR` to list the files in your JAR.
* Make sure that your XML file is in a PATH that includes `category` as a directory.
* Check that your class files are included in the JAR.
* Confirm that the PATH to the class files reflects the package structure in the Java file.

### The `scanner rule list` command displays my new custom rules in the catalog, but when I run them I get an error about the Java version.

One possible reason is that the Java version you used to build your code is different from the version Code Analyzer uses to invoke PMD. Make sure that you compile your Java code with the same Java version and path that’s listed in the `java-home` key in `<HOME_DIR>/.sfdx-scanner/Config.json`.

### The `scanner rule list` command displays my new custom rules in the catalog, but when I run a rule, I get a `ClassNotFoundException`.

One possible reason is that you referenced a class in your custom rule Java code from the PMD library that’s not available in version {{ site.data.versions-v3.pmd }}. Make sure that you reference only PMD features and classes that are available in version  {{ site.data.versions-v3.pmd }}.

---

## Issues with Custom Config

### When using `--eslintconfig` flag, I get a Can’t find module `<some_module>` error

In the directory where you execute the `scanner run` command, install the required ESLint dependencies using npm install `<some_module>`.

---

## Issues Common to Multiple Commands

### Commands display `Javascript isn’t currently supported by the PMD engine`.

Version 6.x of PMD has a [Known Issue](https://github.com/pmd/pmd/issues/2081) that causes a Java `OutOfMemoryError` when scanning some Javascript files. Scanning Javascript with PMD was removed from the current version of SFCA. We plan to restore this feature in a future version.
To resolve this error, make these changes to the PMD engine node in your `${HOME}/.sfdx-scanner/Config.json` file.

Remove the `**/*.js` element from PMD engine’s `targetpatterns` array.

Remove the `javascript` element from PMD engine’s `supportedLanguages` array.

Example:

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

## What languages are supported with PMD?

We removed the PMD support for all languages except:

* Apex
* Java
* Visualforce
* XML

Remove any languages besides the languages in PMD engine’s `supportedLanguages` array in your `${HOME}/.sfdx-scanner/Config.json` file. Create an issue on our [Github repo](https://github.com/forcedotcom/sfdx-scanner) to request that we add more languages.

---

## Issues using Salesforce Graph Engine

### Troubleshooting InternalExecution Errors

Follow these guidelines to resolve InternalExecution errors:

1. Read the error message that you received.
2. Does the error message mention a specific vertex with a DefiningType and BeginLine? These values correspond to an Apex class and line number. Check what’s happening in that line of code, and re-read the error message. With the additional context, is there any debugging that you can do or refactoring that you can try to work around the issue?
3. Does the error message mention the name of a specific method, variable, or property in your code? Search your codebase for references to it. Then re-read the error message.
4. Delete your `~/.sfdx-scanner/sfge`.log file, and then rerun the command. This action produces a clean log file containing just the logs from the most recent execution. Read those logs. Look for:
	- Exceptions and stack traces. The exception often provides more information such as DefiningTypes. If you log an issue, it’s important to include the stack trace.
	- ```TODO: PathScopeVisitor.getApexValue() can currently only support chains of length 2 or lower. keySequence=[X, Y, Z]``` This message indicates that your code has a reference chain of length 3 or greater. Graph Engine is unable to process these references and treats them as indeterminate values. It’s possible that fixing these references can fix your issue. <br> For example, instead of this reference:<br> `String s = someObj.innerObj.someString;`<br> Try: <br> `InnerObj o = someObj.innerObj;`<br> `String s = o.someString;`
5. Attempt to reproduce the issue in simpler code. The process of figuring out how to reproduce it gives you valuable insight into potential workarounds  or fixes.
6. If you’re still blocked, [log an issue](https://github.com/forcedotcom/sfdx-scanner/issues/new/choose), and include:
- a clean log 
- exceptions
- stack traces
- specific code that can be used to reproduce the issue
- clear details about how to write the code. 
- if the error message indicated a specific vertex, include the line of code the vertex refers to.

### My `scanner run dfa` execution ran for a long time and ended with a timeout.

Depending on the complexity of the source code, Graph Engine can take a long time to complete. 

Use one of these methods to increase the timeout:

* increase the `SFCA_RULE_THREAD_TIMEOUT` environment variable
* invoke the command with `rulethreadtimeout` parameter

Both methods take int values of the number of milliseconds to wait. The default timeout is 15 minutes.

To complete the execution within the timeout period, you can also reduce the number of files sent as `--target`, which reduces the number of entry points scanned per run.


### My `scanner run dfa` execution ran into this error: `java.lang.LimitReachedError: Java heap space`.

This `LimitReachedError` is a current, known Graph Engine issue. Refer to our [recommendations](./en/v3.x/salesforce-graph-engine/working-with-sfge/#understand-limitreached-errors) to reduce the probability of experiencing the heap space error.

---


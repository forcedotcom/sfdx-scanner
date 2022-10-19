---
title: 'Working with SFGE'
lang: en
---

## Background

Each code path contains these three elements.

* Source–An entry point for an external interaction.
* Sink–Code that modifies data.
* Sanitizer–The check that happens between source and sink to ensure that the user who is performing this action on data has the necessary access to the object and fields.

A code path must have a sanitizer in between the source and the sink. When the sanitizer is missing, Graph Engine returns a violation. To avoid violations, ensure that each path created from any source to sink is sanitized.

A source can lead to multiple sinks. Also, a sink can be reached through multiple sources. In fact, we can have multiple paths between the same source and sink. 

For more information, read [Try it yourself](./en/v3.x/salesforce-graph-engine/try-it-yourself/).

## Invoke Graph Engine through Code Analyzer
Invoke data-flow-based rules through SFCA by running [scanner:run:dfa](./en/v3.x/scanner-commands/dfa/).

## Interpret Your Results

An individual row in the Graph Engine results file represents a violation. Each violation contains sink and source information, plus the actual violation message. 

Here’s a breakdown of the output you see by column name.

* Severity. The severity of the violation. By default, all security violations are marked as severity 1.
* Sink File, Sink Line, Sink Column. The location where the data interaction happens in your source code.
* Source File, Source Line, Source Column. The location where the path begins.
* Source Type, Source Method. Additional information to help identify the path entry.
* Rule. The rule that was run which led to the violation.
* Description. The violation message. For more info on Graph Engine violation messages, read our [FAQ](./en/v3.x/faq/#q-what-do-the-violation-messages-mean).

## Add Engine Directives

Like all security tools, Graph Engine can create false negatives or false positives.

For example, the engine can fail to create a violation where the code is insecure, which is a false negative. Or it can create a violation even though the code is secure, a false positive.

If you determine that Graph Engine created a false positive, add engine directives to your code so that Graph Engine doesn’t throw that violation anymore.

Graph Engine understands three levels of engine directives.

### Disable Next Line

`/* sfge-disable-next-line <rule_name> */`

To disable just the sink from Graph Engine’s analysis, run `disable-next-line`.

Example:

```
/* sfge-disable-next-line ApexFlsViolationRule */
insert a;
```

### Disable Method

`/* sfge-disable-stack <rule_name> */`

To disable all the sink operations in paths passing through this method, use `disable-stack`. As with the other engine directives, make sure that you add it in the line immediately before the method declaration. 

Example:

```
@AuraEnabled
/* sfge-disable-stack ApexFlsViolationRule */
public static boolean someMethodName() {
```

### Disable Class

`/* sfge-disable <rule_name> */`

To disable all the sink operations that occur in the class, run `disable`. As with other engine directives, add it in the line immediately before class declaration. 

Example:

```
/* sfge-disable ApexFlsViolationRule */
public class MyClass {
```

## Understand OutOfMemory: Java heap space Error

A number of factors can degrade Graph Engine’s efficiency and increase the probability of encountering an OutOfMemory error. 

* With every conditional or method invocation in your code, the number of paths Graph Engine creates increases exponentially. 
* Your OS type, Java setup, and other processes running on your machine can influence the heap space assigned by Java Virtual Machine (JVM).

If Graph Engine’s execution is interrupted, it returns results from the portion of source code it has analyzed so far.

To avoid an Out of Memory error:

* Note your JVM's default max heap size, the `-Xmx` value. Then increase the max heap size assigned to Graph Engine's execution by providing an updated -Xmx value to either the sfgejvmargs parameter with scanner:run:dfa command or to the SFGE_JVM_ARGS environment variable.
	Next, execute Graph Engine with a larger heap space than the default settings.

	For example, to allocate 2 G heap space:

	```
	sfdx scanner:run:dfa --sfgejvmargs "-Xmx2g" <rest of your parameters>
	```
	or
	```
	export SFGE_JVM_ARGS="-Xmx2g"
	sfdx scanner:run:dfa <rest of your parameters>
	```
	Because the heap space value depends on the complexity of the target codebase, there's no magic number. A very large heap space can degrade Graph Engine’s performance, so increase the heap space allocation in increments of 1 G. Experiment to see what works for your project.

* Target a smaller set of files for analysis. Provide a subset of Apex files using the `--target` flag on the `scanner:run:dfa` command while keeping the same `--projectdir` value. This approach reduces the number of paths and reduces the likelihood of `OutOfMemory` errors.

* Simplify your source code to avoid large IF/ELSE-IF/ELSE conditional trees, which helps bring down the number of paths created.

## Limitations of Salesforce Graph Engine

Graph Engine has these limitations.

* Violations thrown as `Internal error. Work in progress. Please ignore`, indicate that the entry point’s analysis didn’t complete successfully. We’re working on fixing this issue. In the meantime, you must verify the validity of this error manually.
* Graph Engine handles unique class names. If the source code has two distinctly different files that have classes with duplicate names, Graph Engine fails with an error message: `<example_class> is defined in multiple files`. In cases like these, provide --projectdir subpath to the source directory that has only one of the file names, and rerun Graph Engine with the subpath to the second file name.
* Graph Engine doesn’t handle anonymous Apex script. Provide the class directory path as the `--projectdir` that doesn’t include any anonymous Apex script.
* Graph Engine doesn’t handle namespace placeholders. Leave the namespace placeholder blank.

### Reporting Errors

We appreciate your help in identifying and fixing issues with Salesforce Graph Engine. To report bugs, create a new issue. 

To verify your bug, include publicly shareable sample code. 

* Create sample code without actual variable names that still mimics the original  issue as closely as possible. 
* Ensure that your sample code runs into the same error as your original code.

If you have thoughts on usability, how the tool works, or new feature requests, we welcome your [feedback](https://www.research.net/r/SalesforceCA). 

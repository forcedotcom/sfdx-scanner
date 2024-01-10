---
title: 'Working with Salesforce Graph Engine'
lang: en
---

## Background

Each code path contains these three elements.

* Source–An entry point for an external interaction.
* Sink–Code that modifies data.
* Sanitizer–The check that happens between source and sink to ensure that the user who performs this action on the data has the necessary access to the object and fields.

A code path must have a sanitizer in between the source and the sink. When the sanitizer is missing, Graph Engine returns a violation. To avoid violations, ensure that each path created from any source to sink is sanitized.

A source can lead to multiple sinks. Also, a sink can be reached through multiple sources. In fact, we can have multiple paths between the same source and sink. 

For more information, read [Try it Yourself](./en/v3.x/salesforce-graph-engine/try-it-yourself/).

## Invoke Graph Engine through Code Analyzer
Invoke data-flow-based rules through SFCA by running [scanner run dfa](./en/v3.x/scanner-commands/dfa/).

## Interpret Your Results

An individual row in the Graph Engine results file represents a violation. Each violation contains sink and source information, plus the actual violation message. 

Here’s a breakdown of the output you see by column name.

* Severity–The severity of the violation. By default, all security violations are marked as severity 1.
* Sink File, Sink Line, Sink Column–The location where the data interaction happens in your source code.
* Source File, Source Line, Source Column–The location where the path begins.
* Source Type, Source Method–Additional information to help identify the path entry.
* Rule–The rule that was run which led to the violation.
* Description–The violation message. For more info on Graph Engine violation messages, read our [FAQ](./en/v3.x/faq/#q-what-do-the-violation-messages-mean).

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

## Unblock Graph Engine Analysis

If your Graph Engine analysis is intentionally blocked, it’s because Graph Engine identified something incorrect in your code. You must modify your code to unblock the analysis. Depending on the situation, you see one of these messages.


| Violation | When it Occurs | Message |
| -------- | ------- | ------- |
| User Action | Returned one time on an entire analysis. <br> Analysis of all code is blocked. | Remove unreachable code to proceed with the analysis. |
| User Action Violation | Returned on a single path. <br> Analysis of only that code path is blocked. | Rename or delete this reused variable to proceed with the analysis. |

### Examples:

**User Action Example.** 

This code example produces multiple actions, such as a `throw` statement followed by a `return` statement.

```
public Integer foo(String input) {
	If (input == ‘Account’) {
		throw new Exception();
		return 5;
	}
	return 10;
}
```

**Result:** A Graph Engine analysis attempt on this code results in the entire analysis being blocked and the User Action message is returned: ```Remove unreachable code to proceed with the analysis```.

**User Action Violation Example.** 

This code example reuses a variable, String input, in the same scope of a method.
```
public Integer foo(String input) {
	String input = ‘another value’;
	System.debug(input);
}
```
**Result:** A Graph Engine analysis attempt on this code path results in a User Action Violation on this path. Analysis on other paths can proceed. Sometimes other violations are returned. This message is returned: ```Rename or delete this reused variable to proceed with the analysis```.


## Understand LimitReached Errors

When Graph Engine analyzes highly complex code, it runs out of heap space, which  results in a `LimitReached` error. To decrease the occurrence of `LimitReached` errors and to complete as much analysis as possible within a shorter period, we added processing limits on Graph Engine. These limits help Graph Engine to fail fast when a path’s analysis is approaching a `LimitReached` error. This fail-fast process includes preemptively aborting a path analysis when Graph Engine encounters a path that’s too complex.

### Recommended Steps to Reduce LimitReached Error Occurrences
To proactively reduce the chances of a `LimitReached` error in your scans, take these steps.

1. Execute `scanner run dfa` with default heap space settings and collect the results to a file using the `--outfile` parameter. The output file contains the majority of the actionable items.
2. Filter your output file on the `LimitReached` violation and group these violations into sets of targets. `LimitReached` violations are the more complex paths that need more heap space and time. 
3. To determine your previous execution’s path expansion limit and maximum heap space allocated, search for this string in `/<home>/.sfdx-scanner/sfge.log`: “Path expansion limit”–You use these values later to control the complexity that Graph Engine can handle for your code.
4. Execute `scanner run dfa` on each `LimitReached` target grouping that you created. 
5. Run `scanner run dfa` iteratively with larger memory allocation each time to exclusively target complex areas. 
	
	**Example**: Sample command allocating max heap space of 20G
 
	```
	sf scanner run dfa --projectdir /path/to/full/project --target /path/to/a/source/file#optionalSpecificEntryMethod --sfgejvmargs "-Xmx20g" --outfile result_2.csv
	```
To optimize your `LimitReached` scans, follow these recommendations.

* Use individual file names in `--target` parameter or names specific to the target method. 
* Use the `--sfgejvmargs` parameter to define a larger heap space than the default.

If the same target row repeatedly reaches the limit, follow these steps.

1. Remove the upper limit by passing in `--pathexplimit -1`.
2. Decrease the number of parallel threads by setting the `--rule-thread-count` parameter to 2.
3. Increase timeout by setting the `--rule-thread-timeout` parameter to 300000 ms.

### Knobs to Control Graph Engine Execution
Two Graph Engine parameters, `--sfgejvmargs` and `--pathexplimit`, act as knobs that turn the max heap size and the complexity of Graph Engine scans up or down. Use these knobs to fine-tune your code’s analysis and rate of `OutOfMemory` occurrences.

#### Modify the Allocated Heap Space with `--sfgejvmargs`
Use the `--sfgejvmargs` parameter to modify your Java Virtual Machine (JVM) default max heap size.

1. Look up your JVM `-Xmx` value, which is your allocated heap size. 
2. Use the `--sfgejvmargs` parameter to increase your `-Xmx` value on `scanner run dfa` command. 
3. Execute Graph Engine with a larger heap space than the default settings.

For example, to allocate 2 G heap space:

	`sf scanner run dfa --sfgejvmargs "-Xmx2g" <rest of your parameters>`

To maximize your heap space balance with Graph Engine performance, follow these recommendations.

* Because the heap space value depends on the complexity of the target codebase, there’s no magic number. A very large heap space can degrade Graph Engine’s performance, so increase the heap space allocation in increments of 1 G. Experiment to see what works for your project.
* Target a smaller set of files for analysis. Provide a subset of Apex files using the `--target` flag on the `scanner run dfa`command while keeping the same `--projectdir` value. This approach reduces the number of paths and reduces the likelihood of `OutOfMemory` errors.
* To avoid large IF/ELSE-IF/ELSE conditional trees, simplify your code, which helps bring down the number of paths created.

#### Set complexity-handling-limit Using`--pathexplimit` Parameter
Heap space allocated for a `scanner run dfa` execution also dictates how much complexity Graph Engine can handle. If you ran our recommended steps earlier, grab the path expansion limit that you looked up.

Override your path expansion limit using the `--pathexplimit` parameter. Or remove the limit by passing in this value as -1.

To find more information about path expansion limits, refer to the `OutOfMemory Error` section in the [FAQ](./en/v3.x/faq/#out-of-memory-error).

## Limitations of Salesforce Graph Engine

Graph Engine has these limitations.

* Violations thrown as `Internal error. Work in progress. Please ignore`, indicate that the entry point’s analysis didn’t complete successfully. We’re working on fixing this issue. In the meantime, you must verify the validity of this error manually.
* Graph Engine handles unique class names. If the source code has two distinctly different files that have classes with duplicate names, Graph Engine fails with an error message: `<example_class> is defined in multiple files`. In cases like these, provide --projectdir subpath to the source directory that has only one of the file names, and rerun Graph Engine with the subpath to the second file name.
* Graph Engine doesn’t handle anonymous Apex script. Provide the class directory path as the `--projectdir` that doesn’t include any anonymous Apex script.
* Graph Engine doesn’t handle namespace placeholders. Leave the namespace placeholder blank.
* Graph Engine supports Apex property chains with a depth of 2 or fewer. For example, Graph Engine supports `Object.x` but not `Object.x.y`.
* Graph Engine doesn’t scan triggers.

### Reporting Errors

We appreciate your help in identifying and fixing issues with Salesforce Graph Engine. To report bugs, create a new issue. 

To verify your bug, include publicly shareable sample code. 

* Create sample code without actual variable names that still mimics the original  issue as closely as possible. 
* Ensure that your sample code runs into the same error as your original code.

If you have thoughts on usability, how the tool works, or new feature requests, we welcome your [feedback](https://www.research.net/r/SalesforceCA). 

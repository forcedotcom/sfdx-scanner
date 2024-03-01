---
title: 'Try it Yourself'
lang: en
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/try-graph-engine.html
---


## Prerequisites

Salesforce Graph Engine (Graph Engine) works a bit differently from other analyzers. To get started with Graph Engine:

* Read the [Introduction to Graph Engine](./en/v3.x/salesforce-graph-engine/introduction/).
* Understand [Working with Graph Engine](./en/v3.x/salesforce-graph-engine/working-with-sfge/).
* Install [Salesforce Code Analyzer](./en/v3.x/getting-started/install/).

Next, run a command to view Graph Engine rules, then install our sample project to try out Graph Engine for yourself.

## See the Rules
To see Graph Engine’s rules, run:

```bash
sf scanner rule list --engine sfge
```

## Clone the Sample Project
All our examples use our [sample app](https://github.com/forcedotcom/sfdx-scanner/tree/dev-3/test/code-fixtures/projects/sfge-working-app/force-app/main/default).

To begin, clone the repo.

`git clone https://github.com/forcedotcom/sfdx-scanner.git`
 
Next, open the sample app directory.

`cd sfdx-scanner/test/code-fixtures/projects/sfge-working-app/`

The sample app contains these key classes.

* `AuraEnabledFls.cls` contains `@AuraEnabled-annotated` methods.
* `PageReferenceFls.cls` includes methods that return `PageReference` objects.
* `VfControllerFls.cls` is a Visualforce Controller for the `VfComponentWithController` component.
* `FlsHelperClass.cls` performs CRUD/FLS checks against objects and fields.

## Run Graph Engine Against All Files
To run Graph Engine, start with a basic evaluation of all files. 

1. Navigate to the sample app root folder. 
`test/code-fixtures/projects/sfge-working-app` 
3. Run: 
`sf scanner run dfa --target './force-app/main/default/classes' --projectdir './force-app/main/default' --format csv`

Review the results. Notice that each violation has a source and a sink vertex. 
* The source vertex is the start of the path in question.
* The sink vertex is the point where the DML operation occurs. 
* If there’s insufficient CRUD/FLS validation between those two points, a violation is thrown.

For example, look at some `AuraEnabledFls.cls` methods that threw violations.
* `flsHelperGivenIncorrectObjectType()`. This method has no branches. Instead, it’s just a single path all the way through. CRUD/FLS was performed on the wrong object type, resulting in the violation. The source vertex is the line where the method is declared, and the sink vertex is the line where the account is inserted.
* `flsInIfBranchOnly()`. This method has an `if` statement, so it has two paths: one that goes through the `if`, and one that doesn’t. Because CRUD/FLS only occurs in one of those paths, a violation is thrown.

Two `AuraEnabledFls.cls` methods didn’t throw violations.
* `flsDoneCorrectly()`. All the fields inserted are checked with the FlsHelperClass first. The method is secure, and no violation was thrown.
* `flsInNonAuraMethod()`. This method isn’t a recognized [entry-point or source](./en/v3.x/salesforce-graph-engine/rules/#dfa-rules), and it isn’t in the call-stack of any entry points. Graph Engine skipped this method even though it’s technically insecure.

## Run Graph Engine Against a Single File
After you fix violations that Graph Engine identified in a specific file, run Graph Engine against that specific file to double-check your work. 

To run Graph Engine against a single file, run:

```
sf scanner run dfa --target './force-app/main/default/classes/AuraEnabledFls.cls' --projectdir './force-app/main/default' --format csv
```

Keep in mind that:

* The `target` file contains the source vertices that you want to scan.
* The `projectdir` is the entire project that Graph Engine builds all paths against.

Graph Engine runs faster against one file than a whole project because it analyzes a smaller number of paths. It also returns results only for source vertices that are in the targeted file.

## Fixing Violations
Pick one of the violations in the sample file to fix. For example, to fix `flsInIfBranchOnly()`, you can:

* Move the CRUD/FLS check out of the `if` branch so it always runs.
* Move the DML operation into the `if` branch so it only runs in the path that performed the CRUD/FLS check.
* Add an `else` branch that performs the same CRUD/FLS checks as the if branch.

Try one or several of these options, then run the provided command again. If you do it right, then the number of violations in the file is smaller.

If you want more experience with Graph Engine, experiment with the remaining violations in the sample app.

## Target Individual Methods
After you fix the violations in a given method, sometimes you want to analyze that method individually. For example, suppose you only want to analyze the `flsHelperGivenIncorrectObjectType()` and `flsHelperMultipleInstances()` methods in `AuraEnabledFls.cls`.

To analyze these two methods only, run:

```
sf scanner run dfa --target './force-app/main/default/classes/AuraEnabledFls.cls#flsHelperGivenIncorrectObjectType;flsHelperMultipleInstances' --projectdir './force-app/main/default' --format csv
```

Running Graph Engine against specific methods has these limitations:

* The syntax is only supported for file paths. You can’t use it with globs or directories.
* If multiple methods in the target file share the specified name, then all such methods are included. Overloads and inner classes are some examples.
* Methods specified through method-level targeting are considered [path entrypoints](./en/v3.x/salesforce-graph-engine/rules/#dfa-rules) even when they otherwise wouldn’t be. This misidentification can cause methods that would ordinarily be skipped to be analyzed.

## Skip a Violation
Sometimes false positives can occur. Other times, you identify a reason why a CRUD/FLS check is unnecessary, such as your code is only executed from an admin-only page. If you want to skip a violation due to a false positive or other reason, use [engine directives](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives).

To skip a violation using an engine directive:

1. Add `/* sfge-disable-next-line ApexFlsViolationRule */` before the DML operation in `flsNoEnforcementAttempted()`.
2. Rerun the command.
3. The violations in the identified method are suppressed.

You can also suppress all violations in a method by adding `/* sfge-disable-stack ApexFlsViolationRule */` immediately above the method, or in the entire file by adding `/* sfge-disable ApexFlsViolationRule */` at the top of the class.

---
title: 'Try it yourself'
lang: en
---


## Before you get started
SFGE works a bit differently from other analyzers with which you might be familiar. As such, we strongly encourage you
to read about [the engine](./en/v3.x/salesforce-graph-engine/introduction) and [its capabilities](./en/v3.x/salesforce-graph-engine/working-with-sfge) before proceeding.

Also, so you can try these examples yourself, please install the pilot version of the analyzer as per [these instructions](./en/v3.x/getting-started/install).

Once you've done all that, continue reading here to see some basic SFGE operations, so you can get a sense of how it all works together.

## See the rules
First thing's first. Let's see the rules that exist in SFGE. Run the following command.

```bash
sfdx scanner:rule:list --engine sfge
```

Note that there's just the one rule. [ApexFlsViolationRule](./en/v3.x/salesforce-graph-engine/rules/#apexflsviolationrule)
identifies CRUD/FLS vulnerabilities in your Apex code. In the future, more rules will likely be added, but for now there's
just that rule. As such, all of the examples will focus on CRUD/FLS.

## Let's look at the sample project
All of our examples will be using the [sample app](https://github.com/forcedotcom/sfdx-scanner/tree/dev-3/test/code-fixtures/projects/sfge-working-app/force-app/main/default).

To begin:

```bash
// clone the `dev-3` branch of repo:

git clone --branch dev-3 https://github.com/forcedotcom/sfdx-scanner.git

```

```bash
// open sample app directory:

cd sfdx-scanner/test/code-fixtures/projects/sfge-working-app/force-app/main/default
```


Before we start running our rules, let's take a look at the sample app and take note of a few things.

The following files are noteworthy:
- `AuraEnabledFls.cls` is a class that has a bunch of `@AuraEnabled`-annotated methods.
- `PageReferenceFls.cls` is a class with a bunch of methods that return `PageReference` objects.
- `VfControllerFls.cls` is a Visualforce Controller for the `VfComponentWithController` component.
- `FlsHelperClass.cls` is a class that performs CRUD/FLS checks against objects and fields.

It may be advantageous for you to skim those files now.

## Basic Run
Let's start with a basic evaluation of all files. `cd` into `test/code-fixtures/projects/sfge-working-app`, then run the
following command:
```
sfdx scanner:run:dfa --target './force-app/main/default/classes' --projectdir './force-app/main/default' --format csv
```

Take a look at the results. Note that, as explained above, each violation has a source and a sink vertex. The source vertex
is the start of the path in question, and the sink vertex is the point where the DML operation is occurring. If there is
insufficient CRUD/FLS validation between those two points, a violation is thrown.

Let's pick a few of those violations and look a little bit closer at them.

Several methods in `AuraEnabledFls.cls` threw violations.

Let's start with [`flsHelperGivenIncorrectObjectType()`](https://github.com/forcedotcom/sfdx-scanner/blob/dev-3/test/code-fixtures/projects/sfge-working-app/force-app/main/default/classes/AuraEnabledFls.cls#L4).
Note that this method has no branches. Instead, it's just a single path all the way through. Also note that we're performing
CRUD/FLS on the wrong object type, hence the violation. The source vertex is the line where the method is declared, and
the sink vertex is the line where the account is inserted.

Next, let's look at a slightly more complicated example, [`flsInIfBranchOnly()`](https://github.com/forcedotcom/sfdx-scanner/blob/dev-3/test/code-fixtures/projects/sfge-working-app/force-app/main/default/classes/AuraEnabledFls.cls#L60).
This method has an `if` statement, and therefore has two paths: one that goes through the `if`, and one that doesn't. Since
CRUD/FLS only occurs in one of those paths, a violation is thrown.

Additionally, let's look at a method that didn't throw a violation: [`flsDoneCorrectly()`](https://github.com/forcedotcom/sfdx-scanner/blob/dev-3/test/code-fixtures/projects/sfge-working-app/force-app/main/default/classes/AuraEnabledFls.cls#L76).
Note that all of the fields being inserted are first being checked with the `FlsHelperClass` instance. As such, the method
is secure, and no violation was thrown.

Finally, one more method that didn't throw a violation: [`flsInNonAuraMethod()`](https://github.com/forcedotcom/sfdx-scanner/blob/dev-3/test/code-fixtures/projects/sfge-working-app/force-app/main/default/classes/AuraEnabledFls.cls#L91).
This method is neither a recognized [entry-point/source](./en/v3.x/salesforce-graph-engine/rules/#apexflsviolationrule), nor is it in the call-stack of any entry-points. As such, the analyzer skipped this method even though it is technically insecure.

## Running against a single file
As you fix the problems in this file, you'll probably want to run the analyzer against this file specifically rather than
the entire codebase. You can do that by running the following command:
```
sfdx scanner:run:dfa --target './force-app/main/default/classes/AuraEnabledFls.cls' --projectdir './force-app/main/default' --format csv
```
Please note the following, as they may save you some grief in the future.
- The `target` file is the one containing the source vertices you wish to scan.
- The `projectdir` must still be the entire project, so that paths can be properly built.

Note that the analyzer ran faster than it did last time, because it was analyzing a smaller number of paths. And note that
the results are only those whose source vertex is in the targeted file.

## Fixing violations
Let's start by picking one of the violations in the file to fix. To fix `flsInIfBranchOnly()`, you can do one of a number
of things.
- You could move the CRUD/FLS check out of the `if` branch, so it's always run.
- You could move the DML operation into the `if` branch, so it's only run in the path that performed the CRUD/FLS check.
- You could add an `else` branch that performs the same CRUD/FLS checks as the `if` branch.

Try one (or several) of these options, then run the provided command again. If you've done it right, then the number of
violations in the file should be decreased by one.

The remaining violations in the working app have been left as exercises for you to complete.

## Skipping a violation
Suppose that one of these violations was a false positive (they're not, but let's pretend). Or alternatively, suppose that
you have a good reason for why a CRUD/FLS check is actually unnecessary (e.g., the code is only executed from an admin-only page).
If you're really, truly certain that you want to skip that violation, you can use [engine directives](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to do so.

For example, if you add `/* sfge-disable-next-line ApexFlsViolationRule */` before the DML operation in `flsNoEnforcementAttempted()`
and rerun the command, the violation in that method will be suppressed.

You can also suppress all violations in an entire file by adding `/* sfge-disable ApexFlsViolationRule */` at the top of the class.

---
title: 'Salesforce Graph Engine'
lang: en
---
## What is the Salesforce Graph Engine?
Salesforce Graph Engine (SFGE) is an open-source Salesforce project used to detect security and quality issues on Salesforce languages. It has the capability to perform more complex checks than an average static analysis tool since it executes [Data Flow Analysis](https://en.wikipedia.org/wiki/Data-flow_analysis).

Salesforce Graph Engine identifies the entry point methods in your specified target, and builds code flow paths across your source code. As SFGE walks the paths, it builds context on how data is transformed at each node of the path.

Rules express interest at specific nodes and are invoked when these nodes are visited while walking the path. Rules execute their checks and use the data context to identify issues. 

Once the engine completes walking all the paths, it returns the issues collected as rule violations.

SFGE uses Apex Jorje Compiler jar to compile Apex code. It uses [Apache TinkerPop Graph Database](https://tinkerpop.apache.org/) to store vertices and [Gremlin Query Language](https://tinkerpop.apache.org/gremlin.html) to help with creating paths.

-------

![Steps run by SFGE](./assets/images/SFGE_flow_overview.png)

-------
As a starting point, SFGE supports one language and one rule: Apex and the ApexFlsViolationRule rule. ApexFlsViolationRule detects [Create, Read, Update, and Delete (CRUD) and Field-Level Security (FLS) violations](https://www.youtube.com/watch?v=1ZYjpjPTIn8).

## ApexFlsViolationRule

### Background

A CRUD/FLS check contains three parts.

*Source*. an entry point for external interaction

*Sink*. code that interacts with the database

*Sanitizer*. the check that happens between Source and Sink to ensure that the user who is performing this action on data has the necessary access to the object/fields.

A code path must have a sanitizer in between the source and the sink. When the sanitizer is missing, you see a CRUD/FLS violation. To avoid this, you want to ensure that each path created from any source to sink is sanitized.

A source could lead to multiple sinks. Also, a sink can be reached through multiple sources. In fact, we could have multiple paths between the same source and sink. We want to ensure that each path created from any source and sink is sanitized.

### Interpret Your Results

No matter which format you choose, the individual rows in the results represents violations. Each violation contains Sink information, Source information, and the actual violation message. Here’s the breakdown by column name to explain the data it represents:

1. Severity. The severity of the violation. By default, all security violations are marked as severity 1.
2. Sink File, Sink Line, Sink Column. The location where in your source code that the data interaction happens
3. Source File,Source Line, Source Column. The location where the path begins
4. SourceType, SourceMethod. Additional information to help identify the path entry
5. Rule. The rule that was run which led to the violation. As of today, ApexFlsViolationRule is the only rule.
6. Description. The violation message. For more info on SFGE violation messages, read our [FAQ](../faq.md).

## Add Engine Directives

Like all Security tools, SFGE could create both false negatives or false positives. For example, the engine could fail to create a violation where the code is insecure, a false negative. Or create a violation even though the code is secure, a false positive. If you determine that SFGE has created a false positive, add engine directives to your code so that SFGE doesn’t throw that violation anymore.

Salesforce Graph Engine understands three levels of engine directives:

### Disable next line

`/* sfge-disable-next-line ApexFlsViolationRule */`

Use this when you want to disable just the CRUD operation from Salesforce Graph Engine’s analysis.
Example usage:

```
/* sfge-disable-next-line ApexFlsViolationRule */
insert a;
```

### Disable stack

`/* sfge-disable-stack ApexFlsViolationRule */`

Use this when you want to disable all the paths that originate from the method. Make sure your engine directive is the line immediately before the method declaration of the stack you wish to disable.
Example usage:

```
/* sfge-disable-stack ApexFlsViolationRule */
public void myMethod() {
```


If the stack you wish to disable has an @AuraEnabled annotation, add the engine directive right after the annotation before the method declaration. This use case would be helpful when you have an entry point that you know is already secure, probably because only the System Admin can access it.

```
@AuraEnabled
/* sfge-disable-stack ApexFlsViolationRule */
public static String myAuraEnabledMethod() {
```

### Disable class

`/* sfge-disable ApexFlsViolationRule */`

Use this when you want to disable all the CRUD operations that occur in the class. As with the other two engine directives, make sure you add it in the line immediately before class declaration.
Example usage:

```
/* sfge-disable ApexFlsViolationRule */
public class MyClass {
```

## Limitations of Salesforce Graph Engine

Since the engine is actively under development, there are many features and bugs that are still work in progress.

1. Violations thrown as, `Internal error. Work in progress. Please ignore`, indicate that the entry point’s analysis did not complete successfully. Please verify its correctness manually.
2. SFGE cannot handle duplicate class names. If the source code has two distinctly different files that have classes with the same names, the engine fails with an error message, `<example_class> is defined in multiple files`. In cases like these, please provide `--projectdir` a subpath to the source directory that has only one of the file names, and rerun with  the subpath to the second duplicate.
3. SFGE cannot handle anonymous apex script. Please provide the classes directory path as the `--projectdir` that does not include any anonymous apex script.
4. SFGE cannot handle namespace placeholders. Please replace the namespace-placeholder-dot with a blank.
5. SFGE does not understand multiple static blocks in code.

### How to report an error?

If you notice an issue beyond these limitations, please create a new issue using this format (<new issue format for SFGE>).

Since SFGE builds paths, we may need to see code invoked from the current class. However, we understand that not all code is shareable to public depending on what you are working on. Please create alternative code without actual variable names that still mimics the original, failing issue as closely as possible. Also, ensure that your sample code runs into the same error as your original code. We appreciate your help in identifying and fixing issues.

We welcome your feedback on usability or other features. Create general issues for these and let us know your thoughts.

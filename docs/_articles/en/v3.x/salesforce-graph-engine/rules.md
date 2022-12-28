---
title: 'Rules'
lang: en
---

## ApexFlsViolationRule
ApexFlsViolationRule detects [Create, Read, Update, and Delete (CRUD) and Field-Level Security (FLS) violations](https://www.youtube.com/watch?v=1ZYjpjPTIn8).

### Definitions

| Rule Component | Definition                                  																		|
| ---------		 | ---------                                															  			|
|**Source**		 |																													|
|		 	  	 | `@AuraEnabled`-annotated methods     																			|
|				 |`@InvocableMethod`-annotated methods																			  	|
|     			 | `@NamespaceAccessible`-annotated methods 																		|
| 				 |`@RemoteAction`-annotated methods																				  	|
|				 |Any method returning a `PageReference` object																	  	|
|				 |`public`-scoped methods on Visualforce Controllers																|
|				 |`global`-scoped methods on any class																			  	|
|				 |`Messaging.InboundEmailResult handleInboundEmail()` methods on implementations of `Messaging.InboundEmailHandler`	|
|				 |Any method targeted during invocation																|
| **Sink**		 | 																													|
| 		  	  	 |All DML operations and their Database.method() counterparts:   													|
|				 |* delete																											|
|     			 |* insert																											|
| 				 |* merge																										  	|
|				 |* undelete																										|
|				 |* update																										  	|
|				 |* upsert																										  	|
|				 |SOQL queries and Database.query counterpart																	  	|
| **Sanitizer**	 |				                                  														      		|
|			 	 |Access check performed using [Schema.DescribeSObjectResult](https://developer.salesforce.com/docs/atlas.en-us.234.0.apexref.meta/apexref/apex_methods_system_sobject_describe.htm)|
|				 |Acceptable only for operations that require CRUD-level checks such as DELETE, UNDELETE, and MERGE.				|
|				 |Access check performed using [Schema.DescribeFieldResult](https://developer.salesforce.com/docs/atlas.en-us.234.0.apexref.meta/apexref/apex_methods_system_fields_describe.htm)|
|				 |Acceptable for operations that require FLS-level checks. Includes READ, INSERT, UPDATE, UPSERT for Standard data objects and Custom Objects|																
|				 |SOQL queries that use [WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm#apex_classes_with_security_enforced)|
|				 |Lists filtered by [Security.stripInaccessible](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)|

### Interpreting ApexFlsViolationRule Results

Match any violation message that you receive with these cases to understand more about the violation.

*Common Case*

>_Validation-Type_ validation is missing for _Operation-Name_ operation on _Object-Type_ with fields _Comma-Separated-Fields_

Parameter explanation:

* _Validation-Type_: Type of validation to be added. CRUD requires object-level checks, and FLS requires field-level checks.

* _Operation-Name_: Data operation that must be sanitized.

* _Object-Type_: Object on which the data operations happen. If Graph Engine couldn’t guess the object type, you see the variable name or *SFGE_Unresolved_Argument*.

* _Comma-Separated-Fields_: Fields on which the data operation works. If you see _Unknown_ as the only field or as one of the fields, Graph Engine didn't have enough information to guess the fields, and you must determine the unlisted fields manually.

*Additional Clause Case*

> _Validation-Type_ validation is missing for _Operation-Name_ operation on _Object-Type_ with fields _Comma-Separated-Fields_ - Graph Engine couldn't parse all objects and fields correctly. Confirm manually if the objects and fields involved in these segments have FLS checks: _Unknown-Segments_

Same as the common case, but also Graph Engine isn't confident about the object names or field names it detected. You also see this additional clause when your field or object ends with `__r`. In both cases, review the relational field, object, and the unparsed segments to ensure they have the required CRUD/FLS checks. Next, add an [engine directive](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to force Graph Engine to ignore this warning in the next run.

*stripInaccessible Warning Case*

The `stripInaccessible` warning is thrown for all `stripInaccessible` checks on READ access type. Graph Engine has no way to ensure that the sanitized value returned by `SecurityDecision` is the value used in the code that follows the check. Confirm the values manually, then add an engine directive to force Graph Engine to ignore this warning in the next run. Alternatively, disable these violations by using the `--rule-disable-warning-violation` flag or setting its corresponding environment variable, SFGE_RULE_DISABLE_WARNING_VIOLATION, to true.

*Internal Error Case*

> Graph Engine identified your source and sink, but you must manually verify that you have a sanitizer in this path. Then, add an engine directive to skip the path. Next, create a GitHub issue for the Code Analyzer team that includes the error and stack trace. After we fix this issue, check the Code Analyzer release notes for more info. Error and stacktrace: [details]

Graph Engine ran into an error while walking this path. Manually verify that you have a sanitizer on the path, and add an engine directive to skip the path. Next, create a GitHub issue for the Code Analyzer team that includes the error and stack trace so we can research and resolve it. After we determine a fix for the issue, check Code Analyzer [Release Information](./en/v3.x/release-information/) for more info.

#### See Also

- [FAQ](./en/v3.x/faq/#questions-about-interpreting-apexflsviolationrule-results)
- [Enforce Security With the stripInaccessible Method](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)
- [Enforcing Object and Field Permissions](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_perms_enforcing.htm)
- [Filter SOQL Queries Using WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm)
- [Frequently Asked Questions](./en/v3.x/faq/)

## UnusedMethodRule

UnusedMethodRule detects methods contained in your code that aren’t invoked. At this stage, it detects:

- unused private instance methods
- unused private constructors
- unused protected constructors

To invoke UnusedMethodRule, you must provide `--engine sfge` and `--projectdir/-p` on `scanner:run`. 

### Definition

Unlike ApexFlsRule, UnusedMethodRule doesn't use sources or sinks. Instead, UnusedMethodRule is a traditional static analysis rule. A violation occurs at a point in the code where the unused method is declared. UnusedMethodRule has a layer of graph-based intelligence on top of the traditional Abstract Syntax Tree (AST) hierarchy to understand a bigger picture of the code.

### Interpreting UnusedMethodRule Results

Match any violation message that you receive with this case to understand more about the violation.

*Common Case*

> Method X in class Y is never invoked.
 
Parameter Explanation:

Because no invocations of the indicated method were found, the method is unnecessary and can be deleted.

### UnusedMethodRule Limitations

- UnusedMethodRule works on private or protected constructors and private instance methods.
- These methods and constructors aren’t supported, are excluded from analysis, and may produce false negatives:
    - public or global constructors
    - protected, public, or global instance methods
    - static methods of any visibility

## Roadmap

We’re working on adding more rules. In the meantime, give us your [feedback](https://www.research.net/r/SalesforceCA).

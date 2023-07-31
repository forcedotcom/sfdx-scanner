---
title: 'Rules'
lang: en
---

Salesforce Graph Engine includes path-based and data-flow analysis rules.

| Rule | Type | Catgegory | Status | Description |
| -------- | ----------- | ----------- |----------- | ----------- |
| ApexFlsViolationRule | Path-based analysis | Security | Generally Available (GA) | Detects Create, Read, Update, and Delete (CRUD) and Field-Level Security violations. |
| ApexNullPointerExceptionRule | Path-based analysis | Error-Prone | GA | Identifies Apex operations that dereference null objects and throw NullPointerExceptions. |
| AvoidDatabaseOperationInLoop | Path-based analysis | Performance | Pilot | Detects database operations in loops that degrade performance. |
| AvoidMultipleMassSchemaLookups | Path-based analysis | Performance | GA | Detects scenarios where expensive schema lookups are made more than one time in a paths. |
| RemoveUnusedMethod | Path-based analysis | Performance | Pilot | Detects methods contained in your code that aren’t invoked from any entry points that Graph Engine recognizes. |
| UnimplementedTypeRule | Graph-based analysis | Performance | GA | Detects abstract classes and interfaces that are non-global and missing implementations or extensions. |
| UseWithSharingOnDatabaseOperation | Path-based analysis | Security | Pilot | Detects database operations outside with-sharing-annotated classes. |

## Running Graph Engine GA Rules
Run all Graph Engine rules against your code, or run a subset of rules by type or by category.

To run the path-based rules run: `scanner:run:dfa --projectdir MyDirectory`.

**Example:**
  
```sfdx scanner:run:dfa --projectdir /project/dir --target /project/dir/target1```

<br>
To run graph-based analysis rules run: `scanner:run --engine sfge --projectdir MyDirectory`.

**Example:**
  
```sfdx scanner:run --engine sfge --projectdir /project/dir --target /project/dir/target1```

<br>
To run a specific category of rules, include the category.
  
**Example:**
  
```sfdx scanner:run:dfa --category "Security" --projectdir /project/dir --target /project/dir/target```

<br>
## Running Graph Engine Pilot Rules

To run each Graph Engine pilot rule, include the ```--with-pilot``` flag in your request. 

To run all Graph Engine rules and all pilot rules, run: ```sfdx scanner:run:dfa --with-pilot --engine sfge --projectdir MyDirectory```.

**Example**:

```sfdx scanner:run:dfa --with-pilot --engine sfge --projectdir /project/dir --target /project/dir/target1```

<br>
To run a specific category of rules including the pilot rules in that category, include the category and the ```--with-pilot``` flag.

**Example**:
  
```sfdx scanner:run:dfa --category “Performance” --with-pilot --engine sfge --projectdir /project/dir --target /project/dir/target1```

<br>
## Generally Available Rules

### ApexFlsViolationRule <a name='ApexFlsViolationRule'>#</a>
ApexFlsViolationRule detects [Create, Read, Update, and Delete (CRUD) and Field-Level Security (FLS) violations](https://www.youtube.com/watch?v=1ZYjpjPTIn8).

#### Definitions

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
|				 |Any method targeted during invocation																				|
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
|				 |Acceptable only for operations that require CRUD-level checks such as DELETE, UNDELETE, and MERGE				|
|				 |Access check performed using [Schema.DescribeFieldResult](https://developer.salesforce.com/docs/atlas.en-us.234.0.apexref.meta/apexref/apex_methods_system_fields_describe.htm)|
|				 |Acceptable for operations that require FLS-level checks. Includes READ, INSERT, UPDATE, UPSERT for Standard data objects and Custom Objects|																
|				 |SOQL queries that use [WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm#apex_classes_with_security_enforced)|
|				 |Lists filtered by [Security.stripInaccessible](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)|

#### Interpreting ApexFlsViolationRule Results

Match any violation message that you receive with these cases to understand more about the violation.

*Common Case*

>_Validation-Type_–validation is missing for _Operation-Name_ operation on _Object-Type_ with fields _Comma-Separated-Fields_

Explanation:

* _Validation-Type_–Type of validation to be added. CRUD requires object-level checks, and FLS requires field-level checks.

* _Operation-Name_–Data operation that must be sanitized.

* _Object-Type_–Object on which the data operations happen. If Graph Engine couldn’t guess the object type, you see the variable name or *SFGE_Unresolved_Argument*.

* _Comma-Separated-Fields_–Fields on which the data operation works. If you see _Unknown_ as the only field or as one of the fields, Graph Engine doesn't have enough information to guess the fields, and you must determine the unlisted fields manually.

*Additional Clause Case*

> _Validation-Type_ validation is missing for _Operation-Name_ operation on _Object-Type_ with fields _Comma-Separated-Fields_–Graph Engine couldn't parse all objects and fields correctly. Manually confirm if the objects and fields involved in these segments have FLS checks: _Unknown-Segments_

This case is the same as the common case, but also Graph Engine isn't confident about the object names or field names that it detected. You also see this clause when your field or object ends with `__r`. In both cases, review the relational field, object, and the unparsed segments to ensure that they have the required CRUD/FLS checks. Next, add an [engine directive](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to force Graph Engine to ignore this warning in the next run.

*stripInaccessible Warning Case*

The `stripInaccessible` warning is thrown for all `stripInaccessible` checks on READ access type. Graph Engine can't ensure that the sanitized value returned by `SecurityDecision` is the value used in the code that follows the check. Confirm the values manually, then add an engine directive to force Graph Engine to ignore this warning in the next run. Or, disable these violations by using the `--rule-disable-warning-violation` flag or by setting its corresponding environment variable, SFGE_RULE_DISABLE_WARNING_VIOLATION, to true.

*Internal Error Case*

> Graph Engine identified your source and sink, but you must manually verify that you have a sanitizer in this path. Then, add an engine directive to skip the path. Next, create a GitHub issue for the Code Analyzer team that includes the error and stack trace. After we fix this issue, check the Code Analyzer release notes for more info. Error and stacktrace: [details]

Graph Engine encountered an error while walking this path. Manually verify that you have a sanitizer on the path, and then add an engine directive to skip the path. Next, create a GitHub issue for the Code Analyzer team that includes the error and stack trace so that we can research and resolve it. After we determine a fix for the issue, check Code Analyzer [Release Information](./en/v3.x/release-information/) for more info.

#### See Also

- [FAQ](./en/v3.x/faq/#questions-about-interpreting-apexflsviolationrule-results)
- [Enforce Security With the stripInaccessible Method](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)
- [Enforcing Object and Field Permissions](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_perms_enforcing.htm)
- [Filter SOQL Queries Using WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm)
- [Frequently Asked Questions](./en/v3.x/faq/)

### ApexNullPointerExceptionRule <a name='ApexNullPointerExceptionRule'>#</a>

ApexNullPointerExceptionRule identifies Apex operations that dereference null objects and throw NullPointerExceptions. NullPointerExceptions generally indicate underlying problems in your code to address. 

**Examples:**

```
public void example1() {
	Object myStr = null;
	System.debug(myStr.toLowerCase()); 
// throws System.NullPointerException 
// since method is invoked on null object.
}

public void example2(String myStr) {
	if (myStr == null) {
		// The following line throws System.NullPointerException, since the conditional confirms
		// that myStr is null when it reaches here.
		System.debug(myStr.toLowerCase());
	}
}

public void example3() {
	Integer i; // Not initialized
	Integer y = i + 2; 
// throws System.NullPointerException since 
// binary operation is invoked on null object
}
```

In these examples, Apex Runtime throws a `System.NullPointerException` at the place where an operation is performed on a null object. The Graph Engine ApexNullPointerException rule preemptively identifies these operations. 

To fix NullPointerException issues, use one of these methods.

- Check that the object is `not-null` before performing the operation
- Make sure that all variables are initialized

Avoid initializing variables to `null`. If your logic demands initialization to `null`, make sure to reassign a value to your variable before you invoke an operation on it.

#### Definitions

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
|				 |Any method targeted during invocation																				|
| **Sink**		 | 																													|
|		 	  	 | Any object dereference is a potential sink because object dereferences are the places where a null pointer exception could throw. Example: `x.someMethod() `   																			                             |
| **Sanitizer**	 |				                                  														      		|
|                |Non-null initialization of variables and null checks before accessing. Examples: `String s = 'abcde';` , `if (s != null) {` |

#### Interpreting ApexNullPointerExceptionRule Results

Match any violation message that you receive with this case to understand more about the violation.

*Common Case*

> ApexNullPointerExceptionRule identifies Apex operations with a high likelihood of throwing a NullPointerException

Explanation:

The operation dereferences a null object and throws a NullPointerException. Review your code and add a null check.

### AvoidMultipleMassSchemaLookups Rule <a name='AvoidMultipleMassSchemaLookups'>#</a>

AvoidMultipleMassSchemaLookups is a path-based rule that detects scenarios where expensive schema lookups are made more than one time in a path and cause performance degradation. 

These methods are identified by AvoidMultipleMassSchemaLookups.

* `Schema.getGlobalDescribe()`
* `Schema.describeSObjects(...)`

Flagged lookups include:

* Lookups within these types of loops: ForLoopStatement, ForEachLoopStatement, DoWhileStatement, and WhileLoopStatement
* Multiple expensive schema lookups that are invoked
* An expensive schema lookup that is executed multiple times

These common scenarios trigger a violation from AvoidMultipleMassSchemaLookups.
* `Schema.getGlobalDescribe()` within a loop
* `Schema.describeSObjects(...)` within a loop
* `Schema.getGlobalDescribe()` preceding a `Schema.getGlobalDescribe()` or `Schema.describeSObjects(...)` method call anywhere in the path
* `Schema.describeSObjects(...)` preceding a `Schema.describeSObjects(...)` or `Schema.getGlobalDescribe()` method call anywhere in the path

#### Definitions

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
|				 |Any method targeted during invocation																				|
| **Sink**		 | 																													|
| 		  	  	 |`Schema.getGlobalDescribe()`													|
|				 |`Schema.describeSObjects(...)`																										|

#### Interpreting AvoidMultipleMassSchemaLookups Results
Match any violation message that you receive with these cases to understand more about the violation.

##### Loop Case

> `Schema.getGlobalDescribe` was called inside a loop. `[ForEachStatement at AuraEnabledFls:27]`

Explanation:

Your code calls `Schema.getGlobalDescribe()` or `Schema.describeSObjects(...)` inside a loop statement. Modify your code to move the `Schema.getGlobalDescribe()` or `Schema.describeSObjects(...)` outside the loop, then rescan your code. 

##### Multiple Schema Lookups Are Invoked Case

> Multiple expensive schema lookups are invoked. `[Schema.describeSObjects at AuraEnabledFls:27]`

Explanation:

Your code invokes `Schema.getGlobalDescribe()` preceded by `Schema.describeSObjects`. Modify your code so that only one expensive schema lookup is invoked.

##### More Than One Execution in a Path Case

> `Schema.getGlobalDescribe` executed multiple times in the call stack. `[getFields at AuraEnabledFls:27, getFields at AuraEnabledFls:28, getFields at AuraEnabledFls:29]`

Explanation:

`Schema.getGlobalDescribe` or `Schema.describeSObjects` is executed multiple times in a single path. Reduce the execution of the method to one time, then rescan your code.

### UnimplementedTypeRule <a name='UnimplementedTypeRule'>#</a>

UnimplementedTypeRule detects abstract classes and interfaces that are non-global and missing implementations or extensions.

#### Definition

UnimplementedTypeRule is a traditional static analysis rule where a violation occurs at a point in the code where the interface or abstract class is declared. It doesn’t use sources or sinks.

#### Interpreting UnimplementedTypeRule Results

Match any violation message that you receive with this case to understand more about the violation.

*Common Case*

> Extend, implement, or delete %s %s

Explanation:

Because this abstract class or interface has no implementations or extensions, it can’t be instantiated. It’s unnecessary and can be deleted.

#### UnimplementedTypeRule Limitations

Because UnimplementedType rule excludes `global` scoped classes from consideration, these classes are prevented from being thrown as false positives and aren’t false negatives.

## Pilot Rules

### AvoidDatabaseOperationInLoop <a name='AvoidDatabaseOperationInLoop'>#</a>

AvoidDatabaseOperationInLoop detects database operations that occur inside loops and which cause performance degradation. Database operations within loops cause performance degradations and exceed Salesforce Governor Limits. 

To remediate database operation violations, move these operations outside of loops. Follow these best practices.

* Instead of querying objects within a loop, use a SOQL For Loop to iterate over a query’s results..

For example, instead of:

```
for (String name : names) {
   Account[] accs = [SELECT Name FROM Account WHERE Name =: name];
```

   Use:
```
for (Account[] accs : [SELECT Name FROM Account WHERE Name IN :names]) {
```

* Instead of putting database operations within a loop, use the loop to iteratively construct lists of objects, and then perform the database operation on the full list after the loop.

For example, instead of:

```
for (String n : names) {
   insert new Account(Name = n);
```

   Use:
```
Account[] accs = new List<Account>();
for (String n : names) {
   accs.add(new Account(Name = n));
}
insert accs;
```

#### Definition

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
|				 |Any method targeted during invocation																				|
| **Sink**		 | 																													|
| 		  	  	 |Any database operation													|						

#### Interpreting AvoidDatabaseOperationInLoop Results

*Loop Case* 

> A database operation occurred inside a loop.` [%s at %s:%d]`

Explanation:

Your code executes a database operation inside a loop statement. Modify your code to move the database operation outside the loop, then rescan your code.

### RemoveUnusedMethod Rule <a name='RemoveUnusedMethod'>#</a>

RemoveUnusedMethod is a path-based analysis rule that detects many methods contained in your code that aren’t invoked from any entry points that Graph Engine recognizes. It detects:

- static methods
- instance methods

RemoveUnusedMethod recognizes these entry points.

* @AuraEnabled-annotated methods
* @InvocableMethod-annotated methods
* @NamespaceAccessible-annotated methods
* @RemoteAction-annotated methods
* Any method returning a PageReference object
* Public-scoped methods on Visualforce Controllers
* Global-scoped methods on any class
* Messaging.InboundEmailResult handleInboundEmail() methods on implementations of Messaging.InboundEmailHandler
* Any method targeted during invocation

#### Definition
RemoveUnusedMethod uses sources like ApexFlsViolationRule does, but the RemoveUnusedMethod sinks are different. Instead of seeking DML operations that occur in the course of a path, RemoveUnusedMethod sinks track all method invocations that occur on that path and use that information to identify methods that are never invoked.

#### Interpreting RemoveUnusedMethod Results

Match any violation message that you receive with this case to understand more about the violation.

*Common Case*

> Method %s in class %s isn’t used in any path from any recognized entry point.
 
Explanation:

Because no invocations of the indicated method were found in the paths originating from the identified entry points, the method is unnecessary and can be deleted.

#### RemoveUnusedMethod Limitations

- RemoveUnusedMethod works on static methods and instance methods. Constructors aren't detected.
- Global methods are intentionally excluded because their external usage is assumed.
- If the set of files included in `--target` is smaller than the files included in `--projectdir`, then `RemoveUnusedMethod` can return unexpected results. For that reason, we recommend running this rule against your whole codebase, not against individual files.

### UseWithSharingOnDatabaseOperation <a name='UseWithSharingOnDatabaseOperation'>#</a>

The UseWithSharingOnDatabaseOperation rule identifies database operations in classes annotated as `without sharing`. It also warns of database operations in classes that inherit with sharing implicitly instead of explicitly using `inherited sharing`. 

With Salesforce sharing rules, you can control who has access to which records, but it is your responsibility to ensure that your Apex code respects sharing rules. To do this, declare classes with a sharing model.

* `with sharing` causes database transactions in a class to respect sharing rules. It is the default recommendation.
* `without sharing` causes database transactions in a class to ignore sharing rules. Use with caution.
* `inherited sharing` causes database transactions in a class to inherit the sharing model of the class that called it. Use for classes that require flexibility.

To protect user data in Apex, use `with sharing` or `inherited sharing` whenever possible. 

#### Definition

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
|				 |Any method targeted during invocation																				|
| **Sink**		 | 																													|
| 		  	  	 |Any database operation													|						
| **Sanitizer**	 |				                                  														      		|
|                |Class-level with sharing or inherited sharing annotation|

#### Interpreting UseWithSharingOnDatabaseOperation Results

*Common Case: Error Message*

> Database operation must be executed from a class that enforces sharing rules.

Explanation:

The database operation occurs in a `without sharing` context, either because it occurs in a class annotated `without sharing` or because its class inherits sharing from a `without sharing` class. To resolve this violation, add `with sharing` or `inherited sharing` to the class.

*Common Case: Warning Message*

> The database operation’s class implicitly inherits a sharing model from %s %s. Explicitly assign a sharing model instead.

Explanation:

This warning is thrown when a database operation occurs in a class that has no explicitly declared sharing model, and therefore it implicitly inherits `with sharing` from its calling class. Even though the operation is secure in this specific case, it isn’t secure by default. Explicitly assign this class a sharing model to make it secure by default.

## Roadmap	

We’re working on adding more rules. In the meantime, give us your [feedback](https://www.research.net/r/SalesforceCA).	

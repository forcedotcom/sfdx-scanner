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
|				 |Any method specifically targeted during invocation																|
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

Have a look at related [FAQ](./en/v3.x/faq/#questions-about-interpreting-apexflsviolationrule-results) to understand the results generated.

#### See Also

- [Enforce Security With the stripInaccessible Method](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)
- [Enforcing Object and Field Permissions](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_perms_enforcing.htm)
- [Filter SOQL Queries Using WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm)
- [Frequently Asked Questions](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/faq/)

## Roadmap

We’re working on adding more rules. In the meantime, give us your [feedback](https://www.research.net/r/SalesforceCA).

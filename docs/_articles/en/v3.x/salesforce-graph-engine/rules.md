---
title: 'Rules'
lang: en
---

## ApexFlsViolationRule
ApexFlsViolationRule detects [Create, Read, Update, and Delete (CRUD) and Field-Level Security (FLS) violations](https://www.youtube.com/watch?v=1ZYjpjPTIn8).

The following are its source, sink, and sanitizer definitions:

*Source*

1. `@AuraEnabled`-annotated methods
2. `@InvocableMethod`-annotated methods
3. `@NamespaceAccessible`-annotated methods
4. `@RemoteAction`-annotated methods
5. Any method returning a `PageReference` object
6. `public`-scoped methods on Visualforce Controllers
7. `global`-scoped methods on any class
8. `Messaging.InboundEmailResult handleInboundEmail()` methods on implementations of `Messaging.InboundEmailHandler`
9. Any method specifically targeted during invocation

*Sink*

1. All DML operations and their Database.method() counterparts:
    1. delete
    2. insert
    3. merge
    4. undelete
    5. update
    6. upsert
2. SOQL queries and Database.query counterpart

*Sanitizer*

1. Access check performed using [Schema.DescribeSObjectResult](https://developer.salesforce.com/docs/atlas.en-us.234.0.apexref.meta/apexref/apex_methods_system_sobject_describe.htm)
    1. Acceptable only for operations that require CRUD-level checks such as DELETE, UNDELETE, and MERGE.
2. Access check performed using [Schema.DescribeFieldResult](https://developer.salesforce.com/docs/atlas.en-us.234.0.apexref.meta/apexref/apex_methods_system_fields_describe.htm)
    1. Acceptable for operations that require FLS-level checks. Includes READ, INSERT, UPDATE, UPSERT for Standard data objects and Custom Objects
3. SOQL queries that use [WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm#apex_classes_with_security_enforced)
4. Lists filtered by [Security.stripInaccessible](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)

Have a look at related [FAQ](./en/v3.x/faq/#questions-about-interpreting-apexflsviolationrule-results) to understand the results generated.

#### References

[Enforcing Object and Field Permissions](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_perms_enforcing.htm)

[Enforce Security With the stripInaccessible Method](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_stripInaccessible.htm)

[Filter SOQL Queries Using WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm)

## Roadmap

We are working on adding more rules in the future. In the meantime, please let us know if you have requests or suggestions.

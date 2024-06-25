AvoidSecurityEnforcedOldApiVersion[](#avoidsecurityenforcedoldapiversion)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   WITH SECURITY_ENFORCED is used with API version lower than 48.0


**Priority:** Medium (3)

**Description:**

   Detects use of WITH SECURITY_ENFORCED in API version less than 48.0. More guidance available in the [WITH SECURITY_ENFORCED](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_with_security_enforced.htm#apex_classes_with_security_enforced) documentation.

**Example(s):**

   


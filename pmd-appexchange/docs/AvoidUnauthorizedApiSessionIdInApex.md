AvoidUnauthorizedApiSessionIdInApex[](#avoidunauthorizedapisessionidinapex)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Use of Session Id might not be authorized.


**Priority:** Medium (3)

**Description:**

   Detects use of ${API.Session_Id} to retrieve a session ID. For more guidance on approved use cases, read the [Session Id Guidance][https://partners.salesforce.com/sfc/servlet.shepherd/version/download/0684V00000O83jT?asPdf=false&operationContext=CHATTER] document.

**Example(s):**

   


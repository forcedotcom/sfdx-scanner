ValidateCrudFlsEmailMessageWhoIdWhatId[](#validatecrudflsemailmessagewhoidwhatid)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Enforce CRUD/FLS checks for methods that accept record IDs as an input value.


**Priority:** Medium (3)

**Description:**

   Detects use WhoId and WhatId without a CRUD/FLS check in these methods: setBccAddresses(bccAddresses), setCcAddresses(ccAddresses), or setToAddresses(toAddresses). Also detects the use of recordIds in setTargetObjectId(targetObjectId) or setWhatId(whatId) without CRUD/FLS validation.

**Example(s):**

   


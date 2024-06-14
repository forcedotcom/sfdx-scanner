AvoidGetInstanceWithTaint[](#avoidgetinstancewithtaint)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   getInstance() is invoked with a potentially tainted parameter.


**Priority:** Medium (3)

**Description:**

   Detects use of getInstance(userId)/getInstance(profileId). Hierarchy Custom Settings return the record owned by the current user when `getInstance()` is invoked without any parameters.
But if a tainted/end-user controlled `userId` or `profileId` is passed as a parameter to `getInstance()` that will allow the code to access records owned by other users on the org.
Protected Custom Settings are the recommended approach to store subscriber owned secrets. Passing `userId` or `proileId` parameters to `getInstance()` could allow a user access to secrets that belong other other users on the org.

**Example(s):**

   


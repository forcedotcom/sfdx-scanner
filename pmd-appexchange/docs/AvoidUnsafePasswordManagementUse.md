AvoidUnsafePasswordManagementUse[](#avoidunsafepasswordmanagementuse)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Before invoking password related methods in Apex, perform necessary authorization checks.


**Priority:** Critical (1)

**Description:**

   Detects where System.setPassword() exists in Apex code. Use this method with caution.

**Example(s):**

   


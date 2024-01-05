AvoidUnsafeSystemSetPassword[](#avoidunsafesystemsetpassword)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Before calling System.setPassword() in Apex, perform necessary authorization checks.


**Priority:** Critical (1)

**Description:**

   Detects where System.setPassword() exists in Apex code. Use this method with caution.

**Example(s):**

   


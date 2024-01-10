AvoidUnsafeSystemResetPassword[](#avoidunsafesystemresetpassword)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Before calling System.resetPassword(), perform the necessary authorization checks.


**Priority:** Critical (1)

**Description:**

   Detects where System.resetPassword() exists in Apex code. Use this method with caution.

**Example(s):**

   


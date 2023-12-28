AvoidUnsafeSystemMovePassword[](#avoidunsafesystemmovepassword)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Before calling System.movePassword(), perform the necessary authorization checks.


**Priority:** Critical (1)

**Description:**

   Detects where System.movePassword() is used in Apex code. Use this method with caution.

**Example(s):**

   


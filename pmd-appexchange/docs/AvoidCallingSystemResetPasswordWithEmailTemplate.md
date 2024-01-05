AvoidCallingSystemResetPasswordWithEmailTemplate[](#avoidcallingsystemresetpasswordwithemailtemplate)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Before calling System.resetPasswordWithEmailTemplate(), perform the necessary authorization checks.


**Priority:** Critical (1)

**Description:**

   Detects where System.resetPasswordWithEmailTemplate() exists in Apex code. Use this method with caution.

**Example(s):**

   


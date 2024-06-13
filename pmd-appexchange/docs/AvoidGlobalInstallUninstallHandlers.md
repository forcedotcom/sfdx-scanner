AvoidGlobalInstallUninstallHandlers[](#avoidglobalinstalluninstallhandlers)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Install and Uninstall handlers should be public and not global


**Priority:** Critical (1)

**Description:**

   Detects Install and Uninstall handlers declared as global. Install and Uninstall Handlers don't need to be global classes. 
Using `global` for these handlers means global methods in these classes act as controllers and can be invoked by untrusted code outside the context of post-install/uninstall scenarios.
Depending on the logic in these handlers, there could potentially unintended consequences. 
For ex: Sometimes post install handlers are used to generate an encryption key to be stored in a protected custom settings object. But if the classes are global, then other untrusted code in the org can invoke the global method and the encryption key may be over-written.
Or
Helper classes for post-install handlers are recommended to be used "without sharing" - which is acceptable in the context of post-install exectution; but could lead to potential security concerns if "without sharing" classes are invoked by untrusted code.

**Example(s):**

   


UseLwcDomManual[](#uselwcdommanual)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Protect against XSS when using lwc:dom="manual".


**Priority:** Medium (3)

**Description:**

   Detects instances of lwc:dom="manual" that could allow unintentional or malicious user input. Don't allow user input on these elements.

**Example(s):**

   

```
<template>
        <div class="chart slds-var-m-around_medium slds-theme_default cursor" lwc:dom="manual"></div> 
    </template>

    & 
    If template has a direct user input. Then XSS is possible.
```



        


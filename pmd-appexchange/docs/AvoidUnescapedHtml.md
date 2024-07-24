AvoidUnescapedHtml[](#avoidunescapedhtml)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Potential XSS due to the use of unesapedHtml


**Priority:** High (2)

**Description:**

Detected use of aura:unescapedHtml. This should be used cautiously. Developers should ensure that the unescapedHtml should not use tainted input to protect against XSS

**Example(s):**

   


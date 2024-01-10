AvoidLwcBubblesComposedTrue[](#avoidlwcbubblescomposedtrue)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Avoid setting both Lightning Web component bubbles and composed=true at the same time.


**Priority:** Medium (3)

**Description:**

   Detects Lightning Web Component event configurations where bubbles and composed are both set to true. To avoid sharing sensitive information unintentionally, use this configuration with caution.

**Example(s):**

   


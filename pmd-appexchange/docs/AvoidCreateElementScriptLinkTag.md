AvoidCreateElementScriptLinkTag[](#avoidcreateelementscriptlinktag)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Load JavaScript/CSS only from static resources.


**Priority:** High (2)

**Description:**

   Detects dynamic creation of script or link tags

**Example(s):**

   

```
<script src="{!$Resource.jquery}"/>
```

See more examples on properly using static resources here: https://developer.salesforce.com/docs/atlas.en-us.236.0.secure_coding_guide.meta/secure_coding_guide/secure_coding_cross_site_scripting.htm

        


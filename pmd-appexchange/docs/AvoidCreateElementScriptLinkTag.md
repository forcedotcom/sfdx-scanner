AvoidCreateElementScriptLinkTag[](#avoidcreateelementscriptlinktag)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Load JavaScript/CSS only from static resources.


**Priority:** High (2)

**Description:**

Detects dynamic creation of script or link tags
Note: This rule identifies the `<script>` block where `createElement` is detected; but can only show the line number where the `<script>` tag begins and not the line number for `createElement`.
That means if there are multiple `createElement` calls with `script` as input, you'll see multiple issues reported with the line number of the `<script>` tag. This is a known issue; developers are expected to go through the `<script>` block to identify the use of `createElement`

**Example(s):**

   

```
<script src="{!$Resource.jquery}"/>
```

See more examples on properly using static resources here: https://developer.salesforce.com/docs/atlas.en-us.236.0.secure_coding_guide.meta/secure_coding_guide/secure_coding_cross_site_scripting.htm

        


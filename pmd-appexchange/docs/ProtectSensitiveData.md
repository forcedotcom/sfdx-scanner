ProtectSensitiveData[](#protectsensitivedata)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   To store secrets, use Protected Custom settings or Protected Custom metadata.


**Priority:** Medium (3)

**Description:**

   Detects where sensitive data must be stored with Protected Custom metadata or Protected Custom settings.

**Example(s):**

   

```
<?xml version="1.0" encoding="UTF-8"?>
<CustomField xmlns="http://soap.sforce.com/2006/04/metadata">
    <fullName>API_Key__c</fullName>
    <externalId>false</externalId>
    <label>API Key</label>
    <length>64</length>
    <required>true</required>
    <trackTrending>false</trackTrending>
    <type>Text</type>
    <unique>false</unique>
</CustomField>

	Or


<?xml version="1.0" encoding="UTF-8"?>
<CustomField xmlns="http://soap.sforce.com/2006/04/metadata">
    <fullName>Scientific_Name__c</fullName>
    <externalId>false</externalId>
    <label>Social Security Number</label>
    <length>128</length>
    <required>true</required>
    <trackTrending>false</trackTrending>
    <type>Text</type>
    <unique>false</unique>
</CustomField>
```



        


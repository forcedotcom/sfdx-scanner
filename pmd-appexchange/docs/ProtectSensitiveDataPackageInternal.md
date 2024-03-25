ProtectSensitiveDataPackageInternal[](#protectsensitivedatapackageinternal)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   To store secrets, use Protected Custom settings or Protected Custom metadata.


**Priority:** Medium (3)

**Description:**

   Detects where sensitive data must be stored with Protected Custom metadata or Protected Custom settings.

**Example(s):**

   

```
<?xml version="1.0" encoding="UTF-8"?>
    <CustomObject xmlns="http://soap.sforce.com/2006/04/metadata">
        <customSettingsType>Hierarchy</customSettingsType>
        <description>Settings for the Dummy package.</description>
        <enableFeeds>false</enableFeeds>
        <fields>
            <fullName>Key__c</fullName>
            <deprecated>false</deprecated>
            <description>User's Key for Applicatio</description>
            <externalId>false</externalId>
            <label>Sensitve Key</label>
            <precision>3</precision>
            <required>true</required>
            <scale>0</scale>
            <type>Number</type>
            <unique>false</unique>
        </fields>
        <label>DummyApp</label>
        <visibility>Protected</visibility>
    </CustomObject>
```



        


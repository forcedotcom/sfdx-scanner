AvoidHardcodedCredentials[](#avoidhardcodedcredentials)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Remove hard-coded credentials from source code.


**Priority:** Medium (3)

**Description:**

   Identifies hard-coded credentials in source code that must be protected using Protected Custom metadata or Protected Custom settings.

**Example(s):**

   Correct Method

```
<?xml version="1.0" encoding="UTF-8"?>
            <CustomObject xmlns="http://soap.sforce.com/2006/04/metadata">
               <customSettingsType>List</customSettingsType>
                  <enableFeeds>false</enableFeeds>
                  <label>Username</label>
                  <visibility>Protected</visibility>
               </CustomObject>
```

Incorrect Method

```
public with sharing class test3 {
         public test3() {
         String key = 'supersecurepassword';
         HttpRequest req = new HttpRequest();
         req.setEndpoint('https://www.example.com/test?APIKEY='+key);
         req.setMethod('GET');
         Http http = new Http();
         HTTPResponse res = http.send(req);
         return res.getBody();
      }
```



        


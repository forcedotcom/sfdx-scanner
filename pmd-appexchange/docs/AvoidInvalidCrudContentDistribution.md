AvoidInvalidCrudContentDistribution[](#avoidinvalidcrudcontentdistribution)
------------------------------------------------------------------------------------------------------------------------------------------------------

**Violation:**

   Do not use Schema.DescribeSObjectResult methods to enforce CRUD check on ContentDistribution


**Priority:** Medium (3)

**Description:**

   
Detects the use of `Schema.DescribeSObjectResult` methods to enforce CRUD check on `ContentDistribution`.
Developers should use `USER MODE` operations or use the custom below to enforce CRUD check against the `ContentDistribution` object.

```
 Boolean userCanCreatePublicLinks = 0 <
        [SELECT COUNT() FROM PermissionSetAssignment
        WHERE PermissionSet.PermissionsDistributeFromPersWksp = TRUE
        AND AssigneeId = :UserInfo.getUserId()];
if(userCanCreatePublicLinks){
//had CRUD permissions 
}
else{
//handle error
}
```

**Example(s):**

   


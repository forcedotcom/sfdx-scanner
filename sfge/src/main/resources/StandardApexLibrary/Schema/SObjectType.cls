global class SObjectType {
	global Schema.DescribeSObjectResult getDescribe() { }
	global SObject newSObject(Id recordTypeId, Boolean loadDefaultValues) { }
	global SObject newSObject(Id id) { }
	global SObject newSObject() { }

}
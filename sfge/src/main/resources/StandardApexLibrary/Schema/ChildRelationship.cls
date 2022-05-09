global class ChildRelationship {
	global Boolean cascadedelete;
	global Schema.SObjectType childsobject;
	global Boolean deprecatedandhidden;
	global Schema.SObjectField field;
	global List<String> junctionidlistnames;
	global List<Schema.SObjectType> junctionreferenceto;
	global String relationshipname;
	global Boolean restricteddelete;
	global Schema.SObjectType getChildSObject() { }
	global Schema.SObjectField getField() { }
	global List<String> getJunctionIdListNames() { }
	global List<Schema.SObjectType> getJunctionReferenceTo() { }
	global String getRelationshipName() { }
	global Boolean isCascadeDelete() { }
	global Boolean isDeprecatedAndHidden() { }
	global Boolean isRestrictedDelete() { }

}
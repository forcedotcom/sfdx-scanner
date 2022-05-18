public class MyClass {
    @AuraEnabled
    public static void foo(List<My_Custom_Set__c> toInsert) {
    	if (My_Custom_Set__c.SObjectType.getDescribe().isCreateable()) {
    		insert toInsert;
    	}
    }
}

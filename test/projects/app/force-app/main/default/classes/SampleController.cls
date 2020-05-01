public with sharing class SampleController {
    
    @AuraEnabled
    public static List<SimpleAccount> getSimpleAccounts() {
		//tests returning data structure defined in apex class
        // Perform isAccessible() check here
		
        // SimpleAccount is a simple "wrapper" Apex class for transport
        List<SimpleAccount> simpleAccounts = new List<SimpleAccount>();

        List<Account> accounts = [SELECT Id, Name, Phone FROM Account LIMIT 5];
        for (Account acct : accounts) {
            simpleAccounts.add(new SimpleAccount(acct.Id, acct.Name, acct.Phone));
        }
        
        return simpleAccounts;
    }
    
    //test returning a list of custom objects to the client
    @auraenabled
    public static List<testObj__c> getObjs() {
        List<testObj__c> objs = [SELECT Name, checkbox__c, datetime__c, richtext__c FROM testObj__c LIMIT 5];
        return objs;
    }
    
    //test returning a list standard objects to the client
    @auraenabled
    public static List<Account> getAccts() {
        List<Account> objs = [SELECT Name, ID, CreatedDate FROM Account LIMIT 5];
        return objs;
    }
    
	    
}
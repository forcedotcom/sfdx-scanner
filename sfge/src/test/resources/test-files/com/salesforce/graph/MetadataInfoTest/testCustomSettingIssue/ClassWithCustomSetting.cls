public class ClassWithCustomSetting {
	public static void doSomething(String s, String sObjectType) {
		MyCustomSetting__c customSettings = MyCustomSetting__c.getOrgDefaults();
	}
}

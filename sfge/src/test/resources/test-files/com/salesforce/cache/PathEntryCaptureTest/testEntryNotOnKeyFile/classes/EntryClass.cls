public class EntryClass {
	@AuraEnabled
	public static void doSomething() {
		Integer int = Class2.getMyInt();
	}
}

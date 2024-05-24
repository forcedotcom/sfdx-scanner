public class EntryClass {
	@AuraEnabled
	public static void doSomething() {
		KeyClass key = new KeyClass();
		Integer int = key.getMyInt();
		System.debug(int);
	}
}

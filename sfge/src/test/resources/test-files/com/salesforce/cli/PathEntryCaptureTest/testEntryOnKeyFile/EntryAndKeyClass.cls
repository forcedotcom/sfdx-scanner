public class EntryAndKeyClass {
	@AuraEnabled
	public static void doSomething() {
		Integer int = getMyInt();
	}

	static Integer getMyInt() {
		return 5;
	}
}

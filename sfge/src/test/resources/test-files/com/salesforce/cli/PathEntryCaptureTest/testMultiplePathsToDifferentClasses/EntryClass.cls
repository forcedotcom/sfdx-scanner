public class EntryClass {
	@AuraEnabled
	public static void doSomething(boolean decider) {
		if (decider) {
			KeyClass1 key1 = new KeyClass1();
        	Integer int = key1.getMyInt();
        	System.debug(int);
		} else {
			KeyClass2 key2 = new KeyClass2();
            String str = key2.getMyString();
            System.debug(str);
		}
	}
}

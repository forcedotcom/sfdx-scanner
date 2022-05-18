public class MyClass3 {
	public static void doSomething(String s, String sObjectType) {
		Boolean isSearchable = Schema.getGlobalDescribe().get(objectName).getDescribe().isSearchable();
		System.debug(isSearchable);
	}
}

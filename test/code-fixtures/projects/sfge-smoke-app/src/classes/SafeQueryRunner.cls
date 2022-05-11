public class SafeQueryRunner {
	public PageReference safeQuery() {
		if (Schema.SObjectType.Account.fields.Name.isAccessible() && Schema.SObjectType.Account.fields.Phone.isAccessible()) {
			Account[] accs = [SELECT Name, Phone FROM Account];
		}
		return null;
	}
}

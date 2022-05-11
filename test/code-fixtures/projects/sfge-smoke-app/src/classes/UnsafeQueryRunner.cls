public class UnsafeQueryRunner {
	public PageReference unsafeQuery() {
		Account[] accs = [SELECT Name, Phone FROM Account];
		return null;
	}
}

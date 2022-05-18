public class UnsafeVfController {
	public String getValueInsecurely() {
		Account[] accs = [SELECT Name FROM Account];
		return accs.get(0).Name;
	}

	global String getAnotherValueInsecurely() {
		Account[] accs = [SELECT Phone FROM Account];
		return accs.get(0).Phone;
	}
}

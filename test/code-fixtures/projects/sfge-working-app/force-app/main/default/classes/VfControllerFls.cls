public class VfControllerFls {
	public String flsHelperGivenIncorrectObjectType() {
		// Note that the helper object is being created on the 'Contact' object type.
		FlsHelperClass helper = new FlsHelperClass('Contact');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};

		for (String fieldToCheck : fieldsToCheck) {
			helper.verifyReadable(fieldToCheck);
		}
		// Since no CRUD/FLS was performed on the Account object, this will throw a violation.
		Account[] accs = [SELECT Name FROM Account];
		return accs.get(0).Name;
	}

	global String flsNoEnforcementAttempted() {
		Account[] accs = [SELECT Phone FROM Account];
		// No CRUD/FLS was even attempted, so this line throws a violation.
		return accs.get(0).Phone;
	}

	public String flsWrongPermissionChecked() {
		FlsHelperClass helper = new FlsHelperClass('Account');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};

		for (String fieldToCheck : fieldsToCheck) {
			helper.verifyCreateable(fieldToCheck);
		}
		// All of the CRUD/FLS checks used `verifyCreateable()` instead of `verifyReadable()`, so this READ operation
		// will still throw a violation.
		Account[] accs = [SELECT Name, Phone FROM Account];
		return accs.get(0).Phone;
	}

	public String flsDoneCorrectly() {
		FlsHelperClass helper = new FlsHelperClass('Account');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};

		for (String fieldToCheck : fieldsToCheck) {
			helper.verifyReadable(fieldToCheck);
		}

		// Because we performed CRUD/FLS checks against all of the necessary fields, this line will succeed and no
		// violation will be thrown.
		Account[] accs = [SELECT Phone FROM Account];
		return accs.get(0).Phone;
	}

	private String flsInPrivateMethod() {
		// Since this is a private method that isn't called by any other methods, the analyzer doesn't treat it as a
		// possible entrypoint, and no violation is thrown.
		Account[] accs = [SELECT Name, Phone FROM Account];
		return accs.get(0).Phone;
	}
}

public with sharing class AuraEnabledFls {

	@AuraEnabled
	public static Account flsHelperGivenIncorrectObjectType() {
		// Note that the helper object is being created on the 'Contact' object type.
		FlsHelperClass helper = new FlsHelperClass('Contact');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};

		for (String fieldToCheck : fieldsToCheck) {
			helper.verifyCreateable(fieldToCheck);
		}

		Account a = new Account(Name = 'Acme Inc.', Phone = '312-555-0123');
		// The object we're inserting is an Account. So the CRUD/FLS checks we did earlier (which were all against the
		// Contact object) are insufficient. Hence, a violation will be thrown on this line.
		insert a;
		return a;
	}

	@AuraEnabled
	public static Account flsHelperMultipleInstances(String name, String phone, String contactLastName) {
		FlsHelperClass accountHelper = new FlsHelperClass('Account');
		FlsHelperClass contactHelper = new FlsHelperClass('Contact');

		String[] accountFieldsToCheck = new String[]{'Name', 'Phone'};
		for (String fieldToCheck : accountFieldsToCheck) {
			accountHelper.verifyCreateable(fieldToCheck);
		}

		String[] contactFieldsToCheck = new String[]{'Account', 'LastName'};
		for (String fieldToCheck : contactFieldsToCheck) {
			// The account helper is being mistakenly invoked here instead of the contact helper. So the Contact CRUD/FLS
			// is not being verified.
			accountHelper.verifyCreateable(fieldToCheck);
		}

		Account a = new Account();
		a.Name = name;
		a.Phone = phone;
		insert a;

		Contact c = new Contact();
		c.Account = a;
		c.LastName = contactLastName;
		// Since Contact CRUD/FLS was never actually checked, this line will throw a violation.
		insert c;

		return a;
	}

	@AuraEnabled
	public static Account flsNoEnforcementAttempted(String name, String phone) {
		Account a = new Account(Name = name, Phone = phone);
		// No CRUD/FLS enforcement was even attempted, so this line will throw a violation.
		insert a;
		return a;
	}

	@AuraEnabled
	public static Account flsInIfBranchOnly(Boolean enterIfBranch) {
		FlsHelperClass helper = new FlsHelperClass('Account');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};
		if (enterIfBranch) {
			for (String fieldToCheck : fieldsToCheck) {
				helper.verifyCreateable(fieldToCheck);
			}
		}

		Account a = new Account(Name = 'Acme Inc.', Phone = '312-555-0123');
		// Because we performed CRUD/FLS checks in only one arm of the if-else branch, this line will throw a violation.
		insert a;
		return a;
	}

	@AuraEnabled
	public static Account flsDoneCorrectly(String name, String phone) {
		FlsHelperClass helper = new FlsHelperClass('Account');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};

		for (String fieldToCheck : fieldsToCheck) {
			helper.verifyCreateable(fieldToCheck);
		}

		Account a = new Account(Name = 'Acme Inc.', Phone = '312-555-0123');
		// Because we performed CRUD/FLS checks against all of the necessary fields, this line will succeed and no
		// violation will be thrown.
		insert a;
		return a;
	}

	public static Account flsInNonAuraMethod(String name, String phone) {
		Account a = new Account(Name = name, Phone = phone);
		// This method is not aura-enabled, nor is it called by any other aura-enabled methods, so the analyzer does not
		// consider it a valid entrypoint. Therefore no violation will be thrown, even though this method is not itself secure.
		insert a;
		return a;
	}
}

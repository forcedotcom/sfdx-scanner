public with sharing class PageReferenceFls {
	public PageReference flsHelperGivenIncorrectObjectType() {
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
		return null;
	}

	public PageReference flsDoneCorrectly() {
		FlsHelperClass helper = new FlsHelperClass('Account');
		String[] fieldsToCheck = new String[]{'Name', 'Phone'};

		for (String fieldToCheck : fieldsToCheck) {
			helper.verifyCreateable(fieldToCheck);
		}

		Account a = new Account(Name = 'Acme Inc.', Phone = '312-555-0123');
		// Because we performed CRUD/FLS checks against all of the necessary fields, this line will succeed and no
		// violation will be thrown.
		insert a;
		return null;
	}

	public String flsInNonPageReferenceMethod() {
		Account a = new Account(Name = name, Phone = phone);
		// This method doesn't return a PageReference, so the analyzer does not consider it a valid entrypoint.
		// Therefore no violation will be thrown, even though this method is not itself secure.
		insert a;
		return a;
	}
}

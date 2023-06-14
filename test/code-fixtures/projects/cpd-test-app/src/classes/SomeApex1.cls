public class SomeApex1 {

    public static void setName(String name, String id) {
        Id obj_id = (Id)id;

        if (hasPerm()) {
            Account acc = [SELECT Name, Id FROM Account WHERE Id=:obj_id][0];
            acc.Name = name;
            update acc;
        }

    }

	private boolean hasPerm() {
		return Account.SObjectType.getDescribe().isUpdateable()
            && Schema.SObjectType.Account.fields.Name.isUpdateable();
	}

	private void anotherMethod() {
		System.debug('This extra method is not duplicated');
	}
}

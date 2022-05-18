public class FlsHelperClass {
	private final Map<String, Schema.SObjectField> objectFields;

	public FlsHelperClass(String objectName) {
		this.objectFields = Schema.getGlobalDescribe()
			.get(objectName)
			.getDescribe().fields.getMap();
	}

	public void verifyCreateable(String fieldName) {
		verifyCreateableNegativeStyle(fieldName);
	}

	private void verifyCreateableNegativeStyle(String fieldName) {
		if (!objectFields.get(fieldName).getDescribe().isCreateable()) {
			throw new PermissionsException();
		}
	}

	private void verifyCreateablePositiveStyle(String fieldName) {
		if (objectFields.get(fieldName).getDescribe().isCreateable()) {
			return;
		}
		throw new PermissionsException();
	}

	public void verifyReadable(String fieldName) {
		if (!objectFields.get(fieldName).getDescribe().isAccessible()) {
			throw new PermissionsException();
		}
	}
}

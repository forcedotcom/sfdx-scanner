/**
* Tests for TwinFieldMapper
* @author ddvinov
* @since 228
*/
@IsTest
public class TwinFieldMapperTests {

	static testMethod void testShouldCopyMappedFieldValueForUnknownField() {
		helpTestShouldCopyMappedFieldValue(Boolean.valueOf('false'), 'randomField');
	}

	static testMethod void testShouldCopyMappedFieldValueForNonBooleanField() {
		helpTestShouldCopyMappedFieldValue(Boolean.valueOf('false'), 'Name');
	}

	static testMethod void testShouldCopyMappedFieldValueForCheckboxField() {
		helpTestShouldCopyMappedFieldValue(Boolean.valueOf('true'), 'LineItemsGrouped__c');
	}

	private static void helpTestShouldCopyMappedFieldValue(Boolean shouldCopy, String fieldName) {
		System.assertEquals(shouldCopy, new TwinFieldMapper(Quote__c.SObjectType, Order.SObjectType, true).
			shouldCopyMappedFieldValue(new Quote__c(), fieldName, Boolean.valueOf('true')));
	}
}

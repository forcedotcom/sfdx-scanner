package com.salesforce.rules.fls.apex.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import com.salesforce.collections.CollectionUtil;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class FlsViolationUtilsTest {
    private static final FlsConstants.FlsValidationType VALIDATION_TYPE =
            FlsConstants.FlsValidationType.UPDATE;
    private static final String OBJECT_NAME = "My_Obj__c";
    private static final TreeSet<String> EMPTY_FIELD_LIST = CollectionUtil.newTreeSet();

    @Test
    public void testMessageWithNoFields() {
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(
                                VALIDATION_TYPE, OBJECT_NAME, EMPTY_FIELD_LIST, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [Unknown]"));
    }

    @Test
    public void testMessageWithAllFields() {
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, EMPTY_FIELD_LIST, true));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [ALL_FIELDS]"));
    }

    @Test
    public void testMessageWithSelectedFields() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("Name", "Status__c");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [Name,Status__c]"));
    }

    @Test
    public void testMessageWithSelectedFieldsAndAllFieldsFlag() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("Name", "Status__c");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, true));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [ALL_FIELDS]"));
    }

    @Test
    public void testMessageWithRelationalField() {
        final TreeSet<String> fieldList =
                CollectionUtil.newTreeSetOf(
                        "Name", "Status__c", "Relational_Field__r.Another_field__c");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [Name,Status__c] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [Relational_Field__r.Another_field__c]"));
    }

    @Test
    public void testMessageWithRelationalObject() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("Name", "Status__c");
        final String relationalObjectName = "My_Relational_Obj__r";
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(
                                VALIDATION_TYPE, relationalObjectName, fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Relational_Obj__r] with field(s) [Name,Status__c] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [My_Relational_Obj__r]"));
    }

    @Test
    public void testMessageWithIllegibleField() {
        final TreeSet<String> fieldList =
                CollectionUtil.newTreeSetOf("Name", "Status__c", "{1}{2}{3}");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [Name,Status__c] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [{1}{2}{3}]"));
    }

    @Test
    public void testMessageWithIllegibleFieldWithAllFields() {
        final TreeSet<String> fieldList =
                CollectionUtil.newTreeSetOf("Name", "Status__c", "{1}{2}{3}");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, true));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [ALL_FIELDS] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [{1}{2}{3}]"));
    }

    @Test
    public void testMessageWithOnlyIllegibleFields() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("{1}{2}{3}");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [{1}{2}{3}]"));
    }

    @Test
    public void testMessageWithOnlyIllegibleFieldsWithAllFields() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("{1}{2}{3}");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, OBJECT_NAME, fieldList, true));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [My_Obj__c] with field(s) [ALL_FIELDS] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [{1}{2}{3}]"));
    }

    @Test
    public void testMessageWithIllegibleObject() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("Name", "Status__c");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(VALIDATION_TYPE, "{1}", fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [{1}] with field(s) [Name,Status__c] - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: [{1}]"));
    }

    @Test
    public void testMessageWithValidCustomObject() {
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("Name", "Status__c");
        final String message =
                FlsViolationUtils.constructMessageInternal(
                        new FlsViolationInfo(
                                VALIDATION_TYPE, "namespace__Random_object__c", fieldList, false));

        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [UPDATE] operation on [namespace__Random_object__c] with field(s) [Name,Status__c]"));
    }

    @Test
    public void testFlsMessageIsIndependentOfFields() {
        // No fields in list
        final TreeSet<String> fieldList = CollectionUtil.newTreeSet();
        // Object is known to handle FLS
        final String objectName = "Account";
        // Validation type is at field-level
        final FlsConstants.FlsValidationType validationType = FlsConstants.FlsValidationType.INSERT;

        FlsViolationInfo violationInfo =
                new FlsViolationInfo(validationType, objectName, fieldList, false);

        final String message = FlsViolationUtils.constructMessage(violationInfo);
        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [INSERT] operation on [Account] with field(s) [UNKNOWN]"));
    }

    @Test
    public void testFlsMessageIsIndependentOfFields_allFields() {
        // No fields in list
        final TreeSet<String> fieldList = EMPTY_FIELD_LIST;
        // With all fields
        final boolean allFields = true;
        // Object is known to handle FLS
        final String objectName = "Account";
        // Validation type is at field-level
        final FlsConstants.FlsValidationType validationType = FlsConstants.FlsValidationType.INSERT;

        FlsViolationInfo violationInfo =
                new FlsViolationInfo(validationType, objectName, fieldList, allFields);

        final String message = FlsViolationUtils.constructMessage(violationInfo);
        assertThat(
                message,
                equalToIgnoringCase(
                        "FLS validation is missing for [INSERT] operation on [Account] with field(s) [ALL_FIELDS]"));
    }

    @Test
    public void testCrudMessageIsIndependentOfFields() {
        // No fields in list
        final TreeSet<String> fieldList = CollectionUtil.newTreeSetOf("Name");
        // Object is known to handle FLS
        final String objectName = "Account";
        // Validation type is at object-level
        final FlsConstants.FlsValidationType validationType = FlsConstants.FlsValidationType.DELETE;

        FlsViolationInfo violationInfo =
                new FlsViolationInfo(validationType, objectName, fieldList, false);

        final String message = FlsViolationUtils.constructMessage(violationInfo);
        assertThat(
                message,
                equalToIgnoringCase(
                        "CRUD validation is missing for [DELETE] operation on [Account]"));
    }

    @Test
    public void testCrudMessageIsIndependentOfFields_allFields() {
        // No fields in list
        final TreeSet<String> fieldList = EMPTY_FIELD_LIST;
        // Include all fields
        final boolean allFields = true;
        // Object is known to handle FLS
        final String objectName = "Account";
        // Validation type is at object-level
        final FlsConstants.FlsValidationType validationType = FlsConstants.FlsValidationType.DELETE;

        FlsViolationInfo violationInfo =
                new FlsViolationInfo(validationType, objectName, fieldList, allFields);

        final String message = FlsViolationUtils.constructMessage(violationInfo);
        assertThat(
                message,
                equalToIgnoringCase(
                        "CRUD validation is missing for [DELETE] operation on [Account]"));
    }
}

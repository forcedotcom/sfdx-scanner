package com.salesforce.rules.fls.apex.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlsValidationRepresentationTest {

    static final TreeSet<String> EMPTY_FIELDS = CollectionUtil.newTreeSet();
    private final FlsValidationRepresentation.Info validationInfoWithMixedCase =
            new FlsValidationRepresentation.Info("Abc", "My_Field__c", FlsValidationType.READ);
    private final FlsValidationRepresentation.Info validationInfoWithLowerCase =
            new FlsValidationRepresentation.Info("abc", "my_field__c", FlsValidationType.READ);

    @Test
    public void testHashcodeForCaseInsensitivity() {
        assertEquals(
                validationInfoWithMixedCase.hashCode(), validationInfoWithLowerCase.hashCode());
    }

    @Test
    public void testEqualsForCaseInsensitivity() {
        assertEquals(validationInfoWithMixedCase, validationInfoWithLowerCase);
    }

    @Test
    public void testEqualityInMultiset() {
        final Multiset<FlsValidationRepresentation.Info> validations = HashMultiset.create();

        validations.add(validationInfoWithLowerCase);

        assertTrue(validations.contains(validationInfoWithMixedCase));
    }

    /** Tests for setting values */
    private static final String ALL_FIELDS = FlsViolationUtils.ALL_FIELDS;

    @Test
    public void testValidationWithFieldsShouldNotAllowObjectLevelType() {
        Assertions.assertThrows(
                UnexpectedException.class,
                () -> {
                    final FlsValidationRepresentation info = new FlsValidationRepresentation();
                    info.addField("Name");
                    info.setValidationType(FlsValidationType.DELETE);
                });
    }

    @Test
    public void testObjectNameCannotBeSetTwice() {
        Assertions.assertThrows(
                UnexpectedException.class,
                () -> {
                    final FlsValidationRepresentation info = new FlsValidationRepresentation();
                    info.setValidationType(FlsValidationType.INSERT);
                    info.setObject("Account");
                    info.setObject("Contact");
                });
    }

    /** Tests for compareWithExistingValidation(...) */
    @Test
    public void testSimpleMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.INSERT);
        existingInfo.setObject("Account");
        existingInfo.addField("Name");

        final Set<FlsViolationInfo> flsViolationInfos =
                existingInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertTrue(
                flsViolationInfos.isEmpty(),
                "No violation message should be generated when match is available");
    }

    @Test
    public void testSimpleMismatch_field() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.INSERT);
        existingInfo.setObject("Account");
        existingInfo.addField("Phone");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertFalse(flsViolationInfos.isEmpty());

        validateFlsViolation(
                flsViolationInfos, "INSERT", "Account", CollectionUtil.newTreeSetOf("Name"));
    }

    @Test
    public void testSimpleMismatch_object() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.INSERT);
        existingInfo.setObject("Contact");
        existingInfo.addField("Name");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertFalse(flsViolationInfos.isEmpty());

        validateFlsViolation(
                flsViolationInfos, "INSERT", "Account", CollectionUtil.newTreeSetOf("Name"));
    }

    @Test
    public void testSimpleMismatch_operation() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.UPDATE);
        existingInfo.setObject("Account");
        existingInfo.addField("Name");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertFalse(flsViolationInfos.isEmpty());

        validateFlsViolation(
                flsViolationInfos, "INSERT", "Account", CollectionUtil.newTreeSetOf("Name"));
    }

    @Test
    public void testMultipleFieldsInExistingButExpectingOnlyOneField_withMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.INSERT);
        existingInfo.setObject("Account");
        existingInfo.addField("Name");
        existingInfo.addField("Phone");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertTrue(
                flsViolationInfos.isEmpty(),
                "No violation message should be generated when required field is validated, even if there are additional fields");
    }

    @Test
    public void testMultipleFieldsInExistingButExpectingOnlyOneField_noMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.INSERT);
        existingInfo.setObject("Account");
        existingInfo.addField("CreatedDate");
        existingInfo.addField("Phone");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertFalse(flsViolationInfos.isEmpty());
        validateFlsViolation(
                flsViolationInfos, "INSERT", "Account", CollectionUtil.newTreeSetOf("Name"));
    }

    @Test
    public void testMultipleFieldsInExistingAcrossMultipleInfo_withMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");
        expectedInfo.addField("Phone");

        final FlsValidationRepresentation existingInfo1 = new FlsValidationRepresentation();
        existingInfo1.setValidationType(FlsValidationType.INSERT);
        existingInfo1.setObject("Account");
        existingInfo1.addField("Name");

        final FlsValidationRepresentation existingInfo2 = new FlsValidationRepresentation();
        existingInfo2.setValidationType(FlsValidationType.INSERT);
        existingInfo2.setObject("Account");
        existingInfo2.addField("Phone");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(expectedInfo, existingInfo1, existingInfo2);

        assertTrue(flsViolationInfos.isEmpty());
    }

    private Set<FlsViolationInfo> compareValidations(
            FlsValidationRepresentation expectedInfo,
            FlsValidationRepresentation existingInfo1,
            FlsValidationRepresentation existingInfo2) {
        final Set<FlsValidationRepresentation.Info> infos = new HashSet<>();
        infos.addAll(existingInfo1.getValidationInfo());
        infos.addAll(existingInfo2.getValidationInfo());
        return expectedInfo.compareWithExistingValidation(infos);
    }

    @Test
    public void testMultipleFieldsInExistingAcrossMultipleInfo_noMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");
        expectedInfo.addField("Phone");

        final FlsValidationRepresentation existingInfo1 = new FlsValidationRepresentation();
        existingInfo1.setValidationType(FlsValidationType.INSERT);
        existingInfo1.setObject("Account");
        existingInfo1.addField("CreatedDate");

        final FlsValidationRepresentation existingInfo2 = new FlsValidationRepresentation();
        existingInfo2.setValidationType(FlsValidationType.INSERT);
        existingInfo2.setObject("Account");
        existingInfo2.addField("ModifiedDate");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(expectedInfo, existingInfo1, existingInfo2);

        assertFalse(flsViolationInfos.isEmpty());
        validateFlsViolation(
                flsViolationInfos,
                "INSERT",
                "Account",
                CollectionUtil.newTreeSetOf("Name", "Phone"));
    }

    @Test
    public void testMultipleFieldsInExistingAcrossMultipleInfo_halfMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");
        expectedInfo.addField("Phone");

        final FlsValidationRepresentation existingInfo1 = new FlsValidationRepresentation();
        existingInfo1.setValidationType(FlsValidationType.INSERT);
        existingInfo1.setObject("Account");
        existingInfo1.addField("CreatedDate");

        final FlsValidationRepresentation existingInfo2 = new FlsValidationRepresentation();
        existingInfo2.setValidationType(FlsValidationType.INSERT);
        existingInfo2.setObject("Account");
        existingInfo2.addField("Phone");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(expectedInfo, existingInfo1, existingInfo2);

        assertFalse(flsViolationInfos.isEmpty());
        validateFlsViolation(
                flsViolationInfos, "INSERT", "Account", CollectionUtil.newTreeSetOf("Name"));
    }

    @Test
    public void testMultipleFieldsInExistingAcrossMultipleInfo_withMatch_andExtraUnmatched() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");
        expectedInfo.addField("Phone");

        final FlsValidationRepresentation existingInfo1 = new FlsValidationRepresentation();
        existingInfo1.setValidationType(FlsValidationType.INSERT);
        existingInfo1.setObject("Account");
        existingInfo1.addField("Name");

        final FlsValidationRepresentation existingInfo2 = new FlsValidationRepresentation();
        existingInfo2.setValidationType(FlsValidationType.INSERT);
        existingInfo2.setObject("Account");
        existingInfo2.addField("Phone");

        final FlsValidationRepresentation existingInfo3 = new FlsValidationRepresentation();
        existingInfo3.setValidationType(FlsValidationType.INSERT);
        existingInfo3.setObject("Contact");
        existingInfo3.addField("Phone");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(expectedInfo, existingInfo1, existingInfo2);
        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testObjectLevelFieldComparison_match() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.DELETE);
        expectedInfo.setObject("Account");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.DELETE);
        existingInfo.setObject("Account");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testObjectLevelFieldComparison_noMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.DELETE);
        expectedInfo.setObject("Account");

        final FlsValidationRepresentation existingInfo = new FlsValidationRepresentation();
        existingInfo.setValidationType(FlsValidationType.DELETE);
        existingInfo.setObject("Contact");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingInfo.getValidationInfo()));

        assertFalse(flsViolationInfos.isEmpty());
        validateFlsViolation(flsViolationInfos, "DELETE", "Account", EMPTY_FIELDS);
    }

    @Test
    public void testFieldObjectCombo_withOnlyFieldMatch_orderMatchFirst() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingMatchingFieldInfo =
                new FlsValidationRepresentation();
        existingMatchingFieldInfo.setValidationType(FlsValidationType.INSERT);
        existingMatchingFieldInfo.setObject("Account");
        existingMatchingFieldInfo.addField("Name");

        final FlsValidationRepresentation existingUnmatchedObjectInfo =
                new FlsValidationRepresentation();
        existingUnmatchedObjectInfo.setValidationType(FlsValidationType.DELETE);
        existingUnmatchedObjectInfo.setObject("Account");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(
                        expectedInfo, existingMatchingFieldInfo, existingUnmatchedObjectInfo);

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testFieldObjectCombo_withOnlyFieldMatch_orderMatchLast() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingMatchingFieldInfo =
                new FlsValidationRepresentation();
        existingMatchingFieldInfo.setValidationType(FlsValidationType.INSERT);
        existingMatchingFieldInfo.setObject("Account");
        existingMatchingFieldInfo.addField("Name");

        final FlsValidationRepresentation existingUnmatchedObjectInfo =
                new FlsValidationRepresentation();
        existingUnmatchedObjectInfo.setValidationType(FlsValidationType.DELETE);
        existingUnmatchedObjectInfo.setObject("Account");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(
                        expectedInfo, existingMatchingFieldInfo, existingUnmatchedObjectInfo);

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testFieldObjectCombo_withOnlyObjectMatch_orderMatchFirst() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.DELETE);
        expectedInfo.setObject("Account");

        final FlsValidationRepresentation existingUnmatchedFieldInfo =
                new FlsValidationRepresentation();
        existingUnmatchedFieldInfo.setValidationType(FlsValidationType.INSERT);
        existingUnmatchedFieldInfo.setObject("Account");
        existingUnmatchedFieldInfo.addField("Name");

        final FlsValidationRepresentation existingMatchingObjectInfo =
                new FlsValidationRepresentation();
        existingMatchingObjectInfo.setValidationType(FlsValidationType.DELETE);
        existingMatchingObjectInfo.setObject("Account");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(
                        expectedInfo, existingUnmatchedFieldInfo, existingMatchingObjectInfo);

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testFieldObjectCombo_withOnlyObjectMatch_orderMatchLast() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.DELETE);
        expectedInfo.setObject("Account");

        final FlsValidationRepresentation existingUnmatchedFieldInfo =
                new FlsValidationRepresentation();
        existingUnmatchedFieldInfo.setValidationType(FlsValidationType.INSERT);
        existingUnmatchedFieldInfo.setObject("Account");
        existingUnmatchedFieldInfo.addField("Name");

        final FlsValidationRepresentation existingMatchingObjectInfo =
                new FlsValidationRepresentation();
        existingMatchingObjectInfo.setValidationType(FlsValidationType.DELETE);
        existingMatchingObjectInfo.setObject("Account");

        final Set<FlsViolationInfo> flsViolationInfos =
                compareValidations(
                        expectedInfo, existingUnmatchedFieldInfo, existingMatchingObjectInfo);

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testFieldObjectCombo_allFields_match() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.setAllFields();

        final FlsValidationRepresentation existingMatched = new FlsValidationRepresentation();
        existingMatched.setValidationType(FlsValidationType.INSERT);
        existingMatched.setObject("Account");
        existingMatched.setAllFields();

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingMatched.getValidationInfo()));

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testFieldObjectCombo_allFieldsOnExisting_match() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.addField("Name");

        final FlsValidationRepresentation existingMatched = new FlsValidationRepresentation();
        existingMatched.setValidationType(FlsValidationType.INSERT);
        existingMatched.setObject("Account");
        existingMatched.setAllFields();

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingMatched.getValidationInfo()));

        assertTrue(flsViolationInfos.isEmpty());
    }

    @Test
    public void testFieldObjectCombo_allFieldsOnExpected_noMatch() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.INSERT);
        expectedInfo.setObject("Account");
        expectedInfo.setAllFields();

        final FlsValidationRepresentation existingMatched = new FlsValidationRepresentation();
        existingMatched.setValidationType(FlsValidationType.INSERT);
        existingMatched.setObject("Account");
        existingMatched.addField("Name");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingMatched.getValidationInfo()));

        assertFalse(flsViolationInfos.isEmpty());
        validateFlsViolation(flsViolationInfos, "INSERT", "Account", EMPTY_FIELDS, true);
    }

    @Test
    public void testFieldObjectCombo_allFields_objectLevelAnalysis() {
        final FlsValidationRepresentation expectedInfo = new FlsValidationRepresentation();
        expectedInfo.setValidationType(FlsValidationType.DELETE);
        expectedInfo.setObject("Account");
        expectedInfo.setAllFields();

        final FlsValidationRepresentation existingMatched = new FlsValidationRepresentation();
        existingMatched.setValidationType(FlsValidationType.DELETE);
        existingMatched.setObject("Account");
        existingMatched.addField("Name");

        final Set<FlsViolationInfo> flsViolationInfos =
                expectedInfo.compareWithExistingValidation(
                        Sets.newHashSet(existingMatched.getValidationInfo()));

        assertTrue(flsViolationInfos.isEmpty());
    }

    private void validateFlsViolation(
            Set<FlsViolationInfo> flsViolationInfos,
            String expectedOperationName,
            String expectedObjectName,
            TreeSet<String> expectedFieldNames) {
        validateFlsViolation(
                flsViolationInfos,
                expectedOperationName,
                expectedObjectName,
                expectedFieldNames,
                false);
    }

    private void validateFlsViolation(
            Set<FlsViolationInfo> flsViolationInfos,
            String expectedOperationName,
            String expectedObjectName,
            TreeSet<String> expectedFieldNames,
            boolean allFields) {
        assertThat(flsViolationInfos, hasSize(1));
        final FlsViolationInfo flsViolationInfo = flsViolationInfos.iterator().next();
        assertThat(
                flsViolationInfo.getValidationType().name(),
                equalToIgnoringCase(expectedOperationName));
        assertThat(flsViolationInfo.getObjectName(), equalToIgnoringCase(expectedObjectName));
        assertThat(flsViolationInfo.getFields(), equalTo(expectedFieldNames));
        assertThat(flsViolationInfo.isAllFields(), equalTo(allFields));
    }
}

package com.salesforce.graph.ops;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.Sets;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.symbols.apex.SoqlQueryInfo;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.testutils.DummyVertex;
import com.salesforce.testutils.ObjectFieldTestHelper;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ObjectFieldUtilTest {

    static final SFVertex DUMMY_VERTEX1 = new DummyVertex("vertex1");
    static final SFVertex DUMMY_VERTEX2 = new DummyVertex("vertex2");

    @Test
    public void testRegrouping_FlsViolationWithDifferentObjects() {
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(
                        new FlsViolationInfo(
                                FlsConstants.FlsValidationType.INSERT,
                                "Account",
                                CollectionUtil.newTreeSetOf("Name"),
                                false),
                        new FlsViolationInfo(
                                FlsConstants.FlsValidationType.INSERT,
                                "Contact",
                                CollectionUtil.newTreeSetOf("FirstName"),
                                false));

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);

        assertThat(regroupedValues, equalTo(inputValues));
    }

    @Test
    public void testRegrouping_FlsViolationWithDifferentValidationType() {
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(
                        new FlsViolationInfo(
                                FlsConstants.FlsValidationType.INSERT,
                                "Account",
                                CollectionUtil.newTreeSetOf("Name"),
                                false),
                        new FlsViolationInfo(
                                FlsConstants.FlsValidationType.UPDATE,
                                "Account",
                                CollectionUtil.newTreeSetOf("Name"),
                                false));

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);

        assertThat(regroupedValues, equalTo(inputValues));
    }

    @Test
    public void testRegrouping_FlsViolationWithDifferentSinkVertices() {
        final FlsViolationInfo violationInfo1 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false);
        violationInfo1.setSinkVertex(DUMMY_VERTEX1);
        final FlsViolationInfo violationInfo2 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Description"),
                        false);
        violationInfo2.setSinkVertex(DUMMY_VERTEX2);
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(violationInfo1, violationInfo2);

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);

        assertThat(regroupedValues, equalTo(inputValues));
    }

    @Test
    public void testRegrouping_FlsViolationWithDifferentSourceVertices() {
        final FlsViolationInfo violationInfo1 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false);
        violationInfo1.setSourceVertex(DUMMY_VERTEX1);
        final FlsViolationInfo violationInfo2 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Description"),
                        false);
        violationInfo2.setSourceVertex(DUMMY_VERTEX2);
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(violationInfo1, violationInfo2);

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);

        assertThat(regroupedValues, equalTo(inputValues));
    }

    @Test
    public void testRegrouping_FlsViolationWithMergeableContents() {
        final FlsViolationInfo violationInfo1 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false);
        violationInfo1.setSinkVertex(DUMMY_VERTEX1);
        violationInfo1.setSourceVertex(DUMMY_VERTEX2);
        final FlsViolationInfo violationInfo2 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Description"),
                        false);
        violationInfo2.setSinkVertex(DUMMY_VERTEX1);
        violationInfo2.setSourceVertex(DUMMY_VERTEX2);
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(violationInfo1, violationInfo2);

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);
        final List<String> fields =
                regroupedValues.stream()
                        .flatMap(v -> v.getFields().stream())
                        .collect(Collectors.toList());
        assertThat(fields, containsInAnyOrder("Name", "Description"));
    }

    @Test
    public void testRegrouping_FlsViolationWithMergeableContents_CaseInsensitive() {
        final FlsViolationInfo violationInfo1 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false);
        violationInfo1.setSinkVertex(DUMMY_VERTEX1);
        final FlsViolationInfo violationInfo2 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "aCCOUNT",
                        CollectionUtil.newTreeSetOf("Description"),
                        false);
        violationInfo2.setSinkVertex(DUMMY_VERTEX1);
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(violationInfo1, violationInfo2);

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);
        final List<String> fields =
                regroupedValues.stream()
                        .flatMap(v -> v.getFields().stream())
                        .collect(Collectors.toList());
        assertThat(fields, containsInAnyOrder("Name", "Description"));
    }

    @Test
    public void testRegrouping_FlsViolationWithMergeableContents_emptyFields() {
        final FlsViolationInfo violationInfo1 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false);
        violationInfo1.setSinkVertex(DUMMY_VERTEX1);
        final FlsViolationInfo violationInfo2 =
                new FlsViolationInfo(
                        FlsConstants.FlsValidationType.INSERT,
                        "Account",
                        CollectionUtil.newTreeSet(),
                        false);
        violationInfo2.setSinkVertex(DUMMY_VERTEX1);
        final HashSet<FlsViolationInfo> inputValues =
                Sets.newHashSet(violationInfo1, violationInfo2);

        final HashSet<FlsViolationInfo> regroupedValues =
                ObjectFieldUtil.regroupByObject(inputValues);
        final List<String> fields =
                regroupedValues.stream()
                        .flatMap(v -> v.getFields().stream())
                        .collect(Collectors.toList());
        assertThat(fields, containsInAnyOrder("Name"));
    }

    @Test
    public void testRegroupSimple_noDuplicates() {
        final HashSet<SoqlQueryInfo> inputQueryInfos = new HashSet<>();
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr1",
                        "Contact",
                        CollectionUtil.newTreeSetOf("FirstName", "LastName"),
                        false,
                        false,
                        false,
                        false,
                        true,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr2",
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));

        final HashSet<SoqlQueryInfo> outputQueryInfos =
                SoqlParserUtil.regroupByObject(inputQueryInfos);
        assertThat(outputQueryInfos, equalTo(inputQueryInfos));
    }

    @Test
    public void testRegroupSimple_withDuplicates() {
        final HashSet<SoqlQueryInfo> inputQueryInfos = new HashSet<>();
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr1",
                        "Contact",
                        CollectionUtil.newTreeSetOf("FirstName", "LastName"),
                        false,
                        false,
                        false,
                        false,
                        true,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr2",
                        "Contact",
                        CollectionUtil.newTreeSetOf("Status__c"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));

        final HashSet<SoqlQueryInfo> outputQueryInfos =
                SoqlParserUtil.regroupByObject(inputQueryInfos);
        assertThat(outputQueryInfos, hasSize(1));
        final SoqlQueryInfo queryInfo = outputQueryInfos.iterator().next();

        assertThat(queryInfo.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(queryInfo.getFields(), containsInAnyOrder("FirstName", "LastName", "Status__c"));
        assertThat(queryInfo.isOutermost(), equalTo(true));
    }

    @Test
    public void testRegroup_withDuplicatesInSingleObject() {
        final HashSet<SoqlQueryInfo> inputQueryInfos = new HashSet<>();
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr1",
                        "Contact",
                        CollectionUtil.newTreeSetOf("FirstName", "LastName"),
                        false,
                        false,
                        false,
                        false,
                        true,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr2",
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr3",
                        "Contact",
                        CollectionUtil.newTreeSetOf("Status__c"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));

        final HashSet<SoqlQueryInfo> outputQueryInfos =
                SoqlParserUtil.regroupByObject(inputQueryInfos);
        assertThat(outputQueryInfos, hasSize(2));
        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(outputQueryInfos);

        final SoqlQueryInfo accountQueryInfo = infoIterator.next();
        assertThat(accountQueryInfo.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(accountQueryInfo.getFields(), containsInAnyOrder("Name"));
        assertThat(accountQueryInfo.isOutermost(), equalTo(false));

        final SoqlQueryInfo contactQueryInfo = infoIterator.next();
        assertThat(contactQueryInfo.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(
                contactQueryInfo.getFields(),
                containsInAnyOrder("FirstName", "LastName", "Status__c"));
        assertThat(contactQueryInfo.isOutermost(), equalTo(true));
    }

    @Test
    public void testRegroup_withMultipleDuplicatesInSingleObject() {
        final HashSet<SoqlQueryInfo> inputQueryInfos = new HashSet<>();
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr1",
                        "Contact",
                        CollectionUtil.newTreeSetOf("FirstName", "LastName"),
                        false,
                        false,
                        false,
                        false,
                        true,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr2",
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr3",
                        "Contact",
                        CollectionUtil.newTreeSetOf("Status__c"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr4",
                        "Contact",
                        CollectionUtil.newTreeSetOf("AnotherField"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));

        final HashSet<SoqlQueryInfo> outputQueryInfos =
                SoqlParserUtil.regroupByObject(inputQueryInfos);
        assertThat(outputQueryInfos, hasSize(2));
        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(outputQueryInfos);

        final SoqlQueryInfo accountQueryInfo = infoIterator.next();
        assertThat(accountQueryInfo.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(accountQueryInfo.getFields(), containsInAnyOrder("Name"));
        assertThat(accountQueryInfo.isOutermost(), equalTo(false));

        final SoqlQueryInfo contactQueryInfo = infoIterator.next();
        assertThat(contactQueryInfo.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(
                contactQueryInfo.getFields(),
                containsInAnyOrder("FirstName", "LastName", "Status__c", "AnotherField"));
        assertThat(contactQueryInfo.isOutermost(), equalTo(true));
    }

    @Test
    public void testRegroup_withMultipleDuplicatesInMultipleObject() {
        final HashSet<SoqlQueryInfo> inputQueryInfos = new HashSet<>();
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr1",
                        "Contact",
                        CollectionUtil.newTreeSetOf("FirstName", "LastName"),
                        false,
                        false,
                        false,
                        false,
                        true,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr2",
                        "Account",
                        CollectionUtil.newTreeSetOf("Name"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr3",
                        "Contact",
                        CollectionUtil.newTreeSetOf("Status__c"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr4",
                        "Contact",
                        CollectionUtil.newTreeSetOf("AnotherField"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));
        inputQueryInfos.add(
                new SoqlQueryInfo(
                        "queryStr5",
                        "Account",
                        CollectionUtil.newTreeSetOf("Description"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false));

        final HashSet<SoqlQueryInfo> outputQueryInfos =
                SoqlParserUtil.regroupByObject(inputQueryInfos);
        assertThat(outputQueryInfos, hasSize(2));
        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(outputQueryInfos);

        final SoqlQueryInfo accountQueryInfo = infoIterator.next();
        assertThat(accountQueryInfo.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(accountQueryInfo.getFields(), containsInAnyOrder("Name", "Description"));
        assertThat(accountQueryInfo.isOutermost(), equalTo(false));

        final SoqlQueryInfo contactQueryInfo = infoIterator.next();
        assertThat(contactQueryInfo.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(
                contactQueryInfo.getFields(),
                containsInAnyOrder("FirstName", "LastName", "Status__c", "AnotherField"));
        assertThat(contactQueryInfo.isOutermost(), equalTo(true));
    }
}

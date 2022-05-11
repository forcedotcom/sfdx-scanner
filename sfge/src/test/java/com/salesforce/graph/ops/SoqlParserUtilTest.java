package com.salesforce.graph.ops;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.graph.symbols.apex.SoqlQueryInfo;
import com.salesforce.testutils.ObjectFieldTestHelper;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class SoqlParserUtilTest {

    @Test
    public void testSimple() {
        String query = "SELECT FirstName FROM Contact";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);
        assertThat(actualQueryInfo.getObjectName(), equalToIgnoringCase("contact"));
        assertThat(actualQueryInfo.getFields(), contains("FirstName"));
        assertThat(actualQueryInfo.isSecurityEnforced(), equalTo(false));
        assertThat(actualQueryInfo.isOutermost(), equalTo(true));
        assertThat(actualQueryInfo.isAllFields(), equalTo(false));
        assertThat(actualQueryInfo.isCount(), equalTo(false));
        assertThat(actualQueryInfo.isLimit1(), equalTo(false));
    }

    @Test
    public void testIsKnownSingleItem() {
        String query = "SELECT FirstName FROM Contact limit 1";
        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);
        assertThat(actualQueryInfo.isLimit1(), equalTo(true));
    }

    @Test
    public void testSimpleWithWhereClause() {
        String query = "SELECT FirstName from Contact where Id='abcd'";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), containsInAnyOrder("Id", "FirstName"));
    }

    @Test
    public void testSelectAllFields() {
        String query = "SELECT fields(all) from Contact";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), Matchers.empty());
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectAllCustomFields() {
        String query = "SELECT fields(Custom) from Contact";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), Matchers.empty());
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectAllStandardFields() {
        String query = "SELECT fields(Standard) from Contact";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), Matchers.empty());
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectAllStandardFieldsWithSecurityEnforced() {
        String query = "SELECT fields(Standard) from Contact WITH Security_Enforced";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.isSecurityEnforced(), equalTo(true));
        assertThat(actualQueryInfo.getFields(), Matchers.empty());
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectAllFieldsWithWhereClause() {
        String query = "SELECT fields(all) from Contact WHERE Id = 'abcd'";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), contains("Id"));
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectAllCustomFieldsWithWhereClause() {
        String query = "SELECT fields(custom) from Contact WHERE Id = 'abcd'";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), contains("Id"));
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectAllStandardFieldsWithWhereClause() {
        String query = "SELECT fields(standard) from Contact WHERE Id = 'abcd'";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), contains("Id"));
        assertThat(actualQueryInfo.isAllFields(), equalTo(true));
    }

    @Test
    public void testSelectWithGroupByClause() {
        String query = "SELECT Name from Contact Group by Category";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), containsInAnyOrder("Name", "Category"));
    }

    @Test
    public void testSelectWithOrderByClause() {
        String query = "SELECT Name from Contact Order by Category";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), containsInAnyOrder("Name", "Category"));
    }

    @Test
    public void testSelectWithGroupByAndWhereClause_correctOrder() {
        String query = "SELECT Id from Contact Where Name = 'abcd' Group by Category";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), containsInAnyOrder("Id", "Name", "Category"));
    }

    @Test
    public void testSelectWithGroupByAndWhereClause_correctOrder_withNewlines() {
        String query =
                "SELECT Id, Name\n"
                        + "                       from Contact\n"
                        + "                       Where\n"
                        + "                       Name = 'abcd'\n"
                        + "                       Group by Category";

        final SoqlQueryInfo actualQueryInfo = getQueryInfo(query);

        assertThat(actualQueryInfo.getFields(), containsInAnyOrder("Id", "Name", "Category"));
    }

    private SoqlQueryInfo getQueryInfo(String query) {
        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(1));

        return queryInfos.iterator().next();
    }

    @Test
    public void testWithOneInnerQuery() {
        String query = "SELECT Id, Name, (SELECT FirstName from Contact) from Account";

        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(2));
        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(queryInfos);

        final SoqlQueryInfo outerQuery = infoIterator.next();
        assertThat(outerQuery.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(outerQuery.getFields(), containsInAnyOrder("Id", "Name"));
        assertThat(outerQuery.isOutermost(), equalTo(true));

        final SoqlQueryInfo innerQuery = infoIterator.next();
        assertThat(innerQuery.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(innerQuery.getFields(), contains("FirstName"));
        assertThat(innerQuery.isOutermost(), equalTo(false));
    }

    @Test
    public void testTwoInnerQueries() {
        String query =
                "SELECT Id, Name, (SELECT FirstName FROM Contact),\n"
                        + "(SELECT Description FROM Opportunity)\n"
                        + "FROM Account\n";

        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(3));

        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(queryInfos);

        final SoqlQueryInfo outerQuery = infoIterator.next();
        assertThat(outerQuery.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(outerQuery.getFields(), containsInAnyOrder("Id", "Name"));
        assertThat(outerQuery.isOutermost(), equalTo(true));

        final SoqlQueryInfo innerQuery1 = infoIterator.next();
        assertThat(innerQuery1.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(innerQuery1.getFields(), contains("FirstName"));
        assertThat(innerQuery1.isOutermost(), equalTo(false));

        final SoqlQueryInfo innerQuery2 = infoIterator.next();
        assertThat(innerQuery2.getObjectName(), equalToIgnoringCase("Opportunity"));
        assertThat(innerQuery2.getFields(), contains("Description"));
        assertThat(innerQuery2.isOutermost(), equalTo(false));
    }

    @Test
    public void test2LevelNestedQueries() {
        String query =
                "SELECT Id, Name, (SELECT FirstName, (SELECT Description FROM Opportunity) FROM Contact) FROM Account\n";

        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(3));

        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(queryInfos);

        final SoqlQueryInfo outerQuery = infoIterator.next();
        assertThat(outerQuery.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(outerQuery.getFields(), containsInAnyOrder("Id", "Name"));
        assertThat(outerQuery.isOutermost(), equalTo(true));

        final SoqlQueryInfo innerQuery = infoIterator.next();
        assertThat(innerQuery.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(innerQuery.getFields(), contains("FirstName"));
        assertThat(innerQuery.isOutermost(), equalTo(false));

        final SoqlQueryInfo innerMostQuery = infoIterator.next();
        assertThat(innerMostQuery.getObjectName(), equalToIgnoringCase("Opportunity"));
        assertThat(innerMostQuery.getFields(), contains("Description"));
        assertThat(innerMostQuery.isOutermost(), equalTo(false));
    }

    @Test
    public void testObjectName() {
        String query = "SELECT Id, Name, (SELECT FirstName from Contact) from Account";

        final HashSet<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);

        assertThat(SoqlParserUtil.getObjectName(queryInfos), equalToIgnoringCase("Account"));
    }

    @Test
    public void testIsCountQuerySimple() {
        String query = "SELECT COUNT() FROM Account";
        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);

        assertThat(SoqlParserUtil.isCountQuery(queryInfos.iterator().next()), equalTo(true));
    }

    @Test
    public void testIsCountQueryWhereClause() {
        String query = "SELECT COUNT() FROM Account WHERE Name = 'Acme Inc.'";
        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);

        assertThat(SoqlParserUtil.isCountQuery(queryInfos.iterator().next()), equalTo(false));
    }

    @Test
    public void testIsSingleSObjectTest_withSingleObject() {
        String query = "SELECT Name, (SELECT FirstName FROM Contact) from Account LIMIT 1";

        final HashSet<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(2));
        assertThat(SoqlParserUtil.isSingleSObject(queryInfos), equalTo(true));
    }

    @Test
    public void testIsSingleSObjectTest_withList() {
        String query = "SELECT Name, (SELECT FirstName FROM Contact LIMIT 1) from Account";

        final HashSet<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(2));
        assertThat(SoqlParserUtil.isSingleSObject(queryInfos), equalTo(false));
    }

    @Test
    public void testSecurityEnforcedOnSimpleInnerQuery() {
        String query =
                "SELECT Name, (SELECT FirstName FROM Contact) from Account WITH Security_Enforced";

        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(2));
        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(queryInfos);

        final SoqlQueryInfo outerQuery = infoIterator.next();
        assertThat(outerQuery.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(outerQuery.isSecurityEnforced(), equalTo(true));

        final SoqlQueryInfo innerQuery = infoIterator.next();
        assertThat(innerQuery.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(outerQuery.isSecurityEnforced(), equalTo(true));
    }

    @Test
    public void testSecurityEnforced2LevelNestedQueries() {
        String query =
                "SELECT Id, Name, (SELECT FirstName, (SELECT Description FROM Opportunity) FROM Contact WITH SECURITY_ENFORCED) FROM Account\n";

        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(3));

        final Iterator<SoqlQueryInfo> infoIterator =
                ObjectFieldTestHelper.getSortedIterator(queryInfos);

        final SoqlQueryInfo outerQuery = infoIterator.next();
        assertThat(outerQuery.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(outerQuery.isSecurityEnforced(), equalTo(false));

        final SoqlQueryInfo innerQuery = infoIterator.next();
        assertThat(innerQuery.getObjectName(), equalToIgnoringCase("Contact"));
        assertThat(innerQuery.isSecurityEnforced(), equalTo(true));

        final SoqlQueryInfo innerMostQuery = infoIterator.next();
        assertThat(innerMostQuery.getObjectName(), equalToIgnoringCase("Opportunity"));
        assertThat(innerQuery.isSecurityEnforced(), equalTo(true));
    }

    @Test
    public void testSecurityEnforcedInComplexQuery() {
        String query =
                "SELECT Id, "
                        + "Name, "
                        + "StageName, "
                        + "Amount, "
                        + "(SELECT Id, "
                        + "Field1__c "
                        + "FROM Relational__r "
                        + "WHERE Field2__c = false) "
                        + "FROM Opportunity "
                        + "WHERE AccountId = :param "
                        + "AND IsClosed = false "
                        + "WITH SECURITY_ENFORCED";

        final Set<SoqlQueryInfo> queryInfos = SoqlParserUtil.parseQuery(query);
        assertThat(queryInfos, hasSize(2));

        final Iterator<SoqlQueryInfo> iterator =
                ObjectFieldTestHelper.getSortedIterator(queryInfos);

        final SoqlQueryInfo queryInfo1 = iterator.next();
        assertThat(queryInfo1.isSecurityEnforced(), equalTo(true));

        final SoqlQueryInfo queryInfo2 = iterator.next();
        assertThat(queryInfo2.isSecurityEnforced(), equalTo(true));
    }

    @Test
    public void testRecordTypeQuery() {
        String query =
                "SELECT Id, Name FROM RecordType WHERE SObjectType = 'Account' AND Name LIKE :formattedValue";
        final SoqlQueryInfo queryInfo = getQueryInfo(query);

        assertThat(queryInfo.getObjectName(), equalToIgnoringCase("Account"));
        assertThat(queryInfo.getFields(), containsInAnyOrder("Id", "Name"));
    }

    @Test
    public void testFieldsRequireAccessCheck() {
        String query = "SELECT Id FROM Account";
        final SoqlQueryInfo queryInfo = getQueryInfo(query);

        assertThat(queryInfo.getFieldsRequireAccessCheck(), equalTo(false));
    }

    @Test
    public void testFieldsRequireAccessCheck_someFields() {
        String query = "SELECT Id, Name FROM Account";
        final SoqlQueryInfo queryInfo = getQueryInfo(query);

        assertThat(queryInfo.getFieldsRequireAccessCheck(), equalTo(true));
    }

    @Test
    public void testFieldsRequireAccessCheck_allFields() {
        String query = "SELECT FIELDS(ALL) FROM Account";
        final SoqlQueryInfo queryInfo = getQueryInfo(query);

        assertThat(queryInfo.getFieldsRequireAccessCheck(), equalTo(true));
    }
}

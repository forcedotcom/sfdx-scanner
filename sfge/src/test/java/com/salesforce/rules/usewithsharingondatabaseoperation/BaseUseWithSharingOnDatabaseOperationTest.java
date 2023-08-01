package com.salesforce.rules.usewithsharingondatabaseoperation;

import com.salesforce.rules.UseWithSharingOnDatabaseOperation;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.*;

/** provides infrastructure for other tests in this package */
public abstract class BaseUseWithSharingOnDatabaseOperationTest extends BasePathBasedRuleTest {

    protected static final UseWithSharingOnDatabaseOperation RULE =
            UseWithSharingOnDatabaseOperation.getInstance();

    protected static final String MY_CLASS = "MyClass";

    /**
     * selected database operations crossed with sharing policies two times see {@link
     * #provideSelectDatabaseOperationMatrixTwoSharing()}
     */
    private static Stream<Arguments> selectDatabaseOperationMatrixTwoSharing = null;

    /**
     * selected database operations crossed with sharing policies three times see {@link
     * #provideSelectDatabaseOperationMatrixThreeSharing()}
     */
    private static Stream<Arguments> selectDatabaseOperationMatrixThreeSharing = null;

    /**
     * holds insert a; etc. queries that are represented as {@link
     * com.salesforce.graph.vertex.DmlStatementVertex}
     *
     * <p>IMPORTANT: these are meant to be used in a situation where <code>a</code>, an Account, is
     * already defined.
     */
    protected static final String[] DML_STATEMENTS = {
        "insert a", "delete a", "merge a a", "undelete a", "upsert a", "update a",
    };

    /**
     * holds [SELECT whatever] queries that are represented as {@link
     * com.salesforce.graph.vertex.SoqlExpressionVertex}
     */
    public static final String[] SOQL_STATEMENTS = {
        "Account b = [SELECT Id, NumberOfEmployees FROM account WHERE NumberOfEmployees = 3 LIMIT 1]",
    };

    /**
     * holds Database.whatever methods that are represented as {@link
     * com.salesforce.graph.vertex.MethodCallExpressionVertex}
     *
     * <p>IMPORTANT: these are meant to be used in a situation where <code>a</code>, an Account, is
     * already defined.
     */
    public static final String[] DATABASE_METHODS = {
        "Database.convertLead(null, true)",
        "Database.countQuery('SELECT count() FROM Account')",
        "Database.countQueryWithBinds('SELECT count() FROM Account', new Map<String, Object>{'name' => 'RAPTOR'})",
        "Database.delete(a, false)",
        "Database.deleteAsync(List.of(na), false)",
        "Database.deleteImmediate(a)",
        "Database.emptyRecycleBin(new Account[]{a})",
        "Database.executeBatch(null)",
        "Database.getAsyncDeleteResult(Database.deleteAsync(List.of(a), false))",
        "Database.getAsyncLocator(Database.deleteAsync(List.of(a), false))",
        "Database.getAsyncSaveResult(Database.insert(a))",
        "Database.getDeleted('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "Database.getQueryLocator('SELECT Id, NumberOfEmployees FROM account WHERE NumberOfEmployees = 3 LIMIT 1')",
        "Database.getQueryLocatorWithBinds('SELECT Id, NumberOfEmployees FROM account WHERE NumberOfEmployees = 3 LIMIT 1', new Map<String, Object>{'name' => 'KENSINGTON'})",
        "Database.getUpdated('account__c', Datetime.now().addHours(-1), Datetime.now())",
        "Database.insert(a)",
        "Database.insertAsync(a, nothing())",
        "Database.insertImmediate(a)",
        "Database.merge(a, new Account(name = 'Benjamin'))",
        "Database.query('SELECT Id, NumberOfEmployees, Name FROM Account WHERE NumberOfEmployees = 100 LIMIT 1')",
        "Database.queryWithBinds('SELECT ID, NumberOfEmployees, Name FROM Account WHERE NumberOfEmployees = 100', new Map<String, Object>{'Name' => 'WOLVES'})",
        "Database.rollback(Database.setSavepoint())",
        "Database.undelete(a, false)",
        "Database.update(a, false)",
        "Database.updateAsync(a, false)",
        "Database.updateImmediate(a, false)",
        "Database.upsert(a, null, false)" // 34 rows
    };

    protected static final String[] SHARING_POLICIES = {
        "with sharing", "without sharing", "inherited sharing", ""
    };

    /** function sto get the expected violation message */
    protected ViolationWrapper.SharingPolicyViolationBuilder expect(int sinkLine) {
        return ViolationWrapper.SharingPolicyViolationBuilder.get(sinkLine);
    }

    /** function sto get the expected warning message */
    protected ViolationWrapper.SharingPolicyViolationBuilder expectWarning(
            int sinkLine,
            SharingPolicyUtil.InheritanceType inheritanceType,
            String classInheritedFrom) {
        return ViolationWrapper.SharingPolicyViolationBuilder.getWarning(
                sinkLine, inheritanceType, classInheritedFrom);
    }

    protected static Stream<Arguments> provideAllDatabaseOperations() {
        Stream.Builder<Arguments> s = Stream.builder();
        Arrays.stream(DML_STATEMENTS).forEach(op -> s.add(Arguments.of(op)));
        Arrays.stream(SOQL_STATEMENTS).forEach(op -> s.add(Arguments.of(op)));
        Arrays.stream(DATABASE_METHODS).forEach(op -> s.add(Arguments.of(op)));

        return s.build();
    }

    protected static Stream<Arguments> provideSelectDatabaseOperations() {
        Stream.Builder<Arguments> s = Stream.builder();
        s.add(Arguments.of(DML_STATEMENTS[0]));
        s.add(Arguments.of(SOQL_STATEMENTS[0]));
        s.add(Arguments.of(DATABASE_METHODS[0]));

        // test 4 obscure Database.whatever methods
        s.add(Arguments.of(DATABASE_METHODS[1])); // count query
        s.add(Arguments.of(DATABASE_METHODS[6])); // empty recycle bin
        s.add(Arguments.of(DATABASE_METHODS[12])); // get query locator
        s.add(Arguments.of(DATABASE_METHODS[14])); // get updated

        return s.build();
    }

    /**
     * provides the cross product of: all database operations x all sharing policies x all sharing
     * policies
     */
    protected static Stream<Arguments> provideSelectDatabaseOperationMatrixTwoSharing() {
        if (selectDatabaseOperationMatrixTwoSharing == null) {

            Stream<Arguments> databaseOpeartions =
                    provideSelectDatabaseOperationMatrixThreeSharing();

            Stream.Builder<Arguments> argsBuilder = Stream.builder();

            databaseOpeartions.forEach(
                    operation -> {
                        for (String policyOne : SHARING_POLICIES) {
                            for (String policyTwo : SHARING_POLICIES) {
                                argsBuilder.add(
                                        Arguments.of(operation.get()[0], policyOne, policyTwo));
                            }
                        }
                    });

            selectDatabaseOperationMatrixTwoSharing = argsBuilder.build();
        }

        return selectDatabaseOperationMatrixTwoSharing;
    }

    /**
     * provides the cross product of: all database operations x all sharing policies x all sharing
     * policies x all sharing policies
     */
    protected static Stream<Arguments> provideSelectDatabaseOperationMatrixThreeSharing() {
        if (selectDatabaseOperationMatrixThreeSharing == null) {
            Stream<Arguments> databaseOpeartions = provideSelectDatabaseOperations();

            Stream.Builder<Arguments> argsBuilder = Stream.builder();

            databaseOpeartions.forEach(
                    operation -> {
                        for (String policyOne : SHARING_POLICIES) {
                            for (String policyTwo : SHARING_POLICIES) {
                                for (String policyThree : SHARING_POLICIES) {
                                    argsBuilder.add(
                                            Arguments.of(
                                                    operation.get()[0],
                                                    policyOne,
                                                    policyTwo,
                                                    policyThree));
                                }
                            }
                        }
                    });

            selectDatabaseOperationMatrixThreeSharing = argsBuilder.build();
        }

        return selectDatabaseOperationMatrixThreeSharing;
    }
}

package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class UnresolvedCrudFlsTest extends BaseFlsTest {

    @Test
    public void testReadFirstLevelMethodCallValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       Database.query(getQuery());\n"
                        + "   }\n"
                        + "   String getQuery() {\n"
                        + "       return 'SELECT Name from Account';"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @Test
    public void testReadSecondLevelMethodCallValue() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void foo() {\n"
                    + "       Database.query(QueryBuilder.createQueryBuilder().setLimit(10).getQuery());\n"
                    + "   }\n"
                    + "}\n",
            "public class QueryBuilder {\n"
                    + "   static QueryBuilder createQueryBuilder() {\n"
                    + "       return new QueryBuilder();\n"
                    + "   }\n"
                    + "   QueryBuilder setLimit(Integer l) {\n"
                    + "       return this;"
                    + "   }\n"
                    + "   String getQuery() {\n"
                    + "       return 'SELECT Name from Account';"
                    + "   }\n"
                    + "}\n"
        };

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @Test
    public void testInsertFirstLevelMethodCallValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       Database.insert(getAccounts());\n"
                        + "   }\n"
                        + "   String getAccounts() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       accounts.add(new Account(Name = 'Acme Inc'));\n"
                        + "       return accounts;"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @Test
    public void testInsertSecondLevelMethodCallValue() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void foo() {\n"
                    + "       Database.insert(QueryBuilder.createQueryBuilder().getAccounts());\n"
                    + "   }\n"
                    + "}\n",
            "public class QueryBuilder {\n"
                    + "   static QueryBuilder createQueryBuilder() {\n"
                    + "       return new QueryBuilder();\n"
                    + "   }\n"
                    + "   String getAccounts() {\n"
                    + "       List<Account> accounts = new List<Account>();\n"
                    + "       accounts.add(new Account(Name = 'Acme Inc'));\n"
                    + "       return accounts;"
                    + "   }\n"
                    + "}\n"
        };

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @EnumSource(
            value = FlsConstants.FlsValidationType.class,
            names = "MERGE", // Other than Merge operation, all methods take a parameter
            mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest()
    public void testUnresolvedMethodCallValue(FlsConstants.FlsValidationType validationType) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       "
                        + validationType.databaseOperationMethod
                        + "(MyBuilder.someMethod());\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expectUnresolvedCrudFls(3, validationType));
    }

    @Test
    public void testReadWithUnresolvedBinaryExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo(String fields, String objectName) {\n"
                        + "       List<Contact> contacts = Database.query('SELECT ' + \n"
                        + "fields +\n"
                        + "'FROM ' + \n"
                        + "objectName);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                ApexFlsViolationRule.getInstance(),
                sourceCode,
                expectUnresolvedCrudFls(3, FlsConstants.FlsValidationType.READ));
    }
}

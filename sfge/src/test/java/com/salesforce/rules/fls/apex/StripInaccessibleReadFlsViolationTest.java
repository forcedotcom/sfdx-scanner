package com.salesforce.rules.fls.apex;

import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StripInaccessibleReadFlsViolationTest extends BaseFlsTest {
    static final FlsConstants.FlsValidationType READ_VALIDATION =
            FlsConstants.FlsValidationType.READ;

    public static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of(
                        "SOQL",
                        ApexFlsViolationRule.getInstance(),
                        "[Select Id, Name, Phone from Account]"),
                Arguments.of(
                        "Database.query",
                        ApexFlsViolationRule.getInstance(),
                        "Database.query('Select Id, Name, Phone from Account')"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_simpleReadQuery(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_simpleReadQuery(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_checkBeforeDml(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "       accounts = "
                        + soqlOperation
                        + ";\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(5, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_SingleType_UnmatchedAccessType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "       Security.stripInaccessible(AccessType.CREATABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_MultipleTypes_UnmatchedListType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Contact> contacts = new List<Contact>();\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, contacts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_MultipleTypes_UnmatchedListType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Contact> contacts = new List<Contact>();\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_IncorrectListChecked(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> accounts2 = new List<Account>();\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts2);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_IncorrectListChecked_FieldMatch(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> accounts2 = new List<Account>();\n"
                        + "       Account account = new Account(Name = 'Acme Inc.', Phone = '123-456-7890');\n"
                        + "       accounts2.add(account);\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts2);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_correctListChecked_FieldMatch(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> accounts2 = new List<Account>();\n"
                        + "       Account account = new Account(Name = 'Acme Inc.', Phone = '123-456-7890');\n"
                        + "       accounts2.add(account);\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts1);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_doubleCheck(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       if (Schema.SObjectType.Account.fields.Name.isAccessible()) {\n"
                        + "           accounts = [Select Name from Account];\n"
                        + "       }\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expectStripInaccWarning(5, FlsConstants.FlsValidationType.READ, "Account")
                        .withField("Name"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_TwoQueries_onlyOneChecked_differentObjectType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> contacts = [Select Id, FirstName, Phone from Contact];\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Contact")
                        .withFields(new String[] {"FirstName", "Phone"}),
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_TwoQueries_bothChecked_differentObjectType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> contacts = [Select Id, FirstName, Phone from Contact];\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, contacts);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}),
                expectStripInaccWarning(4, FlsConstants.FlsValidationType.READ, "Contact")
                        .withFields(new String[] {"FirstName", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testUnsafe_TwoQueries_onlyOneChecked_sameObjectType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> accounts2 = "
                        + soqlOperation
                        + ";\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts1);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}),
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSafe_TwoQueries_bothChecked_sameObjectType(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<Account> accounts1 = "
                        + soqlOperation
                        + ";\n"
                        + "       List<Account> accounts2 = "
                        + soqlOperation
                        + ";\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts1);\n"
                        + "       Security.stripInaccessible(AccessType.READABLE, accounts2);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}),
                expectStripInaccWarning(4, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testNoStripInaccWarningWhenDisabled(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        try {
            // Disable WarningViolation in config
            SfgeConfigTestProvider.set(
                    new TestSfgeConfig() {
                        @Override
                        public boolean isWarningViolationEnabled() {
                            return false;
                        }
                    });

            String sourceCode =
                    "public class MyClass {\n"
                            + "   public void foo() {\n"
                            + "       List<Account> accounts = "
                            + soqlOperation
                            + ";\n"
                            + "       Security.stripInaccessible(AccessType.READABLE, accounts);\n"
                            + "   }\n"
                            + "}\n";
            assertNoViolation(rule, sourceCode);

        } finally {
            SfgeConfigTestProvider.remove();
        }
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSoqlValueFromMethod(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String source =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		Security.stripInaccessible(AccessType.READABLE, getAccounts());\n"
                        + "	}\n"
                        + "	public List<Account> getAccounts() {\n"
                        + "		return "
                        + soqlOperation
                        + ";\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                source,
                expectStripInaccWarning(6, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{0}")
    public void testSoqlValueFromInline(
            String operationName, ApexFlsViolationRule rule, String soqlOperation) {
        String source =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		Security.stripInaccessible(AccessType.READABLE, "
                        + soqlOperation
                        + ");\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                source,
                expectStripInaccWarning(3, FlsConstants.FlsValidationType.READ, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }
}

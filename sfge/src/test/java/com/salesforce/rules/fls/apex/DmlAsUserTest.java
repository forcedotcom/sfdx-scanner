package com.salesforce.rules.fls.apex;

import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests to verify that "as user" invocation on DML does not cause violations on
 * ApexFlsViolationRule.
 */
public class DmlAsUserTest extends BaseFlsTest {
    private static final AbstractPathBasedRule RULE = ApexFlsViolationRule.getInstance();

    public static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of(
                        "Insert_KeyValue1",
                        "Account a = new Account();\n"
                                + "a.name = 'Acme Inc.'\n;"
                                + "insert %s a;\n"),
                Arguments.of(
                        "Insert_KeyValue2",
                        "Account a = new Account(Name = 'Acme Inc.');\n" + "insert %s a;\n"),
                Arguments.of(
                        "Update",
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account a = [SELECT Id, Name FROM Account];\n"
                                + "a.Name = 'Acme Inc.';\n"
                                + "update %s a;\n"),
                Arguments.of(
                        "Delete",
                        "Account a = new Account(Id = '001abc000000001', Name = 'Acme Inc.');\n"
                                + "delete %s a;\n"),
                Arguments.of(
                        "Merge",
                        "Account a1 = new Account(Name = 'Acme Inc.');\n"
                                + "Account a2 = new Account(Name = 'Acme');\n"
                                + "merge %s a1 a2;\n"),
                Arguments.of(
                        "Undelete",
                        "/* sfge-disable-next-line ApexFlsViolationRule */\n"
                                + "Account a = [SELECT Id, Name FROM Account WHERE Name = 'Acme Inc' ALL ROWS];\n"
                                + "undelete %s a;\n"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDmlIsSafe(String testName, String dmlStatement) {
        // spotless: off
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + String.format(dmlStatement, "as user")
                        + "    }\n"
                        + "}\n";
        // spotless: on

        assertNoViolation(RULE, sourceCode);
    }
}

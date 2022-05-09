package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MergeFlsScenariosTest extends BaseFlsTest {
    public static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of("DML", ApexFlsViolationRule.getInstance(), "merge %1$s %2$s;\n"),
                Arguments.of(
                        "Database",
                        ApexFlsViolationRule.getInstance(),
                        "Database.merge(%1$s, %2$s);\n"),
                Arguments.of(
                        "Database with boolean",
                        ApexFlsViolationRule.getInstance(),
                        "Database.merge(%1$s, %2$s, false);\n"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_withDifferentQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account mergeAcct = [SELECT Id FROM Account WHERE Name = 'Acme'];\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account masterAcct = [SELECT Id FROM Account WHERE Name = 'Acme Inc'];\n"
                        + String.format(dmlFormat, "masterAcct", "mergeAcct")
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(7, FlsConstants.FlsValidationType.MERGE, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled
    public void testUnsafe_withSameQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account acc = [SELECT Id FROM Account WHERE Name = 'Acme'];\n"
                        + String.format(dmlFormat, "acc.get(0)", "acc.get(1)")
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(5, FlsConstants.FlsValidationType.MERGE, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_withTwoSObjects(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account acc1 = new Account(Name = 'Acme Inc');\n"
                        + "        Account acc2 = new Account(Name = 'Acme');\n"
                        + String.format(dmlFormat, "acc1", "acc2")
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(5, FlsConstants.FlsValidationType.MERGE, "Account"));
    }

    @MethodSource("input")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafe_withsObjectQuery(
            String testCategory, ApexFlsViolationRule rule, String dmlFormat) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Account acc1 = new Account(Name = 'Acme Inc');\n"
                        + "		 /* sfge-disable-next-line ApexFlsViolationRule */\n"
                        + "        Account acc2 = [SELECT Id FROM Account WHERE Name = 'Acme' limit 1];\n"
                        + String.format(dmlFormat, "acc1", "acc2")
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule, sourceCode, expect(6, FlsConstants.FlsValidationType.MERGE, "Account"));
    }
}

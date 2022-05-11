package com.salesforce.rules.fls.apex;

import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.testutils.BaseFlsTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SpecialObjectTypesFlsTest extends BaseFlsTest {

    private final ApexFlsViolationRule rule;

    public SpecialObjectTypesFlsTest() {
        rule = ApexFlsViolationRule.getInstance();
    }

    private static Stream<Arguments> provideSpecialObjectData() {
        return Stream.of(
                Arguments.of("System read-only object", "FeedItem"),
                Arguments.of("Metadata object", "My_Metadata__mdt"));
    }

    @MethodSource("provideSpecialObjectData")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeRead(String testName, String objectName) {
        final String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		[SELECT Name FROM "
                        + objectName
                        + "];\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @MethodSource("provideSpecialObjectData")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testUnsafeDml(String testName, String objectName) {
        final String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		"
                        + objectName
                        + " obj = new "
                        + objectName
                        + "(Name='Acme Inc');\n"
                        + "		insert obj;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsValidationType.INSERT, objectName).withField("Name"));
    }

    @MethodSource("provideSpecialObjectData")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSafeDml(String testName, String objectName) {
        final String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "		"
                        + objectName
                        + " obj = new "
                        + objectName
                        + "(Name='Acme Inc');\n"
                        + "		if (Schema.SObjectType."
                        + objectName
                        + ".Fields.Name.isCreateable()) {\n"
                        + "			insert obj;\n"
                        + "		}\n"
                        + "    }\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeObjectLevelCheckForStdObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void foo(Account acc) {\n"
                        + "		if (Schema.SObjectType.Account.isCreateable()) {\n"
                        + "			insert acc;\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(4, FlsValidationType.INSERT, "Account").withField("Unknown"));
    }
}

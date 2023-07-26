package com.salesforce.rules.usewithsharingondatabaseoperation;

import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import org.junit.jupiter.api.Test;

/**
 * the purpose of these tests are to ensure warnings are not logged when the configuration disables
 * them. for full warning code testing & coverage, see other tests. This class has one test for each
 * "route" to a warning: implicitly inheriting from an ancestor, and implicitly inheriting from a
 * calling class.
 */
public class SharingPolicyWarningDisabledTest extends BaseUseWithSharingOnDatabaseOperationTest {

    // spotless:off
    private static final String ONE_SOURCE =
        "public %s class One {\n"
            + "void foo() {\n"
            + "}\n"
        + "}\n";
    private static final String TWO_SOURCE =
        "public %s class Two extends One {\n"
            + "void foo(Account a) {\n"
            + "   %s;"
            + "}\n"
        + "\n} ";

    private static final String PARENT_CLASS_SOURCE =
        "public %s class ParentClass { } \n";

    private static final String MY_CLASS_SOURCE =
        "public %s class " + MY_CLASS + " extends ParentClass {\n"
            + "void foo(Account a) {\n"
            + "    Two t = new Two();\n"
            + "    t.foo(a);\n"
            + "}\n"
        + "}\n";

    // spotless:on

    /**
     * Although this code should produce a warning (class with database operation implicitly
     * inherits "with sharing" policy from its parent), we configure an SFGE configuration to
     * disable warnings. Thus, there should be no violations.
     */
    @Test
    public void testInheritFromParentNoWarning() {

        try {
            // Disable WarningViolation in config
            SfgeConfigTestProvider.set(
                    new TestSfgeConfig() {
                        @Override
                        public boolean isWarningViolationDisabled() {
                            return true;
                        }
                    });

            String[] sourceCode = {
                String.format(ONE_SOURCE, "with sharing"),
                String.format(TWO_SOURCE, "", "insert a"),
                String.format(PARENT_CLASS_SOURCE, ""),
                String.format(MY_CLASS_SOURCE, ""),
            };

            assertNoViolation(RULE, sourceCode);
        } finally {
            SfgeConfigTestProvider.remove();
        }
    }

    /**
     * Although this code should produce a warning (class with database operation implicitly
     * inherits "with sharing" policy from the calling class), we configure an SFGE configuration to
     * disable warnings. Thus, there should be no violations.
     */
    @Test
    public void testInheritFromCallingNoWarning() {

        try {
            // Disable WarningViolation in config
            SfgeConfigTestProvider.set(
                    new TestSfgeConfig() {
                        @Override
                        public boolean isWarningViolationDisabled() {
                            return true;
                        }
                    });

            String[] sourceCode = {
                String.format(ONE_SOURCE, ""),
                String.format(TWO_SOURCE, "", "insert a"),
                String.format(PARENT_CLASS_SOURCE, "with sharing"),
                String.format(MY_CLASS_SOURCE, "with sharing"),
            };

            assertNoViolation(RULE, sourceCode);
        } finally {
            SfgeConfigTestProvider.remove();
        }
    }
}

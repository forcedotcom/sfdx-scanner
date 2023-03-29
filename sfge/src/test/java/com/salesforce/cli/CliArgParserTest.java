package com.salesforce.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.rules.RuleUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class CliArgParserTest {

    @ValueSource(
            strings = {
                // Simple lowercase test.
                "execute",
                // Case-insensitivity test.
                "eXeCuTe",
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void getCliAction_getsExecuteEnum(String arg) {
        CliArgParser parser = new CliArgParser();
        CliArgParser.CLI_ACTION result = parser.getCliAction(arg);
        assertThat(result, equalTo(CliArgParser.CLI_ACTION.EXECUTE));
    }

    @ValueSource(
            strings = {
                // Simple lowercase test.
                "catalog",
                // Case-insensitivity test.
                "CaTaLoG"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void getCliAction_getsCatalogEnum(String arg) {
        CliArgParser parser = new CliArgParser();
        CliArgParser.CLI_ACTION result = parser.getCliAction(arg);
        assertThat(result, equalTo(CliArgParser.CLI_ACTION.CATALOG));
    }

    @Test
    public void getCliAction_throwsExpectedError() {
        CliArgParser parser = new CliArgParser();
        assertThrows(
                CliArgParser.InvocationException.class,
                () -> parser.getCliAction("notARealAction"),
                String.format(
                        UserFacingMessages.InvocationErrors.UNRECOGNIZED_ACTION, "notARealAction"));
    }

    @CsvSource({
        // As we add new DFA and non-DFA rules, the numbers in these tests
        // will increase.
        "pathless, 2",
        "dfa, 2"
    })
    @ParameterizedTest(name = "{displayName}: {0} rules")
    public void catalogFlowReturnsExpectedRules(String arg, int ruleCount) {
        CliArgParser.CatalogArgParser parser = new CliArgParser.CatalogArgParser();
        try {
            parser.parseArgs("catalog", arg);
            assertThat(parser.getSelectedRules().size(), equalTo(ruleCount));
        } catch (RuleUtil.RuleNotFoundException rnf) {
            fail("Should not throw exception");
        }
    }
}

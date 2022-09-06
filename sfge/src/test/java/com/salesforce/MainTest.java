package com.salesforce;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.salesforce.cli.CliArgParser;
import com.salesforce.cli.Result;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.rules.RuleRunner;
import com.salesforce.rules.Violation;
import com.salesforce.testutils.DummyVertex;
import java.io.IOException;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MainTest {

    private static final String EXECUTION_ARGS_FILENAME = "executionArgsTest.json";

    private static final List<String> EXECUTION_ARGS_JSON =
            Lists.newArrayList(
                    "{\n",
                    "   \"rulesToRun\": [\"ApexFlsViolationRule\"],\n",
                    "   \"projectDirs\": [\"/path/to/project/dir/\"],\n",
                    "   \"targets\": [{\n",
                    "       \"targetFile\": \"/path/to/DemoExample.cls\",\n",
                    "       \"targetMethods\": [\"exampleMethod\"]\n",
                    "   }]\n",
                    "}");

    private static final String EXECUTE_ACTION = CliArgParser.CLI_ACTION.EXECUTE.name();
    private static final Violation.StaticRuleViolation DUMMY_VIOLATION =
            new Violation.StaticRuleViolation("dummy message", new DummyVertex("dummy label"));
    private static final String CLI_MESSAGES = "SFDX-START[]SFDX-END";
    private static final String DUMMY_VIOLATION_JSON =
            "VIOLATIONS_START[{\"message\":\"dummy message\",\"sourceFileName\":\"dummy\",\"sourceType\":\"dummy\",\"sourceVertexName\":\"\",\"sourceLineNumber\":0,\"sourceColumnNumber\":0,\"severity\":0}]VIOLATIONS_END";
    private static final OutOfMemoryError DUMMY_ERROR =
            new OutOfMemoryError("dummy OutOfMemory error");
    private static final String ERROR_OUTPUT = "SfgeErrorStart\ndummy OutOfMemory error";

    @Mock Main.Dependencies dependencies;
    @Mock CliArgParser.Dependencies argParserDependencies;
    @Mock GraphTraversalSource g;
    @Mock RuleRunner ruleRunner;

    @BeforeEach
    void init() throws IOException {
        Mockito.lenient()
                .when(argParserDependencies.getAllLines(EXECUTION_ARGS_FILENAME))
                .thenReturn(EXECUTION_ARGS_JSON);
        final CliArgParser.ExecuteArgParser executeArgParser =
                new CliArgParser.ExecuteArgParser(argParserDependencies);

        Mockito.lenient().when(dependencies.createExecuteArgParser()).thenReturn(executeArgParser);
        Mockito.lenient().when(dependencies.getGraph()).thenReturn(g);
    }

    @Test
    void testNoActionProvided() {
        final Main main = new Main(dependencies);
        main.process();
        verify(dependencies).printError(UserFacingMessages.REQUIRES_AT_LEAST_ONE_ARGUMENT);
    }

    @Test
    void testInvalidActionProvided() {
        final Main main = new Main(dependencies);
        final String invalid_action = "INVALID_ACTION";

        assertThrows(
                CliArgParser.InvocationException.class,
                () -> main.process(invalid_action),
                String.format(UserFacingMessages.UNRECOGNIZED_ACTION, invalid_action));
    }

    @Test
    void testRuleExecutionNoErrorNoViolations() {
        final Result noViolationNoErrorResult = new Result();
        Mockito.lenient()
                .when(ruleRunner.runRules(Mockito.anyList(), Mockito.anyList()))
                .thenReturn(noViolationNoErrorResult);
        Mockito.lenient().when(dependencies.createRuleRunner(g)).thenReturn(ruleRunner);

        final Main main = new Main(dependencies);
        final int exitCode = main.process(EXECUTE_ACTION, EXECUTION_ARGS_FILENAME);

        assertThat(exitCode, equalTo(Main.EXIT_GOOD_RUN_NO_VIOLATIONS));
    }

    @Test
    void testRuleExecutionNoErrorWithViolations() {
        final Result withViolationNoErrorResult = new Result();
        withViolationNoErrorResult.addViolation(DUMMY_VIOLATION);
        Mockito.lenient()
                .when(ruleRunner.runRules(Mockito.anyList(), Mockito.anyList()))
                .thenReturn(withViolationNoErrorResult);
        Mockito.lenient().when(dependencies.createRuleRunner(g)).thenReturn(ruleRunner);

        final Main main = new Main(dependencies);
        final int exitCode = main.process(EXECUTE_ACTION, EXECUTION_ARGS_FILENAME);

        assertThat(exitCode, equalTo(Main.EXIT_GOOD_RUN_WITH_VIOLATIONS));

        final ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(dependencies, times(2)).printOutput(outputCaptor.capture());
        assertThat(
                outputCaptor.getAllValues(), Matchers.contains(CLI_MESSAGES, DUMMY_VIOLATION_JSON));

        final ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(dependencies, times(0)).printError(errorCaptor.capture());
        assertThat(errorCaptor.getAllValues(), Matchers.empty());
    }

    @Test
    void testRuleExecutionWithErrorWithViolations() {
        final Result withViolationNoErrorResult = new Result();
        withViolationNoErrorResult.addViolation(DUMMY_VIOLATION);
        withViolationNoErrorResult.addThrowable(DUMMY_ERROR);
        Mockito.lenient()
                .when(ruleRunner.runRules(Mockito.anyList(), Mockito.anyList()))
                .thenReturn(withViolationNoErrorResult);
        Mockito.lenient().when(dependencies.createRuleRunner(g)).thenReturn(ruleRunner);

        final Main main = new Main(dependencies);
        final int exitCode = main.process(EXECUTE_ACTION, EXECUTION_ARGS_FILENAME);

        assertThat(exitCode, equalTo(Main.EXIT_WITH_INTERNAL_ERROR_AND_VIOLATIONS));

        final ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        verify(dependencies, times(2)).printOutput(outputCaptor.capture());
        assertThat(
                outputCaptor.getAllValues(), Matchers.contains(CLI_MESSAGES, DUMMY_VIOLATION_JSON));

        final ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(dependencies, times(1)).printError(errorCaptor.capture());
        assertThat(errorCaptor.getAllValues(), Matchers.contains(ERROR_OUTPUT));
    }
}

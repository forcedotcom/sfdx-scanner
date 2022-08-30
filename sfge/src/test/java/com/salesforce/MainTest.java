package com.salesforce;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.salesforce.cli.CliArgParser;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.UserActionException;
import com.salesforce.rules.RuleRunner;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class MainTest {

    @Mock
    Main.Dependencies dependencies;
    @Mock
    CliArgParser.ExecuteArgParser executeArgParser;
    @Mock
    GraphTraversalSource g;
    @Mock
    RuleRunner ruleRunner;

    @BeforeEach
    void init() {

    }

    @Test
    void testNoActionProvided() {
        final Main main = new Main(dependencies);
        main.process();
        Mockito.verify(dependencies).printError(UserFacingMessages.REQUIRES_AT_LEAST_ONE_ARGUMENT);
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
    void testRuleExecutionErrorWithNoViolations() {
        Mockito.lenient().when(dependencies.createExecuteArgParser()).thenReturn(executeArgParser);
        Mockito.lenient().when(dependencies.getGraph()).thenReturn(g);
        Mockito.lenient().when(dependencies.createRuleRunner(g)).thenReturn(ruleRunner);
        final Main main = new Main(dependencies);


        assertThrows(
            CliArgParser.InvocationException.class,
            () -> main.process(invalid_action),
            String.format(UserFacingMessages.UNRECOGNIZED_ACTION, invalid_action));
    }



}

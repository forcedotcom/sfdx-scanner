package com.salesforce.graph.ops.directive;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;

import com.salesforce.ArgumentsUtil;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.ContextProviders;
import com.salesforce.graph.vertex.AbstractVisitingVertexPredicate;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.DmlStatementVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VertexPredicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EngineDirectiveTest {
    private static final Logger LOGGER = LogManager.getLogger(EngineDirectiveTest.class);
    public static final List<String> DISABLE_NEXT_LINE_RULE_1_DIRECTIVES =
            Arrays.asList("/* sfge-disable-next-line Rule1 */", "// sfge-disable-next-line Rule1");

    public static final List<String> DISABLE_STACK_RULE_1_DIRECTIVES =
            Arrays.asList("/* sfge-disable-stack Rule1 */", "// sfge-disable-stack Rule1");

    private static EngineDirective DISABLE_1 =
            EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                    .withRuleName("Rule1")
                    .build();
    private static EngineDirective DISABLE_NEXT_LINE_1 =
            EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE_NEXT_LINE)
                    .withRuleName("Rule1")
                    .build();
    private static EngineDirective DISABLE_STACK_1 =
            EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE_STACK)
                    .withRuleName("Rule1")
                    .build();

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    public static Stream<Arguments> rule1DisableDirectives() {
        List<Arguments> arguments =
                Arrays.asList(
                        Arguments.of("/* sfge-disable Rule1 */"),
                        Arguments.of("// sfge-disable Rule1"));

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    public static Stream<Arguments> rule1DisableStackDirectives() {
        List<Arguments> arguments =
                Arrays.asList(
                        Arguments.of("/* sfge-disable-stack Rule1 */"),
                        Arguments.of("// sfge-disable-stack Rule1"));

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource(value = "rule1DisableDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testClassDirective(String comment) {
        String[] sourceCode = {
            comment
                    + "\n"
                    + "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug('Hello');\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        UserClassVertex userClass =
                SFVertexFactory.load(
                        g, g.V().hasLabel(NodeType.USER_CLASS).not(has(Schema.IS_STANDARD, true)));
        engineDirectives = userClass.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_1));

        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD)
                                .has(Schema.NAME, "doSomething")
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(method, DISABLE_1);

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD_CALL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(methodCallExpression, DISABLE_1);
    }

    /** Users can append '-- Some comment' to their directive */
    public static Stream<Arguments> testAdditionalComment() {
        return Stream.of(
                Arguments.of(
                        "/* sfge-disable Rule1 -- this is a comment */",
                        EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                                .withRuleName("Rule1")
                                .withComment("this is a comment")
                                .build()),
                Arguments.of(
                        "/* sfge-disable Rule1, Rule2 -- this is a comment */",
                        EngineDirective.Builder.get(EngineDirectiveCommand.DISABLE)
                                .withRuleNames(CollectionUtil.newTreeSetOf("Rule1", "Rule2"))
                                .withComment("this is a comment")
                                .build()));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: comment=({0}):expected=({1})")
    public void testAdditionalComment(String comment, EngineDirective expected) {
        String[] sourceCode = {
            comment
                    + "\n"
                    + "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug('Hello');\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        UserClassVertex userClass =
                SFVertexFactory.load(
                        g, g.V().hasLabel(NodeType.USER_CLASS).not(has(Schema.IS_STANDARD, true)));
        engineDirectives = userClass.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(expected));

        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD)
                                .has(Schema.NAME, "doSomething")
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(method, expected);

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD_CALL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(methodCallExpression, expected);
    }

    @MethodSource(value = "rule1DisableStackDirectives")
    @ParameterizedTest(name = "{displayName}: value=({0})")
    public void testMethodDirective(String comment) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + comment
                    + "\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug('Hello');\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        assertUserClassHasNoDirectives();

        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD)
                                .has(Schema.NAME, "doSomething")
                                .not(has(Schema.IS_STANDARD, true)));
        engineDirectives = method.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_STACK_1));

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD_CALL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(methodCallExpression, DISABLE_STACK_1);
    }

    public static Stream<Arguments> testMethodCallExpressionNextLine() {
        List<Arguments> arguments = new ArrayList<>();
        for (String engineDirective : DISABLE_NEXT_LINE_RULE_1_DIRECTIVES) {
            for (String methodCall : Arrays.asList("System.debug('Hello')", "Database.insert(a)")) {
                arguments.add(Arguments.of(engineDirective, methodCall));
            }
        }
        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-methodCall=({1})")
    public void testMethodCallExpressionNextLine(String comment, String methodCall) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public void doSomething(Account a) {\n"
                    + comment
                    + "\n"
                    + "		"
                    + methodCall
                    + ";\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        assertUserClassHasNoDirectives();
        assertMethodHasNoDirectives();

        ExpressionStatementVertex expressionStatement =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.EXPRESSION_STATEMENT)
                                .not(has(Schema.IS_STANDARD, true)));
        engineDirectives = expressionStatement.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_NEXT_LINE_1));

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD_CALL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(methodCallExpression, DISABLE_NEXT_LINE_1);
    }

    public static Stream<Arguments> testStandAloneSoqlNextLine() {
        List<Arguments> arguments = new ArrayList<>();
        for (String engineDirective : DISABLE_NEXT_LINE_RULE_1_DIRECTIVES) {
            arguments.add(Arguments.of(engineDirective, "[SELECT Id, Name FROM Account]"));
        }
        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expression=({1})")
    public void testStandAloneSoqlNextLine(String comment, String expression) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public void doSomething(Account a) {\n"
                    + comment
                    + "\n"
                    + "		"
                    + expression
                    + ";\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        assertUserClassHasNoDirectives();
        assertMethodHasNoDirectives();

        ExpressionStatementVertex expressionStatement =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.EXPRESSION_STATEMENT)
                                .not(has(Schema.IS_STANDARD, true)));
        engineDirectives = expressionStatement.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_NEXT_LINE_1));

        SoqlExpressionVertex soqlExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.SOQL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(soqlExpression, DISABLE_NEXT_LINE_1);
    }

    public static Stream<Arguments> testVariableDeclarationSoqlNextLine() {
        List<Arguments> arguments = new ArrayList<>();
        for (String engineDirective : DISABLE_NEXT_LINE_RULE_1_DIRECTIVES) {
            arguments.add(
                    Arguments.of(
                            engineDirective,
                            "List<Account> accounts = [SELECT Id, Name FROM Account]"));
        }
        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expression=({1})")
    public void testVariableDeclarationSoqlNextLine(String comment, String expression) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public void doSomething(Account a) {\n"
                    + comment
                    + "\n"
                    + "		"
                    + expression
                    + ";\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        assertUserClassHasNoDirectives();
        assertMethodHasNoDirectives();

        VariableDeclarationStatementsVertex variableDeclarationStatements =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.VARIABLE_DECLARATION_STATEMENTS)
                                .not(has(Schema.IS_STANDARD, true)));
        engineDirectives = variableDeclarationStatements.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_NEXT_LINE_1));

        SoqlExpressionVertex soqlExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.SOQL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(soqlExpression, DISABLE_NEXT_LINE_1);
    }

    public static Stream<Arguments> testVariableAssignmentSoqlNextLine() {
        List<Arguments> arguments = new ArrayList<>();
        for (String engineDirective : DISABLE_NEXT_LINE_RULE_1_DIRECTIVES) {
            arguments.add(
                    Arguments.of(engineDirective, "accounts = [SELECT Id, Name FROM Account]"));
        }
        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expression=({1})")
    public void testVariableAssignmentSoqlNextLine(String comment, String expression) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public void doSomething(Account a) {\n"
                    + "		List<Account> accounts;\n"
                    + comment
                    + "\n"
                    + "		"
                    + expression
                    + ";\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        assertUserClassHasNoDirectives();
        assertMethodHasNoDirectives();

        ExpressionStatementVertex expressionStatement =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.EXPRESSION_STATEMENT)
                                .not(has(Schema.IS_STANDARD, true)));
        engineDirectives = expressionStatement.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_NEXT_LINE_1));

        SoqlExpressionVertex soqlExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.SOQL_EXPRESSION)
                                .not(has(Schema.IS_STANDARD, true)));
        assertDirectiveIsInherited(soqlExpression, DISABLE_NEXT_LINE_1);
    }

    public static Stream<Arguments> testDmlInsertNextLine() {
        List<Arguments> arguments = new ArrayList<>();

        List<String> operations =
                Arrays.asList(
                        "delete a1",
                        "insert a1",
                        "merge a1 a2",
                        "undelete a1",
                        "update a1",
                        "upsert a1");
        // Use the disable next line stream to create new permutations with the dml operations
        for (String engineDirective : DISABLE_NEXT_LINE_RULE_1_DIRECTIVES) {
            for (String operation : operations) {
                arguments.add(Arguments.of(engineDirective, operation));
            }
        }

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-operation=({1})")
    public void testDmlInsertNextLine(String comment, String operation) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public void doSomething(Account a1, Account a2) {\n"
                    + comment
                    + "\n"
                    + "		"
                    + operation
                    + ";\n"
                    + "	}\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        List<EngineDirective> engineDirectives;

        assertUserClassHasNoDirectives();
        assertMethodHasNoDirectives();

        DmlStatementVertex dmlStatementVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(
                                        NodeType.DML_DELETE_STATEMENT,
                                        ASTConstants.NodeType.DML_INSERT_STATEMENT,
                                        ASTConstants.NodeType.DML_MERGE_STATEMENT,
                                        ASTConstants.NodeType.DML_UNDELETE_STATEMENT,
                                        ASTConstants.NodeType.DML_UPDATE_STATEMENT,
                                        ASTConstants.NodeType.DML_UPSERT_STATEMENT)
                                .not(has(Schema.IS_STANDARD, true)));
        engineDirectives = dmlStatementVertex.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_NEXT_LINE_1));
    }

    public static Stream<Arguments> testDisableStackDirective() {
        List<Arguments> arguments = new ArrayList<>();

        for (String engineDirective : DISABLE_STACK_RULE_1_DIRECTIVES) {
            arguments.add(Arguments.of(engineDirective));
        }

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource(value = "testDisableStackDirective")
    @ParameterizedTest(name = "{displayName}: directive=({0})")
    public void testDisableStackDirectiveStartMethod(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    "
                    + directive
                    + "\n"
                    + "    public static void doSomething() {\n"
                    + "    	doSomethingElse();\n"
                    + "    }\n"
                    + "    public static void doSomethingElse() {\n"
                    + "    	System.debug('Hello');\n"
                    + "    }\n"
                    + "}"
        };

        List<EngineDirective> engineDirectives = new ArrayList<>();
        VertexPredicate predicate =
                new AbstractVisitingVertexPredicate() {
                    @Override
                    public boolean test(BaseSFVertex vertex) {
                        if (vertex instanceof MethodCallExpressionVertex) {
                            MethodCallExpressionVertex methodCallExpression =
                                    (MethodCallExpressionVertex) vertex;
                            if (methodCallExpression.getFullMethodName().equals("System.debug")) {
                                engineDirectives.addAll(
                                        ContextProviders.ENGINE_DIRECTIVE_CONTEXT
                                                .get()
                                                .getEngineDirectiveContext()
                                                .getEngineDirectives());
                            }
                        }
                        return true;
                    }
                };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPathExpanderConfig expanderConfig =
                ApexPathExpanderConfig.Builder.get()
                        .expandMethodCalls(true)
                        .withVertexPredicate(predicate)
                        .build();
        TestUtil.getApexPaths(config, expanderConfig, "doSomething");

        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_STACK_1));
    }

    /**
     * This is a test for W-11120894, wherein the sfge-disable-stack engine directive wasn't working
     * for methods with exceptions inside nested forked method calls. The attempt to clone the
     * annotation during path generation was causing an exception, which was overriding the
     * functionality of the annotation itself. This test recreates such a scenario, then makes sure
     * the associated with it were correctly generated and contain the expected annotation.
     */
    @MethodSource(value = "testDisableStackDirective")
    @ParameterizedTest(name = "{displayName}: directive=({0})")
    public void testDisableStackAnnotationWithForkedExceptionPaths(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + directive
                    + "\n"
                    + "    public static void doSomething(boolean enterBranch, boolean throwException) {\n"
                    + "        ExceptionThrower helper = new ExceptionThrower();\n"
                    + "        if (enterBranch) {\n"
                    + "            ExceptionThrower.throwExceptionIfAsked(throwException);\n"
                    + "        }\n"
                    + "        System.debug('hello');\n"
                    + "    }\n"
                    + "}",
            "public class ExceptionThrower {\n"
                    + "    public void throwExceptionIfAsked(boolean isAsked) {\n"
                    + "        if (isAsked) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}"
        };
        List<EngineDirective> engineDirectives = new ArrayList<>();
        VertexPredicate predicate =
                new AbstractVisitingVertexPredicate() {
                    @Override
                    public boolean test(BaseSFVertex vertex) {
                        if (vertex instanceof MethodCallExpressionVertex) {
                            MethodCallExpressionVertex methodCallExpression =
                                    (MethodCallExpressionVertex) vertex;
                            if (methodCallExpression.getFullMethodName().equals("System.debug")) {
                                engineDirectives.addAll(
                                        ContextProviders.ENGINE_DIRECTIVE_CONTEXT
                                                .get()
                                                .getEngineDirectiveContext()
                                                .getEngineDirectives());
                            }
                        }
                        return true;
                    }
                };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPathExpanderConfig expanderConfig =
                ApexPathExpanderConfig.Builder.get()
                        .expandMethodCalls(true)
                        .withVertexPredicate(predicate)
                        .build();
        TestUtil.getApexPaths(config, expanderConfig, "doSomething");
        MatcherAssert.assertThat(engineDirectives, hasItem(DISABLE_STACK_1));
    }

    @MethodSource(value = "testDisableStackDirective")
    @ParameterizedTest(name = "{displayName}: directive=({0})")
    public void testDisableStackAnnotationIntermediateMethod(String directive) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "    	doSomethingElse1();\n"
                    + "    }\n"
                    + "    public static void doSomethingElse1() {\n"
                    + "    	doSomethingElse2();\n"
                    + "    }\n"
                    + "    "
                    + directive
                    + "\n"
                    + "    public static void doSomethingElse2() {\n"
                    + "    	System.debug('Hello');\n"
                    + "    }\n"
                    + "}"
        };

        List<EngineDirective> engineDirectives = new ArrayList<>();
        VertexPredicate predicate =
                new AbstractVisitingVertexPredicate() {
                    @Override
                    public boolean test(BaseSFVertex vertex) {
                        if (vertex instanceof MethodCallExpressionVertex) {
                            MethodCallExpressionVertex methodCallExpression =
                                    (MethodCallExpressionVertex) vertex;
                            if (methodCallExpression.getFullMethodName().equals("System.debug")) {
                                engineDirectives.addAll(
                                        ContextProviders.ENGINE_DIRECTIVE_CONTEXT
                                                .get()
                                                .getEngineDirectiveContext()
                                                .getEngineDirectives());
                            }
                        }
                        return false;
                    }
                };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPathExpanderConfig expanderConfig =
                ApexPathExpanderConfig.Builder.get()
                        .expandMethodCalls(true)
                        .withVertexPredicate(predicate)
                        .build();
        TestUtil.getApexPaths(config, expanderConfig, "doSomething");

        MatcherAssert.assertThat(engineDirectives, contains(DISABLE_STACK_1));
    }

    void assertUserClassHasNoDirectives() {
        UserClassVertex userClass =
                SFVertexFactory.load(
                        g, g.V().hasLabel(NodeType.USER_CLASS).not(has(Schema.IS_STANDARD, true)));
        // It's not hoisted to the class
        MatcherAssert.assertThat(userClass.getEngineDirectives(), empty());
    }

    void assertMethodHasNoDirectives() {
        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(NodeType.METHOD)
                                .has(Schema.NAME, "doSomething")
                                .not(has(Schema.IS_STANDARD, true)));
        // It's not hoisted to the method
        MatcherAssert.assertThat(method.getEngineDirectives(), empty());
    }

    void assertDirectiveIsInherited(BaseSFVertex vertex, EngineDirective engineDirective) {
        // The vertex does not have a direct directive
        List<EngineDirective> engineDirectives = vertex.getEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, empty());

        // tThe vertex inherits the class annotation
        engineDirectives = vertex.getAllEngineDirectives();
        MatcherAssert.assertThat(engineDirectives, contains(engineDirective));
    }
}

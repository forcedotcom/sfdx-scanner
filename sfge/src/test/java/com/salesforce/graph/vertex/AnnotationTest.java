package com.salesforce.graph.vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.directive.EngineDirectiveCommand;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AnnotationTest {
    private static final Logger LOGGER = LogManager.getLogger(AnnotationTest.class);

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testClassAnnotation() {
        String[] sourceCode = {
            "@SuppressWarnings('sfge-disable')\n"
                    + "@TestVisible\n"
                    + "public class MyClass {\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        UserClassVertex userClass =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .not(has(Schema.IS_STANDARD, true)));

        List<AnnotationVertex> annotations;

        annotations = userClass.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(2)));
        assertContainsSuppressedWarning(annotations, EngineDirectiveCommand.DISABLE.getToken());
        assertContainsTestVisible(annotations);
    }

    @Test
    public void testMultipleClassAnnotations() {
        String[] sourceCode = {
            "@SuppressWarnings('sfge-disable')\n"
                    + "@SuppressWarnings('sfge-disable-stack')\n"
                    + "public class MyClass {\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        UserClassVertex userClass =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .not(has(Schema.IS_STANDARD, true)));

        List<AnnotationVertex> annotations;

        annotations = userClass.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(2)));
        assertContainsSuppressedWarning(annotations, EngineDirectiveCommand.DISABLE.getToken());
        assertContainsSuppressedWarning(
                annotations, EngineDirectiveCommand.DISABLE_STACK.getToken());
    }

    @Test
    public void testMethodAnnotation() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    @SuppressWarnings('Warning2')\n"
                    + "    public static void doSomething() {\n"
                    + "    }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        List<AnnotationVertex> annotations;
        MethodVertex methodVertex =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "doSomething"));
        annotations = methodVertex.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        assertContainsSuppressedWarning(annotations, "Warning2");
    }

    @Test
    public void testGetAllAnnotations() {
        String[] sourceCode = {
            "@SuppressWarnings('Warning1')\n"
                    + "@TestVisible\n"
                    + "public class MyClass {\n"
                    + "    @SuppressWarnings('Warning2')\n"
                    + "    public static void doSomething() {\n"
                    + "       @SuppressWarnings('Warning3')\n"
                    + "       List<Account> accounts = [\n"
                    + "           SELECT Id, Name\n"
                    + "           FROM Account\n"
                    + "       ];\n"
                    + "    }\n"
                    + "	@SuppressWarnings('Warning4')\n"
                    + "   public class InnerClass {\n"
                    + "    @SuppressWarnings('Warning5')\n"
                    + "    public static void doSomethingInner() {\n"
                    + "       @SuppressWarnings('Warning6')\n"
                    + "       List<Account> accounts = [\n"
                    + "           SELECT Id, Name\n"
                    + "           FROM Account\n"
                    + "       ];\n"
                    + "    }\n"
                    + "   }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        UserClassVertex myClass =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyClass"));

        List<AnnotationVertex> annotations;
        SoqlExpressionVertex soqlExpression;
        MethodVertex methodVertex;

        annotations = myClass.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(2)));
        assertContainsSuppressedWarning(annotations, "Warning1");
        assertContainsTestVisible(annotations);

        annotations = myClass.getAllAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(2)));

        methodVertex =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "doSomething"));
        annotations = methodVertex.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        assertContainsSuppressedWarning(annotations, "Warning2");

        annotations = methodVertex.getAllAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(3)));

        soqlExpression =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.SOQL_EXPRESSION)
                                .has(Schema.BEGIN_LINE, 7));
        annotations = soqlExpression.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        assertContainsSuppressedWarning(annotations, "Warning3");

        annotations = soqlExpression.getAllAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(4)));
        assertContainsSuppressedWarning(annotations, "Warning1");
        assertContainsTestVisible(annotations);
        assertContainsSuppressedWarning(annotations, "Warning2");
        assertContainsSuppressedWarning(annotations, "Warning3");

        UserClassVertex innerClass =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "InnerClass"));
        annotations = innerClass.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        assertContainsSuppressedWarning(annotations, "Warning4");

        annotations = innerClass.getAllAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(3)));
        assertContainsSuppressedWarning(annotations, "Warning1");
        assertContainsTestVisible(annotations);
        assertContainsSuppressedWarning(annotations, "Warning4");

        methodVertex =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "doSomethingInner"));
        annotations = methodVertex.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        assertContainsSuppressedWarning(annotations, "Warning5");

        soqlExpression =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.SOQL_EXPRESSION)
                                .has(Schema.BEGIN_LINE, 17));
        annotations = soqlExpression.getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        assertContainsSuppressedWarning(annotations, "Warning6");

        annotations = soqlExpression.getAllAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(5)));
        assertContainsSuppressedWarning(annotations, "Warning1");
        assertContainsTestVisible(annotations);
        assertContainsSuppressedWarning(annotations, "Warning4");
        assertContainsSuppressedWarning(annotations, "Warning5");
        assertContainsSuppressedWarning(annotations, "Warning6");
    }

    private void assertContainsTestVisible(List<AnnotationVertex> annotations) {
        MatcherAssert.assertThat(
                annotations.stream()
                        .filter(a -> a.getName().equals("TestVisible"))
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    private void assertContainsSuppressedWarning(
            List<AnnotationVertex> annotations, String warning) {
        MatcherAssert.assertThat(
                annotations.stream()
                        .filter(
                                a ->
                                        a.getName().equals("SuppressWarnings")
                                                && a.getParameters()
                                                        .get(0)
                                                        .getValue()
                                                        .get()
                                                        .equals(warning))
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }
}

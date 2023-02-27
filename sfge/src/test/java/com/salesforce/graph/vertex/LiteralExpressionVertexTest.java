package com.salesforce.graph.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LiteralExpressionVertexTest {
    private GraphTraversalSource g;

    // spotless:off
    private final String isLiterallyXSourceTemplate =
            "public with sharing class Beep {\n"
                    + "    public void doNothing() {\n"
                    + "        %s\n"
                    + "    }\n"
                    + "}";
    // spotless:on

    @BeforeEach
    public void setup() {
        g = TestUtil.getGraph();
    }

    @Nested
    public class TrueTest {
        @CsvSource({
            "boolean b = true;, true",
            "boolean b = false;, false",
            "boolean b = !true;, false",
            "boolean b = !false;, true",
            "boolean b = !!true;, true",
            "boolean b = !!false;, false",
            "Account acc = new Account();, false"
        })
        @ParameterizedTest(name = "{displayName}: {0}")
        public void test_isLiterallyTrue(String expression, String expectationString) {
            // Build the graph.
            TestUtil.buildGraph(g, String.format(isLiterallyXSourceTemplate, expression));
            // Get the VariableDeclarationVertex at line 3.
            VariableDeclarationVertex vdv =
                    SFVertexFactory.load(
                            g,
                            g.V()
                                    .hasLabel(NodeType.VARIABLE_DECLARATION)
                                    .has(Schema.BEGIN_LINE, 3));
            // The target vertex is always the declaration's first child.
            BaseSFVertex target = vdv.getChild(0);
            Boolean expectation = expectationString.equalsIgnoreCase("true");
            assertEquals(expectation, LiteralExpressionVertex.True.isLiterallyTrue(target));
        }
    }

    @Nested
    public class FalseTest {
        @CsvSource({
            "boolean b = true;, false",
            "boolean b = false;, true",
            "boolean b = !true;, true",
            "boolean b = !false;, false",
            "boolean b = !!true;, false",
            "boolean b = !!false;, true",
            "Account acc = new Account();, false"
        })
        @ParameterizedTest(name = "{displayName}: {0}")
        public void test_isLiterallyFalse(String expression, String expectationString) {
            // Build the graph.
            TestUtil.buildGraph(g, String.format(isLiterallyXSourceTemplate, expression));
            // Get the VariableDeclarationVertex at line 3.
            VariableDeclarationVertex vdv =
                    SFVertexFactory.load(
                            g,
                            g.V()
                                    .hasLabel(NodeType.VARIABLE_DECLARATION)
                                    .has(Schema.BEGIN_LINE, 3));
            // The target vertex is always the declaration's first child.
            BaseSFVertex target = vdv.getChild(0);
            Boolean expectation = expectationString.equalsIgnoreCase("true");
            assertEquals(expectation, LiteralExpressionVertex.False.isLiterallyFalse(target));
        }
    }
}

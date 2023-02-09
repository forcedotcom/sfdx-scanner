package com.salesforce.rules.fls.apex.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StandardConditionDecomposerTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        g = TestUtil.getGraph();
    }

    // spotless off
    private static final String SOURCE_CODE =
            "public class Foo {\n"
                    + "    public boolean bar(boolean b) {\n"
                    + "        if (%s) {\n"
                    + "            return true;\n"
                    + "        }\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}";
    // spotless on
    private static final Map<String, Boolean[]> VARIANTS_MAP =
            Stream.of(
                            new Object[][] {
                                {"b", new Boolean[] {false, true}},
                                {"!b", new Boolean[] {true, false}},
                                {"!!b", new Boolean[] {false, true}},
                                {"b == true", new Boolean[] {false, true}},
                                {"b == !false", new Boolean[] {false, true}},
                                {"b == !!true", new Boolean[] {false, true}},
                                {"b == false", new Boolean[] {true, false}},
                                {"b == !true", new Boolean[] {true, false}},
                                {"b == !!false", new Boolean[] {true, false}},
                                {"b != false", new Boolean[] {false, true}},
                                {"b != !true", new Boolean[] {false, true}},
                                {"b != !!false", new Boolean[] {false, true}},
                                {"b != true", new Boolean[] {true, false}},
                                {"b != !false", new Boolean[] {true, false}},
                                {"b != !!true", new Boolean[] {true, false}},
                            })
                    .collect(
                            Collectors.toMap(
                                    data -> (String) data[0], data -> (Boolean[]) data[1]));

    private static Stream<Arguments> params(int idx) {
        return VARIANTS_MAP.keySet().stream()
                .map(variant -> Arguments.of(variant, VARIANTS_MAP.get(variant)[idx]));
    }

    private static Stream<Arguments> params_positive() {
        return params(0);
    }

    private static Stream<Arguments> params_negative() {
        return params(1);
    }

    @MethodSource("params_positive")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void decomposePositiveCondition(String condition, Boolean expectingEmpty) {
        TestUtil.buildGraph(g, String.format(SOURCE_CODE, condition));
        StandardConditionVertex.Unknown scvu =
                SFVertexFactory.load(g, g.V().hasLabel(NodeType.STANDARD_CONDITION));
        List<BaseSFVertex> res =
                StandardConditionDecomposer.decomposeStandardCondition(scvu.convertToPositive());
        assertEquals(expectingEmpty, res.isEmpty());
        if (!res.isEmpty()) {
            assertTrue(res.get(0) instanceof VariableExpressionVertex);
            VariableExpressionVertex vev = (VariableExpressionVertex) res.get(0);
            assertEquals("b", vev.getFullName());
        }
    }

    @MethodSource("params_negative")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void decomposeNegativeCondition(String condition, Boolean expectingEmpty) {
        TestUtil.buildGraph(g, String.format(SOURCE_CODE, condition));
        StandardConditionVertex.Unknown scvu =
                SFVertexFactory.load(g, g.V().hasLabel(NodeType.STANDARD_CONDITION));
        List<BaseSFVertex> res =
                StandardConditionDecomposer.decomposeStandardCondition(scvu.convertToNegative());
        assertEquals(expectingEmpty, res.isEmpty());
        if (!res.isEmpty()) {
            assertTrue(res.get(0) instanceof VariableExpressionVertex);
            VariableExpressionVertex vev = (VariableExpressionVertex) res.get(0);
            assertEquals("b", vev.getFullName());
        }
    }
}

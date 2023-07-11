package com.salesforce.graph.build;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.apex.jorje.AstNodeWrapper;
import com.salesforce.apex.jorje.JorjeUtil;
import com.salesforce.graph.Schema;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomerApexVertexBuilderTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimple() {
        AstNodeWrapper<?> compilation = JorjeUtil.compileApexFromString("public class MyClass {}");
        Util.CompilationDescriptor compilationDescriptor =
                new Util.CompilationDescriptor("SimpleSource", compilation);

        CustomerApexVertexBuilder bvb =
                new CustomerApexVertexBuilder(g, Collections.singletonList(compilationDescriptor));
        bvb.build();

        // Validate the expected number of vertices
        assertEquals(Long.valueOf(9), g.V().not(has(Schema.IS_STANDARD, true)).count().next());
        assertEquals(
                Long.valueOf(1),
                g.V()
                        .hasLabel(NodeType.USER_CLASS)
                        .not(has(Schema.IS_STANDARD, true))
                        .count()
                        .next());
        assertEquals(
                Long.valueOf(3),
                g.V().hasLabel(NodeType.METHOD).not(has(Schema.IS_STANDARD, true)).count().next());
        assertEquals(
                Long.valueOf(4),
                g.V()
                        .hasLabel(NodeType.MODIFIER_NODE)
                        .not(has(Schema.IS_STANDARD, true))
                        .count()
                        .next());
        assertEquals(
                Long.valueOf(1),
                g.V()
                        .hasLabel(NodeType.USER_CLASS_METHODS)
                        .not(has(Schema.IS_STANDARD, true))
                        .count()
                        .next());
        // TODO: We probably should exclude this
        // assertEquals(Long.valueOf(1),
        // g.V().hasLabel(NodeType.BRIDGE_METHOD_CREATOR).count().next());

        // Validate some properties of the UserClass vertex
        Map<Object, Object> properties =
                g.V()
                        .hasLabel(NodeType.USER_CLASS)
                        .not(has(Schema.IS_STANDARD, true))
                        .elementMap()
                        .next();
        assertEquals("MyClass", properties.get(Schema.DEFINING_TYPE));
        assertEquals("MyClass", properties.get(Schema.NAME));
        assertEquals(1, properties.get(Schema.BEGIN_LINE));
        assertEquals("SimpleSource", properties.get(Schema.FILE_NAME));
    }

    /**
     * Verifies that the special properties associated with {@link
     * com.salesforce.graph.vertex.UserTriggerVertex} are set.
     */
    @Test
    public void testTriggers() {
        // spotless:off
        String triggerSource =
            "trigger AccountBefore on Account (before insert, before update) {\n"
          + "    if (Trigger.isInsert) {\n"
          + "        System.debug('Some debug-worthy message');\n"
          + "    }\n"
          + "}\n";
        // spotless:on

        AstNodeWrapper<?> triggerComp = JorjeUtil.compileApexFromString(triggerSource);
        Util.CompilationDescriptor triggerDesc =
                new Util.CompilationDescriptor("TestCode1", triggerComp);

        CustomerApexVertexBuilder vertexBuilder =
                new CustomerApexVertexBuilder(g, Collections.singletonList(triggerDesc));
        vertexBuilder.build();

        // Validate that a vertex was created for the trigger.
        Map<Object, Object> triggerVertex =
                g.V().hasLabel(NodeType.USER_TRIGGER).elementMap().next();
        assertEquals("AccountBefore", triggerVertex.get(Schema.NAME));
        assertEquals("AccountBefore", triggerVertex.get(Schema.DEFINING_TYPE));
        assertEquals("Account", triggerVertex.get(Schema.TARGET_NAME));
        String rawUsages = (String) triggerVertex.get(Schema.USAGES);
        Set<String> usages =
                new HashSet<>(
                        Arrays.asList(rawUsages.substring(1, rawUsages.length() - 1).split(", ")));
        assertTrue(usages.contains("BEFORE INSERT"));
        assertTrue(usages.contains("BEFORE UPDATE"));
        // validate that the trigger's contents were added as an `invoke()` method.
        Map<Object, Object> invokeVertex =
                g.V()
                        .hasLabel(NodeType.USER_TRIGGER)
                        .out(Schema.CHILD)
                        .hasLabel(NodeType.METHOD)
                        .has(Schema.NAME, "invoke")
                        .elementMap()
                        .next();
        assertEquals("AccountBefore", invokeVertex.get(Schema.DEFINING_TYPE));
    }

    @Test
    public void testCaseSafeProperties() {
        String interfaceSource =
                "pUBliC INtErfAce soMeINTERfAcE {\n" + "	voId sOMEMetHOd();\n" + "}";
        String spongeBobCasedSource =
                "pUbLiC cLaSs MyClAsS eXtEnDs PaReNtClAsS iMpLeMeNtS sOmEiNtErFaCe {\n"
                        + "	pUbLiC vOiD SoMeMethod() {\n"
                        + "		SyStEm.DeBuG(LoGgInGlEvEl.ErRoR, 'Beep');\n"
                        + "	}\n"
                        + "}";
        AstNodeWrapper<?> comp1 = JorjeUtil.compileApexFromString(interfaceSource);
        Util.CompilationDescriptor compDesc1 = new Util.CompilationDescriptor("TestCode1", comp1);
        AstNodeWrapper<?> comp2 = JorjeUtil.compileApexFromString(spongeBobCasedSource);
        Util.CompilationDescriptor compDesc2 = new Util.CompilationDescriptor("TestCode2", comp2);

        CustomerApexVertexBuilder bvb =
                new CustomerApexVertexBuilder(g, Arrays.asList(compDesc1, compDesc2));
        bvb.build();

        // Validate the case-safe properties of the vertices.
        Map<Object, Object> userClassVertex =
                g.V()
                        .hasLabel(NodeType.USER_CLASS)
                        .not(has(Schema.IS_STANDARD, true))
                        .elementMap()
                        .next();
        assertEquals(
                "myclass",
                userClassVertex.get(Schema.NAME + CaseSafePropertyUtil.CASE_SAFE_SUFFIX));
        assertEquals(
                "parentclass",
                userClassVertex.get(
                        Schema.SUPER_CLASS_NAME + CaseSafePropertyUtil.CASE_SAFE_SUFFIX));
        assertEquals(
                "someinterface",
                ((Object[])
                                userClassVertex.get(
                                        Schema.INTERFACE_NAMES
                                                + CaseSafePropertyUtil.CASE_SAFE_SUFFIX))
                        [0]);

        Map<Object, Object> userInterfaceVertex =
                g.V().hasLabel(NodeType.USER_INTERFACE).elementMap().next();
        assertEquals(
                "someinterface",
                userInterfaceVertex.get(Schema.NAME + CaseSafePropertyUtil.CASE_SAFE_SUFFIX));
    }

    @Test
    public void testMethodSignature() {
        String classWithMethods =
                "public class MyClass {\n"
                        + "   public void noParams_ReturnsVoid() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'Beep boop bop');\n"
                        + "   }\n"
                        + "   public void intParam_ReturnsVoid(integer int) {\n"
                        + "       System.debug(LoggingLevel.ERROR, int);\n"
                        + "   }\n"
                        + "   public void intBoolParams_ReturnsVoid(integer int, boolean bool) {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'does not matter');\n"
                        + "   }\n"
                        + "   public void listParam_ReturnsVoid(List<integer> ints) {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'this does not matter');\n"
                        + "   }\n"
                        + "   public boolean noParams_ReturnsBool() {\n"
                        + "       return true;\n"
                        + "   }\n"
                        + "   public boolean intParam_ReturnsBool(integer int) {\n"
                        + "       return int == 5;\n"
                        + "   }\n"
                        + "   public List<integer> intParams_ReturnsIntList(integer int1, integer int2, integer int3) {\n"
                        + "       return new List<integer>{int1, int2, int3};\n"
                        + "   }\n"
                        + "}";

        VertexCacheProvider.get().initialize(g);

        AstNodeWrapper<?> compilation = JorjeUtil.compileApexFromString(classWithMethods);
        Util.CompilationDescriptor compilationDescriptor =
                new Util.CompilationDescriptor("TestCode", compilation);

        CustomerApexVertexBuilder bvb =
                new CustomerApexVertexBuilder(g, Collections.singletonList(compilationDescriptor));
        bvb.build();

        List<MethodVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        g.V()
                                .hasLabel(NodeType.METHOD)
                                .has(Schema.DEFINING_TYPE, "MyClass")
                                .not(has(Schema.CONSTRUCTOR, true))
                                .not(has(Schema.NAME, Schema.INSTANCE_CONSTRUCTOR_CANONICAL_NAME))
                                .not(has(Schema.NAME, Schema.STATIC_CONSTRUCTOR_CANONICAL_NAME))
                                .not(has(Schema.NAME, "clone"))
                                .not(has(Schema.IS_STANDARD, true))
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
        List<String> methodSigs =
                vertices.stream().map(v -> v.getSignature()).collect(Collectors.toList());
        MatcherAssert.assertThat(
                methodSigs,
                equalTo(
                        Arrays.asList(
                                "void noParams_ReturnsVoid()",
                                "void intParam_ReturnsVoid(Integer)",
                                "void intBoolParams_ReturnsVoid(Integer,Boolean)",
                                "void listParam_ReturnsVoid(List<Integer>)",
                                "Boolean noParams_ReturnsBool()",
                                "Boolean intParam_ReturnsBool(Integer)",
                                "List<Integer> intParams_ReturnsIntList(Integer,Integer,Integer)")));
    }


    @Test
    public void testEndScopesOrderDeepLoop() {
        // spotless:off
        String classWithLoops =
            "public class MyClass {\n"
                + "     public void foo(List<Integer> myList) {\n"
                + "         for (Integer i : myList) {\n"
                + "             for (int j = 0; j < myList.size(); j++) {\n"
                + "                 int k = 0;\n"
                + "             }\n"
                + "         }\n"
                + "     }\n"
                + "}\n"
            ;
        // spotless:on

        VertexCacheProvider.get().initialize(g);

        AstNodeWrapper<?> compilation = JorjeUtil.compileApexFromString(classWithLoops);
        Util.CompilationDescriptor compilationDescriptor = new Util.CompilationDescriptor("TestCode", compilation);

        CustomerApexVertexBuilder bvb = new CustomerApexVertexBuilder(g, Collections.singletonList(compilationDescriptor));

        bvb.build();

        BaseSFVertex k = SFVertexFactory.loadVertices(g, g.V().hasLabel(NodeType.VARIABLE_DECLARATION_STATEMENTS).has(Schema.BEGIN_LINE, 5)).get(0);
        List<String> kEndScopes = k.getEndScopes();

        assertEquals(kEndScopes, Arrays.asList(
            NodeType.BLOCK_STATEMENT,
            NodeType.FOR_LOOP_STATEMENT,
            NodeType.BLOCK_STATEMENT,
            NodeType.FOR_EACH_STATEMENT,
            NodeType.BLOCK_STATEMENT
        ));


    }


    @Test
    public void testEndScopesOrderOutsideLoop() {
        // spotless:off
        String classWithLoops =
            "public class MyClass {\n"
                + "     public void foo(List<Integer> myList) {\n"
                + "         for (Integer i : myList) {\n"
                + "             for (int j = 0; j < myList.size(); j++) {\n"
                + "                 int k = 0;\n"
                + "             }\n"
                + "             boolean b = false;\n"
                + "         }\n"
                + "     }\n"
                + "}\n"
            ;
        // spotless:on

        VertexCacheProvider.get().initialize(g);

        AstNodeWrapper<?> compilation = JorjeUtil.compileApexFromString(classWithLoops);
        Util.CompilationDescriptor compilationDescriptor = new Util.CompilationDescriptor("TestCode", compilation);

        CustomerApexVertexBuilder bvb = new CustomerApexVertexBuilder(g, Collections.singletonList(compilationDescriptor));

        bvb.build();

        BaseSFVertex k = SFVertexFactory.loadVertices(g, g.V().hasLabel(NodeType.VARIABLE_DECLARATION_STATEMENTS).has(Schema.BEGIN_LINE, 5)).get(0);
        List<String> kEndScopes = k.getEndScopes();

        assertEquals(kEndScopes, Arrays.asList(
            NodeType.BLOCK_STATEMENT,
            NodeType.FOR_LOOP_STATEMENT
        ));



        BaseSFVertex b = SFVertexFactory.loadVertices(g, g.V().hasLabel(NodeType.VARIABLE_DECLARATION_STATEMENTS).has(Schema.BEGIN_LINE, 7)).get(0);
        List<String> bEndScopes = b.getEndScopes();

        assertEquals(bEndScopes, Arrays.asList(
            NodeType.BLOCK_STATEMENT,
            NodeType.FOR_EACH_STATEMENT,
            NodeType.BLOCK_STATEMENT
        ));

    }


}

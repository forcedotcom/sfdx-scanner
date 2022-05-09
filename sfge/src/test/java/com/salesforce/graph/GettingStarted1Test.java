package com.salesforce.graph;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.AstNodeWrapper;
import com.salesforce.apex.jorje.JorjeUtil;
import com.salesforce.graph.build.CustomerApexVertexBuilder;
import com.salesforce.graph.build.Util;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ModifierNodeVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class demonstrates basic Tinkerpop principles, Gremlin principles, and some of the
 * additional abstractions and utilities available in the project.
 *
 * <p>See https://tinkerpop.apache.org/docs/current/tutorials/getting-started/
 */
public class GettingStarted1Test {
    private GraphTraversalSource g;

    private static final String FIRST_VERTEX_LABEL = "MyFirstVertex";
    private static final String SECOND_VERTEX_LABEL = "MySecondVertex";
    private static final String VERTEX_BOOLEAN_PROP = "MyBooleanProp";
    private static final String VERTEX_LONG_PROP = "MyLongProp";
    private static final String VERTEX_STRING_PROP = "MyStringProp";

    /**
     * This method creates a new graph before each test. The drop method is called for cases when
     * the graph is held in a persistent store.
     */
    @BeforeEach
    public void setup() {
        this.g = GraphUtil.getGraph();
    }

    // *********************************************************************************************************
    // BEGIN GENERIC GREMLIN INFO
    // *********************************************************************************************************
    @Test
    public void addingAndLoadingVertices() {
        // Normally the VertexCache is initialized when the Apex is compiled. These fundamental
        // tests don't use
        // that infrastructure and must manually initialize the VertexCache.
        VertexCacheProvider.get().initialize(g);

        // The graph starts off with zero vertices
        MatcherAssert.assertThat(g.V().count().next(), equalTo(Long.valueOf(0)));

        // Add a vertex, make sure to call #next()
        Vertex vertex = g.addV(FIRST_VERTEX_LABEL).next();

        // In memory ids are Longs starting at 0
        MatcherAssert.assertThat((Long) vertex.id(), greaterThan(Long.valueOf(-1)));

        // Query for the number of vertices in the graph
        MatcherAssert.assertThat(g.V().count().next(), equalTo(Long.valueOf(1)));

        Vertex loadedVertex;

        // Load a vertex by id
        loadedVertex = g.V(vertex.id()).next();
        MatcherAssert.assertThat(loadedVertex.id(), equalTo(vertex.id()));
        MatcherAssert.assertThat(loadedVertex.label(), equalTo(FIRST_VERTEX_LABEL));

        // Find a vertex by label
        loadedVertex = g.V().hasLabel(FIRST_VERTEX_LABEL).next();
        MatcherAssert.assertThat(loadedVertex.id(), equalTo(vertex.id()));
        MatcherAssert.assertThat(loadedVertex.label(), equalTo(FIRST_VERTEX_LABEL));

        // Next will throw if it can't find the label
        Exception caught = null;
        try {
            g.V().hasLabel("non-existent").next();
        } catch (Exception ex) {
            caught = ex;
        }
        MatcherAssert.assertThat(caught, instanceOf(NoSuchElementException.class));

        // Iterators protect against this
        GraphTraversal<Vertex, Vertex> traversal = g.V().hasLabel("non-existent");
        MatcherAssert.assertThat(traversal.hasNext(), equalTo(false));

        // Our helper class SFVertexFactory avoids an exception if the vertex can't be found.
        BaseSFVertex sfVertex = SFVertexFactory.loadSingleOrNull(g, g.V().hasLabel("non-existent"));
        MatcherAssert.assertThat(sfVertex, is(nullValue()));

        // Use SFVertexFactory#create in cases where you want an exception to throw if the vertex is
        // missing
        caught = null;
        try {
            SFVertexFactory.load(g, g.V().hasLabel("non-existent"));
        } catch (Exception ex) {
            caught = ex;
        }
        MatcherAssert.assertThat(caught, instanceOf(NoSuchElementException.class));
    }

    @Test
    public void vertexProperties() {
        VertexCacheProvider.get().initialize(g);

        // Add a vertex with a property
        Vertex vertex = g.addV(FIRST_VERTEX_LABEL).property(VERTEX_STRING_PROP, "a-string").next();
        Vertex loadedVertex;

        Map<Object, Object> vertexMap;

        // Check the properties on the vertex
        vertexMap = g.V(vertex.id()).elementMap().next();
        MatcherAssert.assertThat(vertexMap.get(VERTEX_STRING_PROP), equalTo("a-string"));

        // Query for a vertex using its properties
        vertexMap = g.V().has(VERTEX_STRING_PROP, "a-string").elementMap().next();
        MatcherAssert.assertThat(vertexMap.get(VERTEX_STRING_PROP), equalTo("a-string"));
    }

    @Test
    public void vertexPropertyEdgeCases() {
        VertexCacheProvider.get().initialize(g);

        // Add a vertex
        Vertex vertex = g.addV(FIRST_VERTEX_LABEL).next();
        Map<Object, Object> loadedVertex;

        // The label and id properties don't behave as expected. invoking Map#get("id") returns null
        loadedVertex = g.V(vertex.id()).elementMap().next();

        // The id key is an enum
        final Long foundId = (Long) loadedVertex.get(T.id);
        MatcherAssert.assertThat(foundId, greaterThan(Long.valueOf(-1)));

        // The label key is an enum
        final String foundLabel = (String) loadedVertex.get(T.label);
        MatcherAssert.assertThat(foundLabel, equalTo(FIRST_VERTEX_LABEL));

        // SFVertexFactory hides these issues from you
        BaseSFVertex sfVertex =
                SFVertexFactory.loadSingleOrNull(g, g.V().hasLabel(FIRST_VERTEX_LABEL));
        MatcherAssert.assertThat(sfVertex.getId(), greaterThan(Long.valueOf(-1)));
    }

    /**
     * There are several helper utilities to make gremlin easier. See {@link
     * com.salesforce.graph.vertex.SFVertexFactory} and {@link com.salesforce.graph.ops.GraphUtil}
     */
    @Test
    public void helperMethods() {
        VertexCacheProvider.get().initialize(g);

        // Use SFVertexFactory to load multiple vertices
        g.addV(FIRST_VERTEX_LABEL).next();
        g.addV(SECOND_VERTEX_LABEL).next();

        List<BaseSFVertex> vertices = SFVertexFactory.loadVertices(g, g.V());
        MatcherAssert.assertThat(vertices, hasSize(equalTo(2)));

        // SFVertexFactory uses reflection to load strongly typed objects, BaseSFVertex is used in
        // cases where a concrete
        // type can't be found
        g.addV(ASTConstants.NodeType.MODIFIER_NODE).property(Schema.BEGIN_LINE, 10).next();
        ModifierNodeVertex vertex =
                SFVertexFactory.load(g, g.V().hasLabel(ASTConstants.NodeType.MODIFIER_NODE));
        MatcherAssert.assertThat(vertex.getBeginLine(), equalTo(10));
    }

    @Test
    public void helperMethodEdgeCases() {
        VertexCacheProvider.get().initialize(g);

        // Use ASTConstants.NodeType to load vertices, but don't rely on the label after the vertex
        // is loaded, some
        // generic AST types are converted to more specific vertex types.

        Vertex vertex;
        // Add a String vertex
        vertex =
                g.addV(ASTConstants.NodeType.LITERAL_EXPRESSION)
                        .property(Schema.LITERAL_TYPE, "STRING")
                        .property(Schema.VALUE, "a-string-property")
                        .next();

        // It is loaded as a StringLiteralExpressionVertex, but its label is the generic
        // ASTConstants.NodeType.LITERAL_EXPRESSION
        LiteralExpressionVertex.SFString stringVertex = SFVertexFactory.load(g, g.V(vertex.id()));
        MatcherAssert.assertThat(stringVertex.getLiteral(), equalTo("a-string-property"));
        MatcherAssert.assertThat(
                stringVertex.getLabel(), equalTo(ASTConstants.NodeType.LITERAL_EXPRESSION));
        // Use instanceof to determine the correct type
        MatcherAssert.assertThat(stringVertex, instanceOf(LiteralExpressionVertex.SFString.class));

        // Eventually we might modify the graph to replace the jorje label, but using instanceof is
        // a future proof best practice
    }

    // *********************************************************************************************************
    // END GENERIC GREMLIN INFO
    // *********************************************************************************************************

    // *********************************************************************************************************
    // BEGIN APEX AST INFO
    // *********************************************************************************************************

    private static final String SIMPLE_CLASS =
            "public class SimpleClass {\n"
                    + "    public void methodWithNoControlFLow() {\n"
                    + "        String s = 'HelloFoo1';\n"
                    + // ASTVariableDeclarationStatements
                    "        System.debug(s);\n"
                    + // ASTExpressionStatement
                    "        s = 'HelloFoo2';\n"
                    + // ASTExpressionStatement
                    "        System.debug(s);\n"
                    + // ASTExpressionStatement
                    "    }\n"
                    + "}";

    /** We use the Jorje to compile. */
    @Test
    public void addingApexToTheGraph() {
        VertexCacheProvider.get().initialize(g);

        // Compile the apex
        AstNodeWrapper<?> node = JorjeUtil.compileApexFromString(SIMPLE_CLASS);
        Util.CompilationDescriptor compilationDescriptor =
                new Util.CompilationDescriptor("TestCode", node);

        // BaseVertexBuilder imports the AST into the gremlin graph, the imported graph is a 1:1
        // mapping of the AST
        // with some properties filtered out. See BaseVertexBuilder#FILTERED_OUT_PROPERTIES
        CustomerApexVertexBuilder vertexBuilder =
                new CustomerApexVertexBuilder(g, Collections.singletonList(compilationDescriptor));

        // The graph starts off with zero vertices
        MatcherAssert.assertThat(g.V().count().next(), equalTo(Long.valueOf(0)));

        vertexBuilder.build();

        // The graph now has one root vertex for the user class
        MatcherAssert.assertThat(
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS).count().next(),
                equalTo(Long.valueOf(1)));

        Vertex loadedVertex;
        loadedVertex = g.V().hasLabel(ASTConstants.NodeType.USER_CLASS).next();
        MatcherAssert.assertThat(loadedVertex.label(), equalTo(ASTConstants.NodeType.USER_CLASS));

        // Use SFVertexFactory to load the strongly typed object
        UserClassVertex userClassVertex =
                SFVertexFactory.load(g, g.V().hasLabel(ASTConstants.NodeType.USER_CLASS));
        MatcherAssert.assertThat(userClassVertex.getName(), equalTo("SimpleClass"));
    }

    @Test
    public void navigatingTheGraph() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, SIMPLE_CLASS, true);

        // Load all of the expression vertices in the graph, see SIMPLE_CLASS above to see what this
        // corresponds to
        List<ExpressionStatementVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.EXPRESSION_STATEMENT)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
        MatcherAssert.assertThat(vertices, hasSize(equalTo(3)));

        ExpressionStatementVertex expressionStatement;

        // The first expression contains the System.debug method
        expressionStatement = vertices.get(0);
        MatcherAssert.assertThat(expressionStatement.getBeginLine(), equalTo(4));

        // Navigate to the next sibling
        expressionStatement = expressionStatement.getNextSibling();
        MatcherAssert.assertThat(expressionStatement.getBeginLine(), equalTo(5));

        // The parent's parent is a method
        BaseSFVertex parent = expressionStatement.getParent().getParent();
        MatcherAssert.assertThat(parent, instanceOf(MethodVertex.class));
    }

    // *********************************************************************************************************
    // END APEX AST INFO
    // *********************************************************************************************************
}

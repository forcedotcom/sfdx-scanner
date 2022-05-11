package com.salesforce.graph;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.fail;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GettingStarted2Test {
    private GraphTraversalSource g;

    /**
     * This method creates a new graph before each test. The drop method is called for cases when
     * the graph is held in a persistent store.
     */
    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    // *********************************************************************************************************
    // BEGIN PATH INFO
    // *********************************************************************************************************
    private static final String SINGLE_PATH =
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

    @Test
    public void findingASimplePath() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, SINGLE_PATH, true);

        // Find the vertex that starts the method
        MethodVertex methodVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "methodWithNoControlFLow"));
        Long id = methodVertex.getId();

        // We could alternatively use the #getMethodVertex helper
        methodVertex = TestUtil.getMethodVertex(g, "SimpleClass", "methodWithNoControlFLow");
        MatcherAssert.assertThat(id, equalTo(methodVertex.getId()));

        // Figure out the path through the method
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);

        // There is only a single path through the method since there is no branching logic
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        // The path consists of the four top level vertices. #verticesInCurrentMethod returns the
        // vertices in the
        // current method. It's best to use ApexPathWalker to traverse all vertices. This example is
        // simple enough that it
        // works, but in most cases it is too simplistic.
        List<BaseSFVertex> vertices = path.verticesInCurrentMethod();
        MatcherAssert.assertThat(vertices, hasSize(equalTo(5)));
        MatcherAssert.assertThat(vertices.get(0), instanceOf(BlockStatementVertex.class));
        MatcherAssert.assertThat(
                vertices.get(1), instanceOf(VariableDeclarationStatementsVertex.class));
        MatcherAssert.assertThat(vertices.get(2), instanceOf(ExpressionStatementVertex.class));
        MatcherAssert.assertThat(vertices.get(3), instanceOf(ExpressionStatementVertex.class));
        MatcherAssert.assertThat(vertices.get(4), instanceOf(ExpressionStatementVertex.class));

        // We can also start at the last vertex in the method and ask for all paths that reach that
        // point
        // ask for all paths that reach the fourth vertex, and verify the same path is returned as
        // when #getForwardPaths
        // was used.
        paths = ApexPathUtil.getReversePaths(g, vertices.get(3));
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        vertices = path.verticesInCurrentMethod();
        MatcherAssert.assertThat(vertices, hasSize(equalTo(5)));
        MatcherAssert.assertThat(vertices.get(0), instanceOf(BlockStatementVertex.class));
        MatcherAssert.assertThat(
                vertices.get(1), instanceOf(VariableDeclarationStatementsVertex.class));
        MatcherAssert.assertThat(vertices.get(2), instanceOf(ExpressionStatementVertex.class));
        MatcherAssert.assertThat(vertices.get(3), instanceOf(ExpressionStatementVertex.class));
        MatcherAssert.assertThat(vertices.get(4), instanceOf(ExpressionStatementVertex.class));
    }

    private static final String MULTIPLE_PATHS =
            "public class ComplexClass {\n"
                    + "    public void methodWithControlFlow(boolean isError) {\n"
                    + "        if (isError) {\n"
                    + "           System.debug('an-error');\n"
                    + "        } else {\n"
                    + "           System.debug('not-an-error');\n"
                    + "        }\n"
                    + "        System.debug('done');\n"
                    + "    }\n"
                    + "}";

    // The MULTIPLE_PATHS class results in the following AST
    // <IfElseBlockStatement BeginLine='3' DefiningType='ComplexClass' ElseStatement='true'
    // EndLine='3' Image='' RealLoc='true'>
    //    <IfBlockStatement BeginLine='3' DefiningType='ComplexClass' EndLine='3' Image=''
    // RealLoc='true'>
    //        <StandardCondition BeginLine='3' DefiningType='ComplexClass' EndLine='3' Image=''
    // RealLoc='true'>
    //            <VariableExpression BeginLine='3' DefiningType='ComplexClass' EndLine='3'
    // Image='isError' RealLoc='true'>
    //                <EmptyReferenceExpression BeginLine='3' DefiningType='' EndLine='3' Image=''
    // RealLoc='false' />
    //            </VariableExpression>
    //        </StandardCondition>
    //        <BlockStatement BeginLine='3' DefiningType='ComplexClass' EndLine='5' Image=''
    // RealLoc='true'>
    //            <ExpressionStatement BeginLine='4' DefiningType='ComplexClass' EndLine='4'
    // Image='' RealLoc='true'>
    //                <MethodCallExpression BeginLine='4' DefiningType='ComplexClass' EndLine='4'
    // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
    //                    <ReferenceExpression BeginLine='4' Context='' DefiningType='ComplexClass'
    // EndLine='4' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
    // SafeNav='false' />
    //                    <LiteralExpression BeginLine='4' DefiningType='ComplexClass' EndLine='4'
    // Image='an-error' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
    //                </MethodCallExpression>
    //            </ExpressionStatement>
    //        </BlockStatement>
    //    </IfBlockStatement>
    //    <BlockStatement BeginLine='5' DefiningType='ComplexClass' EndLine='7' Image=''
    // RealLoc='true'>
    //        <ExpressionStatement BeginLine='6' DefiningType='ComplexClass' EndLine='6' Image=''
    // RealLoc='true'>
    //            <MethodCallExpression BeginLine='6' DefiningType='ComplexClass' EndLine='6'
    // FullMethodName='System.debug' Image='' MethodName='debug' RealLoc='true'>
    //                <ReferenceExpression BeginLine='6' Context='' DefiningType='ComplexClass'
    // EndLine='6' Image='System' Names='[System]' RealLoc='true' ReferenceType='METHOD'
    // SafeNav='false' />
    //                <LiteralExpression BeginLine='6' DefiningType='ComplexClass' EndLine='6'
    // Image='not-an-error' LiteralType='STRING' Long='false' Name='' Null='false' RealLoc='true' />
    //            </MethodCallExpression>
    //        </ExpressionStatement>
    //    </BlockStatement>
    // </IfElseBlockStatement>

    @Test
    public void findingAPathThatBranches() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, MULTIPLE_PATHS, true);

        // Find the vertex that defines the method
        MethodVertex methodVertex =
                TestUtil.getMethodVertex(g, "ComplexClass", "methodWithControlFlow");

        // Figure out the paths through the method
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);

        // There are two paths since there is an if/else statement
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        // Look at the AST above, it contains an ASTStandardCondition vertex.
        // Loading the ASTStandardCondition from the graph without visiting the path results in an
        // UnknownStandardConditionVertex. This is because the StandardCondition could evaluate to
        // true or false. The
        // Path visitors will always operate on the PositiveStandardConditionVertex or
        // NegativeStandardConditionVertex
        BaseSFVertex standardCondition =
                SFVertexFactory.load(g, g.V().hasLabel(ASTConstants.NodeType.STANDARD_CONDITION));
        MatcherAssert.assertThat(
                standardCondition, instanceOf(StandardConditionVertex.Unknown.class));

        // We can look at the paths to find the difference

        // The positive path is the path where the "if" statement evaluated to true
        List<ApexPath> positivePaths =
                paths.stream()
                        .filter(
                                p ->
                                        p.verticesInCurrentMethod().stream()
                                                .anyMatch(
                                                        v ->
                                                                v
                                                                        instanceof
                                                                        StandardConditionVertex
                                                                                .Positive))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(positivePaths, hasSize(equalTo(1)));
        // Check that the sixth vertex in the positive path is line 4, this is the expression in the
        // "if" block
        MatcherAssert.assertThat(
                positivePaths.get(0).verticesInCurrentMethod().get(5).getBeginLine(), equalTo(4));

        // The negative path is the path where the "if" statement evaluated to false and the code
        // fell through to the else
        List<ApexPath> negativePaths =
                paths.stream()
                        .filter(
                                p ->
                                        p.verticesInCurrentMethod().stream()
                                                .anyMatch(
                                                        v ->
                                                                v
                                                                        instanceof
                                                                        StandardConditionVertex
                                                                                .Negative))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(negativePaths, hasSize(equalTo(1)));
        // Check that the sixth vertex in the negative path is line 6, this is the expression in the
        // "else" block
        MatcherAssert.assertThat(
                negativePaths.get(0).verticesInCurrentMethod().get(5).getBeginLine(), equalTo(6));
    }

    /**
     * {@link PathVertexVisitor} is the visitor that allows rules to traverse the path. This works
     * in tandem with a {@link SymbolProvider} that is passed to the visitor. The {@code
     * SymbolProvider} allows the visitor to determine the value of parameters at the current point
     * in the path. The {@code SymbolProvider} visits the node just before the visitor is called.
     */
    @Test
    public void traversingASimplePath() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, SINGLE_PATH, true);

        // Find the vertex that starts the method
        MethodVertex methodVertex =
                TestUtil.getMethodVertex(g, "SimpleClass", "methodWithNoControlFLow");

        // Figure out the path through the method
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        // Paths are navigated by creating a class that implements PathVertexVisitor and walking the
        // path with the
        // ApexPathWalker class. This is a very simple visitor that will record the order that the
        // vertices were visited
        // and record the values passed to System#debug whenever it is invoked.

        // List vertices visited
        List<BaseSFVertex> visitedVertices = new ArrayList<>();

        // Mapping of line number where System#debug was called to the value passed to System#debug
        Map<Integer, ChainedVertex> lineNumberToValuePassedToSystemDebug = new HashMap<>();

        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            ChainedVertex parameter = vertex.getParameters().get(0);
                            MatcherAssert.assertThat(
                                    parameter, instanceOf(VariableExpressionVertex.Single.class));
                            String symbolicName = parameter.getSymbolicName().orElse(null);
                            MatcherAssert.assertThat(symbolicName, equalTo("s"));

                            // System#debug is invoked with the "s" variable, we can ask the
                            // SymbolProvider to provide a more
                            // specific value for "s" if one exists. "s" should resolve to the last
                            // string literal that it was assigned.

                            // #getValue(ChainedVertex) returns an empty Optional if parameter can't
                            // be resolved any further
                            ChainedVertex resolvedUsingObject =
                                    symbols.getValue(parameter).orElse(parameter);
                            MatcherAssert.assertThat(
                                    resolvedUsingObject,
                                    instanceOf(LiteralExpressionVertex.SFString.class));

                            // #getValue(String) returns an empty Optional if the symbolic name
                            // can't be resolved
                            ChainedVertex resolvedUsingSymbolicName =
                                    symbols.getValue(symbolicName).orElse(null);
                            MatcherAssert.assertThat(
                                    resolvedUsingSymbolicName,
                                    instanceOf(LiteralExpressionVertex.SFString.class));

                            // In this case, both #getValue methods should return the same value
                            MatcherAssert.assertThat(
                                    resolvedUsingObject, equalTo(resolvedUsingSymbolicName));

                            // Store the line number and value for later analysis
                            ChainedVertex previous =
                                    lineNumberToValuePassedToSystemDebug.put(
                                            vertex.getBeginLine(), resolvedUsingObject);
                            if (previous != null) {
                                fail("Each line should only execute once");
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean visit(
                            VariableDeclarationStatementsVertex vertex, SymbolProvider symbols) {
                        visitedVertices.add(vertex);
                        return true;
                    }

                    @Override
                    public boolean visit(ExpressionStatementVertex vertex, SymbolProvider symbols) {
                        visitedVertices.add(vertex);
                        return true;
                    }
                };

        // Walk the path with our visitor
        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);

        // Verify that the vertices were visited in the order expected and are of the correct type
        MatcherAssert.assertThat(visitedVertices, hasSize(equalTo(4)));
        MatcherAssert.assertThat(visitedVertices.get(0).getBeginLine(), equalTo(3));
        MatcherAssert.assertThat(visitedVertices.get(1).getBeginLine(), equalTo(4));
        MatcherAssert.assertThat(visitedVertices.get(2).getBeginLine(), equalTo(5));
        MatcherAssert.assertThat(visitedVertices.get(3).getBeginLine(), equalTo(6));
        MatcherAssert.assertThat(
                visitedVertices.get(0), instanceOf(VariableDeclarationStatementsVertex.class));
        MatcherAssert.assertThat(
                visitedVertices.get(1), instanceOf(ExpressionStatementVertex.class));
        MatcherAssert.assertThat(
                visitedVertices.get(2), instanceOf(ExpressionStatementVertex.class));
        MatcherAssert.assertThat(
                visitedVertices.get(3), instanceOf(ExpressionStatementVertex.class));

        ChainedVertex systemDebugParameter;
        LiteralExpressionVertex.SFString parameterAsString;

        // Ensure that System#debug was called on two occasions
        MatcherAssert.assertThat(
                lineNumberToValuePassedToSystemDebug.keySet(), hasSize(equalTo(2)));

        // s should equal HelloFoo1 at line 4
        systemDebugParameter = lineNumberToValuePassedToSystemDebug.get(4);
        MatcherAssert.assertThat(
                systemDebugParameter, instanceOf(LiteralExpressionVertex.SFString.class));
        parameterAsString = (LiteralExpressionVertex.SFString) systemDebugParameter;
        MatcherAssert.assertThat(parameterAsString.getLiteral(), equalTo("HelloFoo1"));

        // s should equal HelloFoo2 at line 6, it was reassigned at line 5
        systemDebugParameter = lineNumberToValuePassedToSystemDebug.get(6);
        MatcherAssert.assertThat(
                systemDebugParameter, instanceOf(LiteralExpressionVertex.SFString.class));
        parameterAsString = (LiteralExpressionVertex.SFString) systemDebugParameter;
        MatcherAssert.assertThat(parameterAsString.getLiteral(), equalTo("HelloFoo2"));
    }

    private static final String FOR_EACH_PATH =
            "public class SimpleClass {\n"
                    + "    public void methodWithForEachLoop() {\n"
                    + "        String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                    + "        for (String fieldToCheck : fieldsToCheck) {\n"
                    + "           System.debug(fieldToCheck);\n"
                    + "        }\n"
                    + "    }\n"
                    + "}";

    /**
     * Variables that are declared in for/each context resolve to a {@link
     * VariableExpressionVertex.ForLoop}. The following example keeps track of all the values that
     * were passed to System#debug during the foreach execution
     */
    @Test
    public void resolvingForEachParameterValues() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, FOR_EACH_PATH, true);

        // Find the vertex that defines the method
        MethodVertex methodVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "methodWithForEachLoop"));

        // Figure out the path through the method
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);

        // Keep track of values when System#debug is called
        List<NewListLiteralExpressionVertex> visitedValues = new ArrayList<>();

        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {

                            // Obtain the parameter passed to System#debug, this is "fieldToCheck"
                            ChainedVertex parameter = vertex.getParameters().get(0);
                            String symbolicName = parameter.getSymbolicName().orElse(null);
                            MatcherAssert.assertThat(symbolicName, equalTo("fieldToCheck"));
                            MatcherAssert.assertThat(
                                    parameter, instanceOf(VariableExpressionVertex.Single.class));

                            // Ask the SymbolProvider what fieldToCheck resolves to
                            ChainedVertex resolved = symbols.getValue(parameter).orElse(parameter);
                            MatcherAssert.assertThat(
                                    resolved, instanceOf(VariableExpressionVertex.ForLoop.class));

                            // The ForLoopVariableExpressionVertex points to the original
                            // declaration
                            VariableExpressionVertex.ForLoop forLoopExpression =
                                    (VariableExpressionVertex.ForLoop) resolved;

                            // The values will point to the original declaration
                            MatcherAssert.assertThat(
                                    forLoopExpression.getForLoopValues(),
                                    instanceOf(NewListLiteralExpressionVertex.class));
                            visitedValues.add(
                                    (NewListLiteralExpressionVertex)
                                            forLoopExpression.getForLoopValues());
                        }
                        return true;
                    }
                };
        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);

        // Verify that fieldToCheck had the possible values of "Name" and "Phone"
        MatcherAssert.assertThat(visitedValues, hasSize(equalTo(1)));
        NewListLiteralExpressionVertex possibleValues = visitedValues.get(0);
        MatcherAssert.assertThat(possibleValues.getParameters(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                ((LiteralExpressionVertex.SFString) possibleValues.getParameters().get(0))
                        .getLiteral(),
                equalTo("Name"));
        MatcherAssert.assertThat(
                ((LiteralExpressionVertex.SFString) possibleValues.getParameters().get(1))
                        .getLiteral(),
                equalTo("Phone"));
    }

    private static final String OBJECT_FIELDS =
            "public class SimpleClass {\n"
                    + "    public void methodWithNoControlFLow() {\n"
                    + "        Account a = new Account(Phone = '415-555-1212');\n"
                    + "        a.Name = 'Acme Inc.';\n"
                    + "        a.put('PostalCode', '12345');\n"
                    + "        insert a;\n"
                    + "    }\n"
                    + "}";

    @Test
    @Disabled // TODO: Convert to ApexValues
    public void resolvingObjectsFields() {
        //        // Build the graph and run the graph builders
        //        TestUtil.buildGraph(g, OBJECT_FIELDS, true);
        //
        //        // Find the vertex that defines the method
        //        MethodVertex methodVertex = TestUtil.getMethodVertex(g, "SimpleClass",
        // "methodWithNoControlFLow");
        //
        //        // Figure out the path through the method
        //        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        //        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        //        ApexPath path = paths.get(0);
        //
        //        // Analyze the account object that is inserted
        //        List<ApexValue<?>> insertedApexValues = new ArrayList<>();
        //
        //        PathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor() {
        //            @Override
        //            public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols)
        // {
        //                if (!insertedApexValues.isEmpty()) {
        //                    throw new UnexpectedException("This should only execute once");
        //                }
        //                BaseSFVertex child = vertex.getOnlyChild();
        //                MatcherAssert.assertThat(child,
        // instanceOf(VariableExpressionVertex.class));
        //                ApexValue<?> apexValue =
        // symbols.getApexValue((VariableExpressionVertex)child).orElse(null);
        //                insertedApexValues.add(apexValue);
        //                return true;
        //            }
        //        };
        //
        //        // Walk the path with our visitor
        //        SymbolProviderVertexVisitor symbolProvider = new
        // DefaultSymbolProviderVertexVisitor(g);
        //        ApexPathWalker.walkPath(path, visitor, symbolProvider);
        //
        //        MatcherAssert.assertThat(insertedApexValues, hasSize(equalTo(1)));
        //        ApexValue<?> apexValue = insertedApexValues.get(0);
        //        MatcherAssert.assertThat(apexValue, not(nullValue()));
        //
        //        // Determine the concreteType. The concrete type differs based on how the object
        // is first created.
        //        // For instance the following all have different concrete types.
        //        // Account a = new Account();                                               //
        // NewObjectExpression
        //        // Account a = new Account(Name = 'Acme Inc.');                             //
        // NewKeyValueObjectExpressionVertex
        //        // SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();    //
        // LiteralExpressionVertex
        //        ChainedVertex concreteType = apexValue.getConcreteType().orElse(null);
        //        MatcherAssert.assertThat(concreteType, not(nullValue()));
        //        MatcherAssert.assertThat(concreteType,
        // instanceOf(NewKeyValueObjectExpressionVertex.class));
        //
        //        // Verify that the name, phone, and postal code properties are present
        //        MatcherAssert.assertThat(apexValue, instanceOf(ObjectProperties.class));
        //
        // MatcherAssert.assertThat(((ObjectProperties)apexValue).getApexValueProperties().keySet(),
        // hasSize(equalTo(3)));
        //
        //        // Key/Value pairs passed into an object during object instantiation result in a
        // single AST node that represents
        //        // the key and value at once. As you can see below the key and value are captures
        // in a single LiteralExpression
        //        // node. The key is represented by the 'Name' attribute that has a value of
        // 'Phone' and the value is represented
        //        // by the 'Image' attribute which has a value of '415-555-1212'.
        //        //
        //        // <NewKeyValueObjectExpression BeginLine='3' Image='' Type='Account'>
        //        //    <LiteralExpression BeginColumn='41' BeginLine='3' Image='415-555-1212'
        // Name='Phone' />
        //        // </NewKeyValueObjectExpression>
        //        //
        //        // At the current time, the caller of the ObjectProperties class needs to handle
        // these differences. At first I
        //        // thought we could always represent the key as a string, but some code is so
        // dynamic that this is not always
        //        // possible to know the string values.
        //
        //        // Navigate from the NewKeyValueObjectExpression to find the phone key
        //        LiteralExpressionVertex.SFString phoneVertexKey = concreteType.getOnlyChild();
        //        MatcherAssert.assertThat(phoneVertexKey.getName(), equalTo("Phone"));
        //        MatcherAssert.assertThat(phoneVertexKey.getImage(), equalTo("415-555-1212"));
        //
        //        // We can also retrieve the value that corresponds to the key
        //        // TODO: Clean up casts
        //        MatcherAssert.assertThat(apexValue, instanceOf(ObjectProperties.class));
        //        ChainedVertex phoneVertexValue =
        // ((ObjectProperties)apexValue).getProperties().get(phoneVertexKey);
        //        // The key and value in this context are the same vertex
        //        MatcherAssert.assertThat(phoneVertexKey, equalTo(phoneVertexValue));
        //
        //        // ObjectProperties implements a helper method #getEntryByKeyName. This is useful
        // for testing, but in practice
        //        // it might not be general enough to handle all scenarios when writing a rule.
        //        Map.Entry<ChainedVertex, ChainedVertex> entry =
        // ((ObjectProperties)apexValue).getEntryByKeyName("phone").orElse(null);
        //        MatcherAssert.assertThat(entry, not(nullValue()));
        //        LiteralExpressionVertex<?> literalExpression =
        // (LiteralExpressionVertex<?>)entry.getKey();
        //        MatcherAssert.assertThat(literalExpression.getKeyName(), equalTo("Phone"));
        //        MatcherAssert.assertThat(entry.getValue().getImage(), equalTo("415-555-1212"));
        //
        //        // We could also filter the entries to find the Phone properties. The phone
        // property is the one with a
        //        // LiteralExpressionVertex.SFString key that has a "Name" of "Phone
        //        List<Map.Entry<ChainedVertex, ChainedVertex>> phoneValues =
        // ((ObjectProperties)apexValue)
        //                .getProperties()
        //                .entrySet()
        //                .stream().filter(e -> (e.getKey() instanceof
        // LiteralExpressionVertex.SFString &&
        //
        // ((LiteralExpressionVertex.SFString)e.getKey()).getExpressionType().equals(ExpressionType.KEY_VALUE) &&
        //
        // ((LiteralExpressionVertex.SFString)e.getKey()).getKeyName().equals("Phone")))
        //                .collect(Collectors.toList());
        //        MatcherAssert.assertThat(phoneValues, hasSize(equalTo(1)));
        //        // Verify the key and value are the same vertex
        //        entry = phoneValues.get(0);
        //        MatcherAssert.assertThat(entry.getKey(), equalTo(phoneValues.get(0).getValue()));
        //        MatcherAssert.assertThat(entry.getKey(),
        // instanceOf(LiteralExpressionVertex.SFString.class));
        //        MatcherAssert.assertThat(entry.getValue(),
        // instanceOf(LiteralExpressionVertex.SFString.class));
        //        // See that it equals the value above
        //        LiteralExpressionVertex.SFString stringVertex;
        //        stringVertex = (LiteralExpressionVertex.SFString)entry.getKey();
        //        MatcherAssert.assertThat(stringVertex.getKeyName(), equalTo("Phone"));
        //        stringVertex = (LiteralExpressionVertex.SFString)entry.getValue();
        //        MatcherAssert.assertThat(stringVertex.getLiteral(), equalTo("415-555-1212"));
        //
        //        // We can find the Name property by looking for the key which is a
        // VariableExpressionVertex
        //        MatcherAssert.assertThat(apexValue, instanceOf(ObjectProperties.class));
        //        List<Map.Entry<ChainedVertex, ChainedVertex>> nameValues =
        // ((ObjectProperties)apexValue)
        //                .getProperties()
        //                .entrySet()
        //                .stream().filter(e -> e.getKey() instanceof VariableExpressionVertex)
        //                .collect(Collectors.toList());
        //        MatcherAssert.assertThat(nameValues, hasSize(equalTo(1)));
        //        // Verify the key and value are different vertices of the correct type
        //        entry = nameValues.get(0);
        //        MatcherAssert.assertThat(entry.getKey(),
        // not(equalTo(nameValues.get(0).getValue())));
        //        MatcherAssert.assertThat(entry.getKey(),
        // instanceOf(VariableExpressionVertex.class));
        //        MatcherAssert.assertThat(entry.getValue(),
        // instanceOf(LiteralExpressionVertex.SFString.class));
        //        // Verify the values
        //        VariableExpressionVertex variableExpression =
        // (VariableExpressionVertex)entry.getKey();
        //        MatcherAssert.assertThat(variableExpression.getImage(), equalTo("Name"));
        //        stringVertex = (LiteralExpressionVertex.SFString)entry.getValue();
        //        MatcherAssert.assertThat(stringVertex.getLiteral(), equalTo("Acme Inc."));
        //
        //        // The postal code information is stored on two different LiteralExpression values
        //        List<Map.Entry<ChainedVertex, ChainedVertex>> postalCodeValues =
        // ((ObjectProperties)apexValue)
        //                .getProperties()
        //                .entrySet()
        //                .stream().filter(e -> (e.getKey().getImage().equals("PostalCode")))
        //                .collect(Collectors.toList());
        //        MatcherAssert.assertThat(postalCodeValues, hasSize(equalTo(1)));
        //        // Verify the key and value are the different vertices
        //        entry = postalCodeValues.get(0);
        //        MatcherAssert.assertThat(entry.getKey(),
        // not(equalTo(phoneValues.get(0).getValue())));
        //        MatcherAssert.assertThat(entry.getKey(),
        // instanceOf(LiteralExpressionVertex.SFString.class));
        //        MatcherAssert.assertThat(entry.getValue(),
        // instanceOf(LiteralExpressionVertex.SFString.class));
        //        // Verify the values
        //        stringVertex = (LiteralExpressionVertex.SFString)entry.getKey();
        //        MatcherAssert.assertThat(stringVertex.getLiteral(), equalTo("PostalCode"));
        //        stringVertex = (LiteralExpressionVertex.SFString)entry.getValue();
        //        MatcherAssert.assertThat(stringVertex.getLiteral(), equalTo("12345"));
    }

    private static final String PATH_WITH_METHOD_CALL =
            "public class PathWithMethodCall {\n"
                    + "    public void doSomething() {\n"
                    + "        logSomething('Hello');\n"
                    + "    }\n"
                    + "    public void logSomething(String s) {\n"
                    + "        System.debug(s);\n"
                    + "    }\n"
                    + "}";

    /**
     * You must use the {@link ApexPathWalker} in order to evaluate all vertices in a path with
     * method calls. This is because the method calls can't be resolved unless the path is
     * iteratively traveled to resolve variables. This becomes more important when instance methods
     * are involved.
     */
    @Test
    public void pathsWithMethodCalls() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, PATH_WITH_METHOD_CALL, true);

        // Find the vertex that defines the method
        MethodVertex methodVertex =
                TestUtil.getMethodVertex(g, "PathWithMethodCall", "doSomething");

        // Sanity check that we have the correct vertex
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(2));

        // Verify there is a single path
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        // The path only contains a single vertex
        MatcherAssert.assertThat(path.verticesInCurrentMethod(), hasSize(equalTo(2)));

        // But it contains a method call, the method's vertices are visited when the path is walked
        MatcherAssert.assertThat(path.resolvedInvocableCallCountInCurrentMethod(), equalTo(1));

        // The following visitor will let us see the value at the time System#debug is called
        List<LiteralExpressionVertex.SFString> visitedValues = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            // Obtain the parameter passed to System#debug, this is "s", which was
                            // passed as a method param
                            ChainedVertex parameter = vertex.getParameters().get(0);
                            String symbolicName = parameter.getSymbolicName().orElse(null);
                            MatcherAssert.assertThat(symbolicName, equalTo("s"));
                            MatcherAssert.assertThat(
                                    parameter, instanceOf(VariableExpressionVertex.Single.class));

                            // Ask the SymbolProvider what "s" resolves to
                            ChainedVertex resolved = symbols.getValue(parameter).orElse(parameter);
                            MatcherAssert.assertThat(
                                    resolved, instanceOf(LiteralExpressionVertex.SFString.class));
                            visitedValues.add((LiteralExpressionVertex.SFString) resolved);
                        }
                        return true;
                    }
                };
        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);

        // Verify that System#debug was invoked with the original string literal passed to the
        // method
        MatcherAssert.assertThat(visitedValues, hasSize(equalTo(1)));
        MatcherAssert.assertThat(visitedValues.get(0).getLiteral(), equalTo("Hello"));
    }

    private static final String PATH_WITH_EXCEPTION =
            "public class PathWithException {\n"
                    + "    public void doInsert() {\n"
                    + "        checkPermissions();\n"
                    + "        System.debug('it is safe to inserts');\n"
                    + "    }\n"
                    + "    public void checkPermissions() {\n"
                    + "        if (!hasCorrectExceptions()) {\n"
                    + "           throw new Exception();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}";

    /**
     * The following example demonstrates what happens if we traverse paths that contain exceptions.
     * There are two paths if we ask for the forward paths starting with the first vertex in the
     * #doInsert method, the path that ends in an exception is filtered out. There is only a single
     * path if we ask for the reverse path starting with the System#debug statement in the #doInsert
     * method, we will see the reverse path example in the {@link #reversePathsWithExceptions} test.
     */
    @Test
    public void forwardPathsWithExceptions() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, PATH_WITH_EXCEPTION, true);

        // Find the vertex that defines the method
        MethodVertex methodVertex = TestUtil.getMethodVertex(g, "PathWithException", "doInsert");

        // Sanity check that we have the correct vertex
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(2));

        List<ApexPath> forwardPaths = ApexPathUtil.getForwardPaths(g, methodVertex);
        MatcherAssert.assertThat(forwardPaths, hasSize(equalTo(1)));

        ApexPath pathThatEndsInSystemDebug = forwardPaths.get(0);

        // The following visitor will traverse the path without an exception and ensure
        // 1. System#Debug is called
        // 2. The NegativeStandardConditionVertex is traversed, negative in this context indicates
        // that the "if" statement evaluated to false
        // 3. The throw Exception vertex is never visited
        List<StandardConditionVertex.Negative> negativeConditions = new ArrayList<>();
        List<MethodCallExpressionVertex> methodCalls = new ArrayList<>();
        PathVertexVisitor positivePathVisitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(ThrowStatementVertex vertex, SymbolProvider symbols) {
                        throw new UnexpectedException(
                                "The exceptional case only occurs when the if statement evaluates to true");
                    }

                    @Override
                    public boolean visit(
                            StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
                        throw new UnexpectedException(
                                "The exceptional case only occurs when the if statement evaluates to true");
                    }

                    @Override
                    public boolean visit(
                            StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
                        negativeConditions.add(vertex);
                        return true;
                    }

                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            methodCalls.add(vertex);
                        }
                        return true;
                    }
                };

        // Walk the path and ensure that the path contains the negative condition that led to the
        // exception being
        // skipped and also the call to System#debug
        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, pathThatEndsInSystemDebug, positivePathVisitor, symbolProvider);
        MatcherAssert.assertThat(negativeConditions, hasSize(equalTo(1)));
        MatcherAssert.assertThat(methodCalls, hasSize(equalTo(1)));
    }

    /**
     * Traversing a path in reverse will filter out all paths that end in an exception before the
     * node of interest. In this case, we ask for reverse paths that can lead to the System#debug
     * method. The result is a single path that exactly matches the pathThatEndsInSystemDebug from
     * {@link #forwardPathsWithExceptions}
     */
    @Test
    public void reversePathsWithExceptions() {
        // Build the graph and run the graph builders
        TestUtil.buildGraph(g, PATH_WITH_EXCEPTION, true);

        // Find the vertex that starts the method
        ExpressionStatementVertex firstVertex =
                TestUtil.getVertexOnLine(g, ExpressionStatementVertex.class, 3);

        // Sanity check that we have the correct vertices
        MatcherAssert.assertThat(firstVertex.getBeginLine(), equalTo(3));

        // Traverse to the System#debug method using #getNextSibling
        ExpressionStatementVertex lastVertex = firstVertex.getNextSibling();
        MatcherAssert.assertThat(lastVertex.getBeginLine(), equalTo(4));

        // Asking for all paths that can reach System#debug results in a single path. The path that
        // results in the exception
        // has been filtered out since it doesn't contain the vertex where the reverse path starts.
        // Walking the path
        // will contain the NegativeStandardConditionVertex which gives the visitor enough
        // information to declare if the
        // code is executing properly
        List<ApexPath> reversePaths = ApexPathUtil.getReversePaths(g, lastVertex);
        MatcherAssert.assertThat(reversePaths, hasSize(equalTo(1)));
        ApexPath reversePath = reversePaths.get(0);

        // This is the same visitor as the positive path above. It's inlined for clarity of the
        // example
        // The following visitor will traverse the path without an exception and ensure
        // 1. System#Debug is called
        // 2. The NegativeStandardConditionVertex is traversed, negative in this context indicates
        // that the "if" statement evaluated to false
        // 3. The throw Exception vertex is never visited
        List<StandardConditionVertex.Negative> negativeConditions = new ArrayList<>();
        List<MethodCallExpressionVertex> methodCalls = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(ThrowStatementVertex vertex, SymbolProvider symbols) {
                        throw new UnexpectedException(
                                "The exceptional case only occurs when the if statement evaluates to true");
                    }

                    @Override
                    public boolean visit(
                            StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
                        throw new UnexpectedException(
                                "The exceptional case only occurs when the if statement evaluates to true");
                    }

                    @Override
                    public boolean visit(
                            StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
                        negativeConditions.add(vertex);
                        return true;
                    }

                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            methodCalls.add(vertex);
                        }
                        return true;
                    }
                };

        // Walk the path and ensure that the path contains the negative condition that led to the
        // exception being
        // skipped and also the call to System#debug
        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, reversePath, visitor, symbolProvider);
        MatcherAssert.assertThat(negativeConditions, hasSize(equalTo(1)));
        MatcherAssert.assertThat(methodCalls, hasSize(equalTo(1)));
    }

    // This is an array that would normally be two separate files (MyClass.cls and MyOtherClass.cls)
    String[] MULTIPLE_CLASSES_MULTIPLE_INSTANCES = {
        "public class MyClass {\n"
                + "    private final String a;\n"
                + "    private final String b;\n"
                + "    private String c;\n"
                + "    public MyClass(String a1, String b1) {\n"
                + "       a = a1;\n"
                + "       b = b1;\n"
                + "       c = 'Congratulations';\n"
                + "    }\n"
                + "    public void logSomething() {\n"
                + "       System.debug(a);\n"
                + "       System.debug(b);\n"
                + "       System.debug(c);\n"
                + "    }\n"
                + "}",
        "public class MyOtherClass {\n"
                + "    public void doSomething() {\n"
                + "       MyClass m1 = new MyClass('Hello1', 'Goodbye1');\n"
                + "       m1.logSomething();\n"
                + "       MyClass m2 = new MyClass('Hello2', 'Goodbye2');\n"
                + "       m2.logSomething();\n"
                + "    }\n"
                + "}",
    };

    /**
     * The following example demonstrates how the Path traversal works across classes and within
     * class instances. The values passed to System#debug are based on the values passed to the
     * constructor of MyClass. Variable c is not a final variable, at this point we don't support
     * resolving non-final variables because guaranteeing their value might not be possible, this
     * may change in the future.
     */
    @Test
    public void multipleClassesWithInstanceVariables() {
        // Build the graph and run the graph builders, run it over each class from the
        TestUtil.buildGraph(g, MULTIPLE_CLASSES_MULTIPLE_INSTANCES, true);

        // Find the vertex that defines the method
        MethodVertex methodVertex = TestUtil.getMethodVertex(g, "MyOtherClass", "doSomething");

        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);
        // There is only a single path through the method since there is no branching logic
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        // A mapping of variable name, to its value on a given line. Our example executes the
        // MyClass#logSomething
        // method multiple times using different instances. We we need to keep a list of the values
        // at each line number.
        TreeMap<String, Map<Integer, List<LiteralExpressionVertex.SFString>>> results =
                new TreeMap<>();

        // This visitor duplicates the helper class MethodParameterAccumulator. You should use
        // MethodParameterAccumulator for normal tests
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            ChainedVertex parameter = vertex.getParameters().get(0);
                            MatcherAssert.assertThat(
                                    parameter, instanceOf(VariableExpressionVertex.Single.class));

                            // System#debug should be called with either a, b, or c
                            String symbolicName = parameter.getSymbolicName().orElse(null);
                            MatcherAssert.assertThat(symbolicName, oneOf("a", "b", "c"));

                            Integer line = vertex.getBeginLine();
                            ChainedVertex value = symbols.getValue(symbolicName).orElse(null);
                            // Store the value in the list that corresponds to the current line
                            // number
                            Map<Integer, List<LiteralExpressionVertex.SFString>> variableResults =
                                    results.computeIfAbsent(symbolicName, k -> new HashMap<>());
                            List<LiteralExpressionVertex.SFString> results =
                                    variableResults.computeIfAbsent(line, k -> new ArrayList<>());
                            results.add((LiteralExpressionVertex.SFString) value);
                        }
                        return true;
                    }
                };

        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);

        // We should have collected values for a, b, and c
        MatcherAssert.assertThat(results.keySet(), containsInAnyOrder("a", "b", "c"));

        Map<Integer, List<LiteralExpressionVertex.SFString>> variableValues;
        List<String> valuesPassedToSystemDebug;

        // System#debug with variable "a" was invoked on line 11 of MyClass
        variableValues = results.get("a");
        MatcherAssert.assertThat(variableValues.keySet(), hasSize(1));
        // Map the values to their string equivalents
        valuesPassedToSystemDebug =
                variableValues.get(11).stream()
                        .map(o -> o.getLiteral())
                        .collect(Collectors.toList());
        // Values are lower cased for consistency, Apex is case insensitive
        MatcherAssert.assertThat(valuesPassedToSystemDebug, contains("Hello1", "Hello2"));

        // System#debug with variable "b" was invoked on line 12 of MyClass
        variableValues = results.get("b");
        MatcherAssert.assertThat(variableValues.keySet(), hasSize(1));
        // Map the values to their string equivalents
        valuesPassedToSystemDebug =
                variableValues.get(12).stream()
                        .map(o -> o.getLiteral())
                        .collect(Collectors.toList());
        // Values are lower cased for consistency, Apex is case insensitive
        MatcherAssert.assertThat(valuesPassedToSystemDebug, contains("Goodbye1", "Goodbye2"));

        // System#debug with variable "c" was invoked on line 13 of MyClass
        variableValues = results.get("c");
        MatcherAssert.assertThat(variableValues.keySet(), hasSize(1));
        // There should be two results, both with the same value
        MatcherAssert.assertThat(variableValues.get(13), hasSize(equalTo(2)));
        for (LiteralExpressionVertex.SFString value : variableValues.get(13)) {
            MatcherAssert.assertThat(value.getLiteral(), equalTo("Congratulations"));
        }
    }

    // *********************************************************************************************************
    // END PATH INFO
    // *********************************************************************************************************
}

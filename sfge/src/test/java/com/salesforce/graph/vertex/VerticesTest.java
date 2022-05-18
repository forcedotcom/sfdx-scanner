package com.salesforce.graph.vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for general case vertex tests */
public class VerticesTest {
    private static final Logger LOGGER = LogManager.getLogger(VerticesTest.class);

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimple() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    @AuraEnabled\n"
                        + "    public static void doSomething() {\n"
                        + "       Integer x = 10;\n"
                        + "       x = 20;\n"
                        + "       if (x == 10 || x == 11) {\n"
                        + "           System.debug(x);\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);

        // The column information for these items is not as accurate as one might expect
        UserClassVertex userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyClass"));
        MatcherAssert.assertThat(userClass.getBeginLine(), equalTo(1));
        MatcherAssert.assertThat(userClass.getEndLine(), equalTo(1));
        MatcherAssert.assertThat(userClass.getBeginColumn(), equalTo(14));

        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "doSomething"));
        MatcherAssert.assertThat(method.getBeginLine(), equalTo(3));
        MatcherAssert.assertThat(method.getEndLine(), equalTo(3));

        BlockStatementVertex blockStatement = method.getChild(1);
        MatcherAssert.assertThat(blockStatement.getBeginLine(), equalTo(3));
        MatcherAssert.assertThat(blockStatement.getEndLine(), equalTo(9));

        VariableDeclarationStatementsVertex variableDeclarationStatements =
                blockStatement.getChild(0);
        MatcherAssert.assertThat(variableDeclarationStatements.getBeginLine(), equalTo(4));
        MatcherAssert.assertThat(variableDeclarationStatements.getEndLine(), equalTo(4));
        MatcherAssert.assertThat(variableDeclarationStatements.getBeginColumn(), equalTo(8));

        AssignmentExpressionVertex assignmentExpression = blockStatement.getChild(1).getChild(0);
        MatcherAssert.assertThat(assignmentExpression.getBeginLine(), equalTo(5));
        MatcherAssert.assertThat(assignmentExpression.getEndLine(), equalTo(5));
        MatcherAssert.assertThat(assignmentExpression.getBeginColumn(), equalTo(8));

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "debug"));
        MatcherAssert.assertThat(methodCallExpression.getBeginLine(), equalTo(7));
        MatcherAssert.assertThat(methodCallExpression.getEndLine(), equalTo(7));
        // This is the start of the "debug"
        MatcherAssert.assertThat(methodCallExpression.getBeginColumn(), equalTo(19));
    }

    @Test
    public void testSimpleNoLeadingSpaces() {
        String sourceCode =
                "public class MyClass {\n"
                        + "@AuraEnabled\n"
                        + "public static void doSomething() {\n"
                        + "Integer x = 10;\n"
                        + "x = 20;\n"
                        + "if (x == 10 || x == 11) {\n"
                        + "System.debug(x);\n"
                        + "}\n"
                        + "}\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);

        // The column information for these items is not as accurate as one might expect
        UserClassVertex userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyClass"));
        MatcherAssert.assertThat(userClass.getBeginLine(), equalTo(1));
        MatcherAssert.assertThat(userClass.getEndLine(), equalTo(1));
        MatcherAssert.assertThat(userClass.getBeginColumn(), equalTo(14));

        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, "doSomething"));
        MatcherAssert.assertThat(method.getBeginLine(), equalTo(3));
        MatcherAssert.assertThat(method.getEndLine(), equalTo(3));

        BlockStatementVertex blockStatement = method.getChild(1);
        MatcherAssert.assertThat(blockStatement.getBeginLine(), equalTo(3));
        MatcherAssert.assertThat(blockStatement.getEndLine(), equalTo(9));

        VariableDeclarationStatementsVertex variableDeclarationStatements =
                blockStatement.getChild(0);
        MatcherAssert.assertThat(variableDeclarationStatements.getBeginLine(), equalTo(4));
        MatcherAssert.assertThat(variableDeclarationStatements.getEndLine(), equalTo(4));
        MatcherAssert.assertThat(variableDeclarationStatements.getBeginColumn(), equalTo(1));

        AssignmentExpressionVertex assignmentExpression = blockStatement.getChild(1).getChild(0);
        MatcherAssert.assertThat(assignmentExpression.getBeginLine(), equalTo(5));
        MatcherAssert.assertThat(assignmentExpression.getEndLine(), equalTo(5));
        MatcherAssert.assertThat(assignmentExpression.getBeginColumn(), equalTo(1));

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "debug"));
        MatcherAssert.assertThat(methodCallExpression.getBeginLine(), equalTo(7));
        MatcherAssert.assertThat(methodCallExpression.getEndLine(), equalTo(7));
        // This is the start of the "debug"
        MatcherAssert.assertThat(methodCallExpression.getBeginColumn(), equalTo(8));
    }

    @Test
    public void testMethodAuraAnnotations() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    @AuraEnabled\n"
                        + "    public static void foo() {\n"
                        + "       bar();\n"
                        + "    }\n"
                        + "    public static void bar() {\n"
                        + "    }\n"
                        + "}\n";

        // <Method Arity='0' BeginLine='3' CanonicalName='foo' Constructor='false'
        // DefiningType='MyClass' EndLine='4' Image='foo' RealLoc='true' ReturnType='void'>
        //    <ModifierNode Abstract='false' BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Final='false' Global='false' Image='' InheritedSharing='false' Modifiers='9'
        // Override='false' Private='false' Protected='false' Public='true' RealLoc='true'
        // Static='true' Test='false' TestOrTestSetup='false' Transient='false' WebService='false'
        // WithSharing='false' WithoutSharing='false'>
        //        <Annotation BeginLine='2' DefiningType='MyClass' EndLine='2' Image='AuraEnabled'
        // RealLoc='true' Resolved='true'>
        //            <AnnotationParameter BeginLine='2' DefiningType='' EndLine='2' Image='false'
        // Name='cacheable' RealLoc='false' Value='false' />
        //            <AnnotationParameter BeginLine='2' DefiningType='' EndLine='2' Image='false'
        // Name='continuation' RealLoc='false' Value='false' />
        //        </Annotation>
        //    </ModifierNode>
        //    <BlockStatement BeginLine='3' DefiningType='MyClass' EndLine='4' Image=''
        // RealLoc='true' />
        // </Method>

        TestUtil.buildGraph(g, sourceCode);

        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 3);
        ModifierNodeVertex modifierNode = method.getModifierNode();
        MatcherAssert.assertThat(modifierNode.isFinal(), equalTo(false));
        MatcherAssert.assertThat(modifierNode.isAbstract(), equalTo(false));
        MatcherAssert.assertThat(modifierNode.isStatic(), equalTo(true));
        List<AnnotationVertex> annotations = method.getModifierNode().getAnnotations();
        MatcherAssert.assertThat(annotations, hasSize(equalTo(1)));
        AnnotationVertex annotation = annotations.get(0);
        MatcherAssert.assertThat(annotation.getName(), equalTo(Schema.AURA_ENABLED));
        List<AnnotationParameterVertex> annotationParameters = annotation.getParameters();
        MatcherAssert.assertThat(annotationParameters, hasSize(equalTo(2)));
    }

    @Test
    public void testChainedMethodGetNext() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       String s = MySingleton.getInstance().getName();\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex getNameVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getName"));

        MethodCallExpressionVertex getInstanceVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getInstance"));

        MatcherAssert.assertThat(getInstanceVertex.getNext().isPresent(), equalTo(true));
        MatcherAssert.assertThat(getInstanceVertex.getNext().get(), equalTo(getNameVertex));
        MatcherAssert.assertThat(getInstanceVertex.getFirst(), equalTo(getInstanceVertex));

        MatcherAssert.assertThat(getNameVertex.getNext().isPresent(), equalTo(false));
        MatcherAssert.assertThat(getNameVertex.getFirst(), equalTo(getInstanceVertex));
    }

    // Test MyClass.class.getName()

    @Test
    public void testChainedMethodAsParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       Schema.getGlobalDescribe().get(o.getDescribe().getName()).newSObject();\n"
                        + "    }\n"
                        + "}\n";

        // <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='newSObject' Image='' MethodName='newSObject' RealLoc='true'>
        //    <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass' EndLine='3'
        // Image='' Names='[]' RealLoc='false' ReferenceType='METHOD' SafeNav='false'>
        //        <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='get' Image='' MethodName='get' RealLoc='true'>
        //            <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass'
        // EndLine='3' Image='' Names='[]' RealLoc='false' ReferenceType='METHOD' SafeNav='false'>
        //                <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='Schema.getGlobalDescribe' Image='' MethodName='getGlobalDescribe'
        // RealLoc='true'>
        //                    <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass'
        // EndLine='3' Image='Schema' Names='[Schema]' RealLoc='true' ReferenceType='METHOD'
        // SafeNav='false' />
        //                </MethodCallExpression>
        //            </ReferenceExpression>
        //            <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='getName' Image='' MethodName='getName' RealLoc='true'>
        //                <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass'
        // EndLine='3' Image='' Names='[]' RealLoc='false' ReferenceType='METHOD' SafeNav='false'>
        //                    <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='o.getDescribe' Image='' MethodName='getDescribe' RealLoc='true'>
        //                        <ReferenceExpression BeginLine='3' Context=''
        // DefiningType='MyClass' EndLine='3' Image='o' Names='[o]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        //                    </MethodCallExpression>
        //                </ReferenceExpression>
        //            </MethodCallExpression>
        //        </MethodCallExpression>
        //    </ReferenceExpression>
        // </MethodCallExpression>

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex getGlobalDescribeVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getGlobalDescribe"));
        MatcherAssert.assertThat(
                getGlobalDescribeVertex.getFirst(), equalTo(getGlobalDescribeVertex));

        MethodCallExpressionVertex getVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "get"));
        MatcherAssert.assertThat(getVertex.getFirst(), equalTo(getGlobalDescribeVertex));

        MethodCallExpressionVertex newSObjectVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "newSObject"));
        MatcherAssert.assertThat(newSObjectVertex.getFirst(), equalTo(getGlobalDescribeVertex));
        MatcherAssert.assertThat(getGlobalDescribeVertex.getLast(), equalTo(newSObjectVertex));
        MatcherAssert.assertThat(getVertex.getLast(), equalTo(newSObjectVertex));
        MatcherAssert.assertThat(newSObjectVertex.getLast(), equalTo(newSObjectVertex));

        MethodCallExpressionVertex getDescribeVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getDescribe"));
        MatcherAssert.assertThat(getDescribeVertex.getFirst(), equalTo(getDescribeVertex));

        MethodCallExpressionVertex getNameVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getName"));
        MatcherAssert.assertThat(getNameVertex.getFirst(), equalTo(getDescribeVertex));
        MatcherAssert.assertThat(getDescribeVertex.getLast(), equalTo(getNameVertex));
        MatcherAssert.assertThat(getNameVertex.getLast(), equalTo(getNameVertex));
    }

    @Test
    public void testClassGetName() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       String name = MyClass.class.getName().toLowerCase();\n"
                        + "    }\n"
                        + "}\n";

        // <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='toLowerCase' Image='' MethodName='toLowerCase' RealLoc='true'>
        //    <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass' EndLine='3'
        // Image='' Names='[]' RealLoc='false' ReferenceType='METHOD' SafeNav='false'>
        //        <MethodCallExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // FullMethodName='getName' Image='' MethodName='getName' RealLoc='true'>
        //            <ReferenceExpression BeginLine='3' Context='' DefiningType='MyClass'
        // EndLine='3' Image='' Names='[]' RealLoc='false' ReferenceType='METHOD' SafeNav='false'>
        //                <ClassRefExpression BeginLine='3' DefiningType='MyClass' EndLine='3'
        // Image='' RealLoc='true' />
        //            </ReferenceExpression>
        //        </MethodCallExpression>
        //    </ReferenceExpression>
        // </MethodCallExpression>

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex getNameVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getName"));
        MatcherAssert.assertThat(getNameVertex.getFirst(), equalTo(getNameVertex));

        MethodCallExpressionVertex toLowerCaseVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "toLowerCase"));
        MatcherAssert.assertThat(toLowerCaseVertex.getFirst(), equalTo(getNameVertex));
        MatcherAssert.assertThat(getNameVertex.getLast(), equalTo(toLowerCaseVertex));
        MatcherAssert.assertThat(toLowerCaseVertex.getLast(), equalTo(toLowerCaseVertex));
    }

    @Test
    public void testStandardLibrary() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       String name = MyClass.class.getName().toLowerCase();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);

        UserClassVertex userClass;

        userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyClass"));
        MatcherAssert.assertThat(userClass.isStandardType(), equalTo(false));

        // Load the Schema.SObjectType type
        userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(
                                        Schema.NAME,
                                        ApexStandardLibraryUtil.getCanonicalName(
                                                ApexStandardLibraryUtil.Type
                                                        .SCHEMA_S_OBJECT_TYPE)));
        MatcherAssert.assertThat(userClass.isStandardType(), equalTo(true));

        // Load the canonical SObjectType type
        userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(
                                        Schema.NAME,
                                        ApexStandardLibraryUtil.getCanonicalName(
                                                ApexStandardLibraryUtil.VariableNames
                                                        .S_OBJECT_TYPE)));
        MatcherAssert.assertThat(userClass.isStandardType(), equalTo(true));

        // Load the System.Schema type
        userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(
                                        Schema.NAME,
                                        ApexStandardLibraryUtil.getCanonicalName(
                                                ApexStandardLibraryUtil.Type.SYSTEM_SCHEMA)));
        MatcherAssert.assertThat(userClass.isStandardType(), equalTo(true));

        // Load the canonical Schema type
        userClass =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(
                                        Schema.NAME,
                                        ApexStandardLibraryUtil.getCanonicalName("Schema")));
        MatcherAssert.assertThat(userClass.isStandardType(), equalTo(true));
    }

    /**
     * The default AST doesn't contain information about MyOtherClass. This tests that the graph is
     * enhanced with this information.
     */
    @Test
    public void testClassRefExpressionTypeRef() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       System.debug(MyOtherClass.class.getName());\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);

        ClassRefExpressionVertex classRefExpression =
                SFVertexFactory.load(g, g.V().hasLabel(ASTConstants.NodeType.CLASS_REF_EXPRESSION));
        MatcherAssert.assertThat(classRefExpression.getCanonicalType(), equalTo("MyOtherClass"));
    }

    /** Ensure that the KeyVertex parameters are correctly added to the VariableExpressionVertex */
    @Test
    public void testNewKeyValueObjectExpressionWithVariables() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private static void foo() {\n"
                        + "       String itemId = 'foo';"
                        + "       MyObject__c obj = new MyObject__c(Item_Id__c = itemId);\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);

        NewKeyValueObjectExpressionVertex vertex =
                SFVertexFactory.load(
                        g, g.V().hasLabel(ASTConstants.NodeType.NEW_KEY_VALUE_OBJECT_EXPRESSION));

        List<ChainedVertex> items = vertex.getItems();
        MatcherAssert.assertThat(items, hasSize(equalTo(1)));

        VariableExpressionVertex variableExpression = (VariableExpressionVertex) items.get(0);
        MatcherAssert.assertThat(
                variableExpression.getExpressionType(), equalTo(ExpressionType.KEY_VALUE));
        MatcherAssert.assertThat(variableExpression.getKeyName().get(), equalTo("Item_Id__c"));
        MatcherAssert.assertThat(variableExpression.getName(), equalTo("itemId"));
    }

    @Test
    public void testFieldType() {
        String[] sourceCode = {"public class MyClass {\n" + "   public String myString;\n" + "}"};

        TestUtil.buildGraph(g, sourceCode);

        FieldVertex vertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .has(Schema.NAME, "myString")
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(vertex.getCanonicalType(), equalTo("String"));
    }

    @Test
    public void testFieldDeclarationType() {
        String[] sourceCode = {"public class MyClass {\n" + "   public String myString;\n" + "}"};

        TestUtil.buildGraph(g, sourceCode);

        // TODO: Consider moving the name up to the FieldDeclaration and removing the
        // VariableExpression node, it doesn't seem to add much value
        // Traverse up from the VariableExpression
        // <FieldDeclaration BeginColumn="18" BeginLine="2" DefiningType="MyClass"
        // DefiningType_CaseSafe="myclass" EndLine="2" FirstChild="false" Id="97297"
        // Label="FieldDeclaration" LastChild="true" Type="String" childIdx="1">
        //		<VariableExpression BeginColumn="18" BeginLine="2" DefiningType="MyClass"
        // DefiningType_CaseSafe="myclass" EndLine="2" FirstChild="true" Id="97319"
        // Label="VariableExpression" LastChild="true" Name="myString" Name_CaseSafe="mystring"
        // childIdx="0">
        //			<EmptyReferenceExpression BeginColumn="18" BeginLine="2" DefiningType="MyClass"
        // DefiningType_CaseSafe="myclass" EndLine="2" FirstChild="true" Id="97342"
        // Label="EmptyReferenceExpression" LastChild="true" childIdx="0"/>
        //		</VariableExpression>
        // </FieldDeclaration>
        FieldDeclarationVertex vertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.VARIABLE_EXPRESSION)
                                .has(Schema.NAME, "myString")
                                .not(has(Schema.IS_STANDARD, true))
                                .out(Schema.PARENT));
        MatcherAssert.assertThat(vertex.getCanonicalType(), equalTo("String"));
    }

    @Test
    public void testSyntheticGetterAndSetterWithoutBlockStatement() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       get; set;\n"
                    + "   }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        FieldVertex vertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(vertex.hasGetterBlock(), equalTo(false));
        MatcherAssert.assertThat(vertex.hasSetterBlock(), equalTo(false));
    }

    @Test
    public void testSyntheticGetterOnlyWithBlockStatement() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       get { return 'Hello'; }\n"
                    + "   }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        FieldVertex vertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(vertex.hasGetterBlock(), equalTo(true));
        MatcherAssert.assertThat(vertex.hasSetterBlock(), equalTo(false));
    }

    @Test
    public void testSyntheticSetterOnlyWithBlockStatement() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       set { aString = 'Hello' + value; }\n"
                    + "   }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);

        FieldVertex vertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(vertex.hasGetterBlock(), equalTo(false));
        MatcherAssert.assertThat(vertex.hasSetterBlock(), equalTo(true));
    }

    @Test
    public void testSyntheticPropertiesComplicated() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String noBlocks {\n"
                    + "       get; set; \n"
                    + "   }\n"
                    + "   public String getterWithBlock {\n"
                    + "       get { return 'Hello'; }\n"
                    + "       set; \n"
                    + "   }\n"
                    + "   public String setterWithBlock {\n"
                    + "       get;\n"
                    + "       set { setterWithBlock = 'Hello' + value; }\n"
                    + "   }\n"
                    + "   public String bothWithBlock {\n"
                    + "       get { return 'Hello'; }\n"
                    + "       set { bothWithBlock = 'Hello' + value; }\n"
                    + "   }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        FieldVertex field;

        field =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .has(Schema.NAME, "noBlocks")
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(field.hasGetterBlock(), equalTo(false));
        MatcherAssert.assertThat(field.hasSetterBlock(), equalTo(false));

        field =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .has(Schema.NAME, "getterWithBlock")
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(field.hasGetterBlock(), equalTo(true));
        MatcherAssert.assertThat(field.hasSetterBlock(), equalTo(false));

        field =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .has(Schema.NAME, "setterWithBlock")
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(field.hasGetterBlock(), equalTo(false));
        MatcherAssert.assertThat(field.hasSetterBlock(), equalTo(true));

        field =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.FIELD)
                                .has(Schema.NAME, "bothWithBlock")
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(field.hasGetterBlock(), equalTo(true));
        MatcherAssert.assertThat(field.hasSetterBlock(), equalTo(true));
    }

    @Test
    public void testTernaryExpression() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(Boolean b) {\n"
                    + "       String s = b ? 'Hello' : 'Goodbye';\n"
                    + "    }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        TernaryExpressionVertex ternaryExpression =
                TestUtil.getVertexOnLine(g, TernaryExpressionVertex.class, 3);
        MatcherAssert.assertThat(
                TestUtil.chainedVertexToString(ternaryExpression.getTrueValue()), equalTo("Hello"));
        MatcherAssert.assertThat(
                TestUtil.chainedVertexToString(ternaryExpression.getFalseValue()),
                equalTo("Goodbye"));
    }

    @Test
    public void testGenerics() {
        String[] sourceCode = {
            "public class MyClass implements Database.Batchable<sObject> {\n"
                    + "    public void execute(Database.BatchableContext bc, list<sObject> scope){\n"
                    + "    }\n"
                    + "    public void finish(Database.BatchableContext bc){\n"
                    + "    }\n"
                    + "    public Database.QueryLocator start(Database.BatchableContext BC) {\n"
                    + "    	return null;\n"
                    + "    }\n"
                    + "}"
        };

        TestUtil.buildGraph(g, sourceCode);
        UserClassVertex userClassVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyClass"));

        MatcherAssert.assertThat(
                userClassVertex.getInterfaceNames(), contains("Database.Batchable<sObject>"));
    }

    @Test
    public void testEnumSwitchStatement() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(DisplayType dt) {\n"
                        + "		String s;\n"
                        + "       switch on dt {\n"
                        + "       	when ADDRESS, CURRENCY {\n"
                        + "           	System.debug('address or currency');\n"
                        + "           }\n"
                        + "       	when ANYTYPE {\n"
                        + "           	System.debug('anytype');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);

        SwitchStatementVertex switchStatement =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.SWITCH_STATEMENT, 4);
        List<CaseVertex> caseVertices = switchStatement.getCaseVertices();
        MatcherAssert.assertThat(caseVertices, hasSize(equalTo(4)));

        IdentifierCaseVertex identifierCase;
        LiteralCaseVertex literalCase;

        identifierCase = (IdentifierCaseVertex) caseVertices.get(0);
        MatcherAssert.assertThat(identifierCase.getIdentifier(), equalTo("ADDRESS"));

        identifierCase = (IdentifierCaseVertex) caseVertices.get(1);
        MatcherAssert.assertThat(identifierCase.getIdentifier(), equalTo("CURRENCY"));

        identifierCase = (IdentifierCaseVertex) caseVertices.get(2);
        MatcherAssert.assertThat(identifierCase.getIdentifier(), equalTo("ANYTYPE"));

        literalCase = (LiteralCaseVertex) caseVertices.get(3);
        LiteralExpressionVertex literalExpression = literalCase.getLiteralExpression();
        MatcherAssert.assertThat(literalExpression, instanceOf(LiteralExpressionVertex.Null.class));
    }
}

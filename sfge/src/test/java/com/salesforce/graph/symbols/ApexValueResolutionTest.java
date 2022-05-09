package com.salesforce.graph.symbols;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.DmlDeleteStatementVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.ApexValueAccumulator;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates usage of {@link ApexValue#getReturnedFrom()} and {@link ApexValue#getChain()}
 * methods.
 */
public class ApexValueResolutionTest {
    private static final String OBJECT_NAME = "MyObject__c";

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * Demonstrates that the describe variable can be inspected to discover more information about
     * where it came from
     */
    @Test
    public void testDescribeSObjectResultSimple() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       SObjectType objType = MyObject__c.SObjectType;\n"
                    + "       Schema.DescribeSObjectResult describe = objType.getDescribe();\n"
                    + "       System.debug(describe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        DescribeSObjectResult describeSObjectResult;

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        describeSObjectResult = (DescribeSObjectResult) results.get(5).get();
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo(OBJECT_NAME));
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) describeSObjectResult.getInvocable().get())
                        .getMethodName(),
                equalTo("getDescribe"));

        // Verify that we can traverse back to establish the chain of method calls
        List<ApexValue<?>> chain = describeSObjectResult.getChain();
        MatcherAssert.assertThat(chain, hasSize(equalTo(2)));

        // Figure out which SObjectType returned the DescribeSObjectResult
        SObjectType sObjectType = (SObjectType) chain.get(0);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo(OBJECT_NAME));

        describeSObjectResult = (DescribeSObjectResult) chain.get(1);
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo(OBJECT_NAME));
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) describeSObjectResult.getInvocable().get())
                        .getMethodName(),
                equalTo("getDescribe"));
    }

    @Test
    public void testDescribeSObjectResultArray() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       Schema.DescribeSObjectResult[] describe = Schema.describeSObjects(new String[]{'Account','Contact'});\n"
                    + "       System.debug(describe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        ApexListValue apexListValue;
        DescribeSObjectResult describeSObjectResult;

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        apexListValue = (ApexListValue) results.get(4).get();
        MatcherAssert.assertThat(apexListValue.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) apexListValue.getInvocable().get()).getMethodName(),
                equalTo("describeSObjects"));

        MatcherAssert.assertThat(apexListValue.getValues(), hasSize(equalTo(2)));

        describeSObjectResult = (DescribeSObjectResult) apexListValue.getValues().get(0);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo("Account"));

        describeSObjectResult = (DescribeSObjectResult) apexListValue.getValues().get(1);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo("Contact"));
    }

    @Test
    public void testDescribeSObjectResultArrayIndexedInto() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       Schema.DescribeSObjectResult describe = Schema.describeSObjects(new String[]{'Account','Contact'})[0];\n"
                    + "       System.debug(describe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        ApexListValue apexListValue;
        DescribeSObjectResult describeSObjectResult;

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        describeSObjectResult = (DescribeSObjectResult) results.get(4).get();
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) describeSObjectResult.getInvocable().get())
                        .getMethodName(),
                equalTo("describeSObjects"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo("Account"));
    }

    /**
     * Demonstrates that the describe variable can be inspected to discover more information about
     * where it came from
     */
    @Test
    public void testDescribeSObjectResultChainedMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       Schema.DescribeSObjectResult describe = MyObject__c.SObjectType.getDescribe();\n"
                    + "       System.debug(describe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        DescribeSObjectResult describeSObjectResult;

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        describeSObjectResult = (DescribeSObjectResult) results.get(4).get();
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo(OBJECT_NAME));
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) describeSObjectResult.getInvocable().get())
                        .getMethodName(),
                equalTo("getDescribe"));

        // Verify that we can traverse back to establish the chain of method calls
        List<ApexValue<?>> chain = describeSObjectResult.getChain();
        MatcherAssert.assertThat(chain, hasSize(equalTo(2)));

        // Figure out which SObjectType returned the DescribeSObjectResult
        SObjectType sObjectType = (SObjectType) chain.get(0);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo(OBJECT_NAME));

        describeSObjectResult = (DescribeSObjectResult) chain.get(1);
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo(OBJECT_NAME));
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) describeSObjectResult.getInvocable().get())
                        .getMethodName(),
                equalTo("getDescribe"));
    }

    /**
     * Demonstrates that the isDeletable method can be inspected to discover more information about
     * where it came from
     */
    @Test
    public void testDescribeSObjectResultUsedInIf() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       SObjectType objType = MyObject__c.SObjectType;\n"
                    + "       Schema.DescribeSObjectResult describe = objType.getDescribe();\n"
                    + "       if (describe.isDeletable()) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlDeleteStatementVertex> dmlDeleteStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                            dmlDeleteStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsDeletableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    @Test
    public void testConstructor() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final Schema.DescribeSObjectResult describe;\n"
                    + "    public MyClass() {\n"
                    + "       SObjectType objType = MyObject__c.SObjectType;\n"
                    + "       describe = objType.getDescribe();\n"
                    + "    }\n"
                    + "   public void deleteIt() {\n"
                    + "       if (describe.isDeletable()) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static void foo() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.deleteIt();\n"
                    + "   }\n"
                    + "}\n",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getMethodVertex(g, "MyOtherClass", "foo");
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlDeleteStatementVertex> dmlDeleteStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                            dmlDeleteStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsDeletableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    @Test
    public void testVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void bar(String objectName, String fieldName) {\n"
                    + "       fieldName = fieldName.toLowerCase();\n"
                    + "       Map<String, Schema.DescribeSObjectResult> objectDescribes = new Map<String, Schema.DescribeSObjectResult>();\n"
                    + "       Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe();\n"
                    + "       Map<String, Map<String, Schema.SObjectField>> fieldTokens = new Map<String,Map<String, Schema.SObjectField>>();\n"
                    + "       Map<String, Map<String, Schema.DescribeFieldResult>> fieldDescribes = new Map<String,Map<String, Schema.DescribeFieldResult>>();\n"
                    + "       Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "       objectDescribes.put(objectName, objDescribe);\n"
                    + "       System.debug(objDescribe);\n"
                    + "       objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "       fieldTokens.put(objectName, objectDescribes.get(objectName).fields.getMap());\n"
                    + "       Schema.DescribeFieldResult dfr1 = fieldTokens.get(objectName).get(fieldName).getDescribe();\n"
                    + "       fieldDescribes.put(objectName, new Map<String, Schema.DescribeFieldResult>());\n"
                    + "       fieldDescribes.get(objectName).put(fieldName, dfr1);\n"
                    + "       System.debug(dfr1);\n"
                    + "       Schema.DescribeFieldResult dfr2 = fieldDescribes.get(objectName).get(fieldName);\n"
                    + "       System.debug(dfr2);\n"
                    + "   }\n"
                    + "   public static void foo() {\n"
                    + "       List<String> fields = new List<String>{'Name', 'Phone'};\n"
                    + "       for (String field : fields) {\n"
                    + "           bar('Account', field);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        ApexPathExpanderConfig expanderConfig =
                ApexPathUtil.getFullConfiguredPathExpanderConfigBuilder().build();

        ApexPath path = TestUtil.getSingleApexPath(config, expanderConfig, "foo");

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(
                        Pair.of("System.debug", "objDescribe"),
                        Pair.of("System.debug", "dfr1"),
                        Pair.of("System.debug", "dfr2"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> values;
        DescribeFieldResult describeFieldResult;
        DescribeSObjectResult describeSObjectResult;

        //        values = visitor.getSingleResultPerLineByName("objDescribe");
        //        MatcherAssert.assertThat(values.entrySet(), hasSize(equalTo(1)));
        //        describeSObjectResult = (DescribeSObjectResult)values.get(11).orElse(null);
        //
        // MatcherAssert.assertThat(TestUtil.apexValueToString(describeSObjectResult.getSObjectType()
        //                .get().getAssociatedObjectType()), equalTo("Account"));
        //
        //        values = visitor.getSingleResultPerLineByName("dfr1");
        //        MatcherAssert.assertThat(values.entrySet(), hasSize(equalTo(1)));
        //        describeFieldResult = (DescribeFieldResult)values.get(17).orElse(null);
        //
        // MatcherAssert.assertThat(TestUtil.apexValueToString(describeFieldResult.getFieldName()),
        // equalTo("Name"));
        //
        // MatcherAssert.assertThat(TestUtil.apexValueToString(describeFieldResult.getDescribeSObjectResult()
        //                .get().getSObjectType().get().getAssociatedObjectType()),
        // equalTo("Account"));

        values = visitor.getSingleResultPerLineByName("dfr2");
        MatcherAssert.assertThat(values.entrySet(), hasSize(equalTo(1)));

        describeFieldResult = (DescribeFieldResult) values.get(18).orElse(null);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeFieldResult.getFieldName()),
                equalTo("name, phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        describeFieldResult
                                .getDescribeSObjectResult()
                                .get()
                                .getSObjectType()
                                .get()
                                .getType()),
                equalTo("Account"));
    }

    /**
     * Demonstrates that the canDelete member method can be inspected to discover more information
     * about where it came from
     */
    @Test
    public void testMemberMethodUsedInIf() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       if (canDelete()) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public static Boolean canDelete() {\n"
                    + "       SObjectType objType = MyObject__c.SObjectType;\n"
                    + "       Schema.DescribeSObjectResult describe = objType.getDescribe();\n"
                    + "       return describe.isDeletable();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlDeleteStatementVertex> dmlDeleteStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                            dmlDeleteStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsDeletableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    /**
     * Demonstrates that the b variable can be inspected to discover more information about where it
     * came from
     */
    @Test
    public void testVariableUsedInIf() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       Boolean b = canDelete();\n"
                    + "       if (b) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public static Boolean canDelete() {\n"
                    + "       SObjectType objType = MyObject__c.SObjectType;\n"
                    + "       Schema.DescribeSObjectResult describe = objType.getDescribe();\n"
                    + "       return describe.isDeletable();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlDeleteStatementVertex> dmlDeleteStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                            dmlDeleteStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsDeletableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    @Test
    public void testSObjectFieldMap() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe().isCreateable()) {\n"
                        + "           Account a = new Account(Name = 'Acme Inc');\n"
                        + "           insert a;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlInsertStatementVertex> dmlInsertStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                            dmlInsertStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsFieldCreateableWasCalled(apexBooleanValue);
        }
        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    @Test
    public void testFieldNameAndObjectTypePassedAsParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo(String objectName, String fieldName) {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get(objectName).newSObject();\n"
                        + "       obj.put(fieldName, 'Acme Inc.');\n"
                        + "       if (Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap().get(fieldName).getDescribe().isCreateable()) {\n"
                        + "            insert obj;\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlInsertStatementVertex> dmlInsertStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                            dmlInsertStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsFieldCreateableWasCalled(apexBooleanValue, "fieldName", "objectName");
        }
        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    /**
     * Demonstrates storing results in a map and retrieving them for later use. This is a simplified
     * version of the static caching pattern used by NPSP. It removes the singleton and the if/else
     * caching blocks.
     */
    @Test
    // Copied from github.com/SalesforceFoundation/NPSP
    public void testDescribeSObjectResultStoredInAMap() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void foo() {\n"
                    + "       Schema.DescribeSObjectResult describe = DescribeSingleton.getObjectDescribe(MyObject__c.SObjectType);\n"
                    + "       if (describe.isDeletable()) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   private static void fillMapsForObject(string objectName) {\n"
                    + "       gd = Schema.getGlobalDescribe();\n"
                    + "       Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "       objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlDeleteStatementVertex> dmlDeleteStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                            dmlDeleteStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsDeletableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    /**
     * Demonstrates the caching pattern used by NPSP in BDI_DataImportDeleteBTN_CTRL.cls TODO: Add
     * static caching, this doesn't need the SObjecResultCollapser
     */
    @Test
    // Copied from github.com/SalesforceFoundation/NPSP
    public void testSingletonWithCollapsers() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void foo() {\n"
                    + "       if (checkDelete()) {\n"
                    + "           MyObject__c myObj = new MyObject__c();\n"
                    + "           delete myObj;\n"
                    + "       } else {\n"
                    + "           System.debug('not deletable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public Boolean checkDelete() {\n"
                    + "       return PermissionsSingleton.getInstance().canDelete(MyObject__c.SObjectType);\n"
                    + "   }\n"
                    + "}",
            "public class PermissionsSingleton {\n"
                    + "   private static PermissionsSingleton singleton;\n"
                    + "   public static PermissionsSingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new PermissionsSingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "   }\n"
                    + "   public Boolean canDelete(SObjectType sObjectType) {\n"
                    + "       return DescribeSingleton.getObjectDescribe(sObjectType).isDeletable();\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   private static void fillMapsForObject(string objectName) {\n"
                    + "       gd = Schema.getGlobalDescribe();\n"
                    + "       Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "       objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        ApexPathExpanderConfig apexPathExpanderConfig =
                ApexPathUtil.getFullConfiguredPathExpanderConfig();
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method, apexPathExpanderConfig);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlDeleteStatementVertex> dmlDeleteStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                            dmlDeleteStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsDeletableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlDeleteStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    /** TODO */
    @Test
    public void testDescribeFieldResultVariableUsedInIf() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       DescribeFieldResult dfr = Schema.SObjectType.Account.fields.Name;\n"
                    + "       if (dfr.isCreateable()) {\n"
                    + "           Account a = new Account(Name = 'Acme Inc');\n"
                    + "           insert a;\n"
                    + "       } else {\n"
                    + "           System.debug('not insertable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlInsertStatementVertex> dmlInsertStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                            dmlInsertStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsFieldCreateableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlInsertStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlInsertStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    @Test
    public void testDescribeFieldResultMethodUsedInIf() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       if (Schema.SObjectType.Account.fields.Name.isCreateable()) {\n"
                    + "           Account a = new Account(Name = 'Acme Inc');\n"
                    + "           insert a;\n"
                    + "       } else {\n"
                    + "           System.debug('not insertable');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<StandardConditionVertex> allStandardConditions = new ArrayList<>();
        for (ApexPath path : paths) {
            List<DmlInsertStatementVertex> dmlInsertStatementVertices = new ArrayList<>();

            StandardConditionAccumulator visitor =
                    new StandardConditionAccumulator() {
                        @Override
                        public boolean visit(
                                DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                            dmlInsertStatementVertices.add(vertex);
                            return true;
                        }
                    };
            SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
            allStandardConditions.addAll(visitor.standardConditionValues.keySet());

            // Make sure we only visited one standard condition
            MatcherAssert.assertThat(
                    visitor.standardConditionValues.entrySet(), hasSize(equalTo(1)));
            Map.Entry<StandardConditionVertex, ApexValue<?>> e =
                    visitor.standardConditionValues.entrySet().iterator().next();
            StandardConditionVertex standardCondition = e.getKey();
            ApexBooleanValue apexBooleanValue = (ApexBooleanValue) e.getValue();
            assertBooleanIsFieldCreateableWasCalled(apexBooleanValue);

            if (standardCondition instanceof StandardConditionVertex.Negative) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Negative.class));
                MatcherAssert.assertThat(dmlInsertStatementVertices, hasSize(equalTo(0)));
            } else if (standardCondition instanceof StandardConditionVertex.Positive) {
                // Verify that the negative path doesn't contain a delete statement
                MatcherAssert.assertThat(
                        e.getKey(), instanceOf(StandardConditionVertex.Positive.class));
                MatcherAssert.assertThat(dmlInsertStatementVertices, hasSize(equalTo(1)));
            } else {
                throw new UnexpectedException(standardCondition);
            }
        }

        // Sanity check that all paths were visited as expected
        MatcherAssert.assertThat(allStandardConditions, hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Positive)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                allStandardConditions.stream()
                        .filter(v -> v instanceof StandardConditionVertex.Negative)
                        .collect(Collectors.toList()),
                hasSize(equalTo(1)));
    }

    @Test
    public void testDescribeObjectResultForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       String [] objectsToCheck = new String [] {'Account', 'Contact', 'MyObject__c'};\n"
                    + "       for (String objectToCheck : objectsToCheck) {\n"
                    + "           DescribeSObjectResult describe = Schema.getGlobalDescribe().get(objectToCheck);\n"
                    + "           System.debug(describe);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        SObjectType sObjectType = (SObjectType) results.get(6).get();
        MatcherAssert.assertThat(sObjectType.isDeterminant(), equalTo(true));
        ApexForLoopValue apexForLoopValue = (ApexForLoopValue) sObjectType.getType().get();

        List<ApexValue<?>> items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        String[] expectedValues = new String[] {"Account", "Contact", "MyObject__c"};
        for (int i = 0; i < expectedValues.length; i++) {
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(items.get(i)), equalTo(expectedValues[i]));
        }
    }

    @Test
    public void testDescribeObjectResultForLoopWithVariables() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       String a = 'Account';\n"
                    + "       String b = 'Contact';\n"
                    + "       String c = 'MyObject__c';\n"
                    + "       String [] objectsToCheck = new String [] {a, b, c};\n"
                    + "       for (String objectToCheck : objectsToCheck) {\n"
                    + "           DescribeSObjectResult describe = Schema.getGlobalDescribe().get(objectToCheck);\n"
                    + "           System.debug(describe);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        SObjectType sObjectType = (SObjectType) results.get(9).get();
        MatcherAssert.assertThat(sObjectType.isDeterminant(), equalTo(true));
        ApexForLoopValue apexForLoopValue = (ApexForLoopValue) sObjectType.getType().get();

        List<ApexValue<?>> items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        String[] expectedValues = new String[] {"Account", "Contact", "MyObject__c"};
        for (int i = 0; i < expectedValues.length; i++) {
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(items.get(i)), equalTo(expectedValues[i]));
        }
    }

    @Test
    public void testDescribeObjectResultForLoopWithUnresolvedVariables() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo(String a, String b, String c) {\n"
                    + "       String [] objectsToCheck = new String [] {a, b, c};\n"
                    + "       for (String objectToCheck : objectsToCheck) {\n"
                    + "           DescribeSObjectResult describe = Schema.getGlobalDescribe().get(objectToCheck);\n"
                    + "           System.debug(describe);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        SObjectType sObjectType = (SObjectType) results.get(6).get();
        MatcherAssert.assertThat(sObjectType.isDeterminant(), equalTo(true));
        ApexForLoopValue apexForLoopValue = (ApexForLoopValue) sObjectType.getType().get();

        List<ApexValue<?>> items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        for (int i = 0; i < 3; i++) {
            MatcherAssert.assertThat(items.get(i), instanceOf(ApexStringValue.class));
            MatcherAssert.assertThat(items.get(i).isIndeterminant(), equalTo(true));
        }
    }

    @Test
    public void testDescribeFieldResultForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       String [] fieldsToCheck = new String [] {'Name', 'Phone', 'Foo__c'};\n"
                    + "       for (String fieldToCheck : fieldsToCheck) {\n"
                    + "           DescribeFieldResult describe = Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe();\n"
                    + "           System.debug(describe);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        DescribeFieldResult describeFieldResult = (DescribeFieldResult) results.get(6).get();
        MatcherAssert.assertThat(describeFieldResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        describeFieldResult.getDescribeSObjectResult().get().getSObjectType()),
                equalTo("Account"));
        ApexForLoopValue apexForLoopValue =
                (ApexForLoopValue) describeFieldResult.getFieldName().get();

        List<ApexValue<?>> items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        String[] expectedValues = new String[] {"Name", "Phone", "Foo__c"};
        for (int i = 0; i < expectedValues.length; i++) {
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(items.get(i)), equalTo(expectedValues[i]));
        }
    }

    @Test
    public void testDescribeFieldResultForLoopWithVariables() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       String a = 'Name';\n"
                    + "       String b = 'Phone';\n"
                    + "       String c = 'Foo__c';\n"
                    + "       String [] fieldsToCheck = new String [] {a, b, c};\n"
                    + "       for (String fieldToCheck : fieldsToCheck) {\n"
                    + "           DescribeFieldResult describe = Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe();\n"
                    + "           System.debug(describe);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        DescribeFieldResult describeFieldResult = (DescribeFieldResult) results.get(9).get();
        MatcherAssert.assertThat(describeFieldResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        describeFieldResult.getDescribeSObjectResult().get().getSObjectType()),
                equalTo("Account"));
        ApexForLoopValue apexForLoopValue =
                (ApexForLoopValue) describeFieldResult.getFieldName().get();

        List<ApexValue<?>> items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        String[] expectedValues = new String[] {"Name", "Phone", "Foo__c"};
        for (int i = 0; i < expectedValues.length; i++) {
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(items.get(i)), equalTo(expectedValues[i]));
        }
    }

    @Test
    public void testDescribeFieldResultForLoopWithUnresolvedVariables() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo(String a, String b, String c) {\n"
                    + "       String [] fieldsToCheck = new String [] {a, b, c};\n"
                    + "       for (String fieldToCheck : fieldsToCheck) {\n"
                    + "           DescribeFieldResult describe = Schema.SObjectType.Account.fields.getMap().get(fieldToCheck).getDescribe();\n"
                    + "           System.debug(describe);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        DescribeFieldResult describeFieldResult = (DescribeFieldResult) results.get(6).get();
        MatcherAssert.assertThat(describeFieldResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        describeFieldResult.getDescribeSObjectResult().get().getSObjectType()),
                equalTo("Account"));
        ApexForLoopValue apexForLoopValue =
                (ApexForLoopValue) describeFieldResult.getFieldName().get();

        List<ApexValue<?>> items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        for (int i = 0; i < 3; i++) {
            MatcherAssert.assertThat(items.get(i), instanceOf(ApexStringValue.class));
            MatcherAssert.assertThat(items.get(i).isIndeterminant(), equalTo(true));
        }
    }

    /** Verify that multiple for loops work as expected */
    @Test
    public void testDescribeObjectAndFieldResultForLoop() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void foo() {\n"
                    + "       String [] objectsToCheck = new String [] {'Account', 'Contact', 'MyObject__c'};\n"
                    + "       for (String objectToCheck : objectsToCheck) {\n"
                    + "           String [] fieldsToCheck = new String [] {'Name', 'Phone', 'Foo__c'};\n"
                    + "           for (String fieldToCheck : fieldsToCheck) {\n"
                    + "               DescribeFieldResult describe = Schema.getGlobalDescribe().get(objectToCheck).getDescribe().fields.getMap().get(fieldToCheck).getDescribe();\n"
                    + "               System.debug(describe);\n"
                    + "           }\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(Pair.of("System.debug", "describe"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("describe");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));

        DescribeFieldResult describeFieldResult = (DescribeFieldResult) results.get(8).get();
        MatcherAssert.assertThat(describeFieldResult.isDeterminant(), equalTo(true));

        List<ApexValue<?>> items;
        ApexForLoopValue apexForLoopValue;

        SObjectType sObjectType =
                describeFieldResult.getDescribeSObjectResult().get().getSObjectType().get();

        // Verify that the associated types can have multiple values
        apexForLoopValue = (ApexForLoopValue) sObjectType.getType().get();
        items = apexForLoopValue.getForLoopValues();
        String[] expectedObjectValue = new String[] {"Account", "Contact", "MyObject__c"};
        for (int i = 0; i < expectedObjectValue.length; i++) {
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(items.get(i)), equalTo(expectedObjectValue[i]));
        }

        // Verify that the associated fields can have multiple values
        apexForLoopValue = (ApexForLoopValue) describeFieldResult.getFieldName().get();
        items = apexForLoopValue.getForLoopValues();
        MatcherAssert.assertThat(items, hasSize(equalTo(3)));
        String[] expectedValues = new String[] {"Name", "Phone", "Foo__c"};
        for (int i = 0; i < expectedValues.length; i++) {
            MatcherAssert.assertThat(
                    TestUtil.apexValueToString(items.get(i)), equalTo(expectedValues[i]));
        }
    }

    @Test
    public void testStaticStringReference() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       String s = getError();\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "   public String getError() {\n"
                    + "       return MyOtherClass.AN_ERROR;\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String AN_ERROR = 'An Error';\n"
                    + "}\n"
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "doSomething");
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results = visitor.getSingleResultPerLineByName("s");
        MatcherAssert.assertThat(results.keySet(), hasSize(Matchers.equalTo(1)));
        MatcherAssert.assertThat(TestUtil.apexValueToString(results.get(4)), equalTo("An Error"));
    }

    @Test
    public void testInterfacePassedToMethod() {
        String[] sourceCode = {
            "public interface MyInterface {\n" + "   void doSomethingElse();\n" + "}\n",
            "public class MyClass implements MyInterface {\n"
                    + "   public void doSomethingElse() {\n"
                    + "       System.debug('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public void doSomething() {\n"
                    + "       logSomething(new MyClass());\n"
                    + "   }\n"
                    + "   public void logSomething(MyInterface mi) {\n"
                    + "       mi.doSomethingElse();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testSubclassPassedToMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomethingElse() {\n"
                    + "       System.debug('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MySubClass extends MyClass {\n" + "}\n",
            "public class MyOtherClass {\n"
                    + "   public void doSomething() {\n"
                    + "       logSomething(new MySubClass());\n"
                    + "   }\n"
                    + "   public void logSomething(MyClass mc) {\n"
                    + "       mc.doSomethingElse();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    /**
     * The value returned from #getValue should be an ApexStringValue instead of an ApexSingleValue.
     * The InnerScope class supplies the return type to the ApexValueBuilder.
     */
    @Test
    public void testReturnTypeIsUsedToDetermineApexValueType() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(MyOtherClass.getValue());\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String getValue() {\n"
                    + "       return MyUnresolvedClass.getSomething();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MethodCallExpressionVertex valueVertex =
                (MethodCallExpressionVertex) value.getValueVertex().orElse(null);
        MatcherAssert.assertThat(valueVertex.getMethodName(), equalTo("getSomething"));
    }

    /** Assert that a value assigned to a ApexCustomValue is stored correctly */
    @Test
    public void testAssignCustomSettingFieldSimple() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       MySettings__c settings = MySettings__c.getOrgDefaults();\n"
                    + "       configSettings(settings);\n"
                    + "       System.debug(settings.MyField1__c);\n"
                    + "   }\n"
                    + "   public static void configSettings(MySettings__c settings) {\n"
                    + "       settings.MyField1__c = addNamespace('Foo');\n"
                    + "   }\n"
                    + "   public static string addNamespace(String str) {\n"
                    + "       return 'MyNS__' + str;\n"
                    + "   }\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("MyNS__Foo"));
    }

    /**
     * Assert that a complex set of if/else statements are condensed into a single path, retaining
     * the result
     */
    @Test
    public void testAssignStringComplex() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static string myString;\n"
                    + "   public static void doSomething() {\n"
                    + "       configSettings();\n"
                    + "       printSettings();\n"
                    + "   }\n"
                    + "   public static void configSettings() {\n"
                    + "       addNamespace('Foo');\n"
                    + "   }\n"
                    + "   public static void printSettings() {\n"
                    + "       if (myString == null) {\n"
                    + "           System.debug('It is null');\n"
                    + "       } else {\n"
                    + "           System.debug(myString);\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public static void addNamespace(String str) {\n"
                    + "        if (str != '') {\n"
                    + "           myString = str;\n"
                    + "        } else {\n"
                    + "           myString = 'Bar';\n"
                    + "        }\n"
                    + "   }\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Foo"));
    }

    @Test
    public void testNullReturnHasCorrectType() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static string myString;\n"
                    + "   public static void doSomething() {\n"
                    + "       System.debug(getList());\n"
                    + "   }\n"
                    + "   public static List<String> getList() {\n"
                    + "       return null;\n"
                    + "   }\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isNull(), equalTo(true));
    }

    /**
     * Ensure that the method call return value is used as the key when the method call is passed to
     * the #put method
     */
    @Test
    public void testKeyIsMethodCall() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "   	MyObject__c obj = new MyObject__c();\n"
                    + "   	obj = MyOtherClass.configure(obj, 'Key1', 'Value1');\n"
                    + "   	System.debug(obj.Field1__c);\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String KEY_1 = 'Key1';\n"
                    + "   public static String KEY_2 = 'Key2';\n"
                    + "   public static final Map<String, String> MAP_1 = new Map<String, String>{\n"
                    + "   	KEY_1 => 'Field1__c',"
                    + "   	KEY_2 => 'Field2__c'"
                    + "   };\n"
                    + "   public static MyObject__c configure(MyObject__c obj, String key, Object value) {\n"
                    +
                    // key = Field1__c, value = Value1
                    "   	obj.put(MAP_1.get(key), value);\n"
                    + "   	return obj;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo("Value1"));
    }

    /** Ensure that an indeterminant value can be used as a key without throwing an exception */
    @Test
    public void testKeyIsIndeterminantVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(String s) {\n"
                    + "   	MyObject__c obj = new MyObject__c();\n"
                    + "   	obj.put(s, 'Value1');\n"
                    + "   	System.debug(obj);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getApexValueProperties().size(), equalTo(1));
    }

    /** Ensure that a map populated within a for loop is populated without throwing an exception */
    @Test
    public void testPutInsideForLoopVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "   	MyObject__c obj = new MyObject__c();\n"
                    + "   	obj = MyOtherClass.configure(obj, 'Value1');\n"
                    + "   	System.debug(obj);\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String KEY_1 = 'Key1';\n"
                    + "   public static String KEY_2 = 'Key2';\n"
                    + "   public static final Map<String, String> MAP_1 = new Map<String, String>{\n"
                    + "   	KEY_1 => 'Field1__c',"
                    + "   	KEY_2 => 'Field2__c'"
                    + "   };\n"
                    + "   public static MyObject__c configure(MyObject__c obj, Object value) {\n"
                    + "   	for (String key : MAP_1.keySet()) {\n"
                    + "   		obj.put(MAP_1.get(key), value);\n"
                    + "   	}\n"
                    + "   	return obj;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getApexValueProperties().size(), equalTo(1));
    }

    /** Ensure that a map populated within a for loop is populated without throwing an exception */
    @Test
    public void testKeyIsForLoopVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "   	MyObject__c obj = new MyObject__c();\n"
                    + "   	obj = MyOtherClass.configure(obj, 'Value1');\n"
                    + "   	System.debug(obj);\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String KEY_1 = 'Key1';\n"
                    + "   public static String KEY_2 = 'Key2';\n"
                    + "   public static final Map<String, String> MAP_1 = new Map<String, String>{\n"
                    + "   	KEY_1 => 'Field1__c',"
                    + "   	KEY_2 => 'Field2__c'"
                    + "   };\n"
                    + "   public static MyObject__c configure(MyObject__c obj, Object value) {\n"
                    + "   	for (String key : MAP_1.keySet()) {\n"
                    + "   		obj.put(key, value);\n"
                    + "   	}\n"
                    + "   	return obj;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getApexValueProperties().size(), equalTo(1));
    }

    @Test
    public void testRetrieveFromStaticMap() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "   	MyObject__c obj = new MyObject__c();\n"
                    + "   	MyOtherClass.logSomething('Key1');\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String KEY_1 = 'Key1';\n"
                    + "   public static String KEY_2 = 'Key2';\n"
                    + "   public static final Map<String, String> MAP_1 = new Map<String, String>{\n"
                    + "   	KEY_1 => 'Field1__c',"
                    + "   	KEY_2 => 'Field2__c'"
                    + "   };\n"
                    + "   public static void logSomething(String key) {\n"
                    + "   	System.debug(MAP_1.get(key));\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Field1__c"));
    }

    /**
     * Asserts that the boolean value was returned from
     * MyObject__c.SObjectType.getDescribe().isDeletable(). This method only looks at the last 3
     * items in the ApexValue chain. The chain can be arbitrarily deep, the testSingleton is 5
     * levels deep, but the first two levels aren't consequential. We only need to know what
     * generated the boolean value. The method demonstrates validating the value by manually
     * traversing the {@link ApexValue#getReturnedFrom()} relations or using the helper method
     * {@link ApexValue#getChain()}
     */
    private void assertBooleanIsDeletableWasCalled(ApexBooleanValue apexBooleanValue) {
        MethodCallExpressionVertex methodCallExpressionVertex;
        DescribeSObjectResult describeSObjectResult;
        SObjectType sObjectType;

        // **************************************************
        // Validation Method 1. Use ApexValue#getReturnedFrom
        // **************************************************
        // Validate it is a boolean that was returned from the "isDeletable" method
        methodCallExpressionVertex =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(
                methodCallExpressionVertex.getMethodName(), equalToIgnoringCase("isDeletable"));

        // Validate it is an DescribeSObjectResult that corresponds to the MyObject__c object
        describeSObjectResult = (DescribeSObjectResult) apexBooleanValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType()),
                equalTo(OBJECT_NAME));

        // Validate it is an SObjectType that corresponds to the MyObject__c object
        sObjectType = (SObjectType) describeSObjectResult.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo(OBJECT_NAME));

        // **************************************************
        // Validation Method 2. Use ApexValue#getChain
        // **************************************************
        List<ApexValue<?>> chain = apexBooleanValue.getChain();
        // We only care that the chain has at least 3 items, it's possible the chain is longer but
        // our verification
        // only needs to look at the the following 3 items
        // 1. isDeletable()             // chain.size() - 1
        // 2. getDescribe()             // chain.size() - 2
        // 3. MyObject__c.SObjectType   // chain.size() - 3
        MatcherAssert.assertThat(chain, hasSize(greaterThanOrEqualTo(3)));

        // Validate it is a boolean that was returned from the "isDeletable" method
        apexBooleanValue = (ApexBooleanValue) chain.get(chain.size() - 1);
        MatcherAssert.assertThat(apexBooleanValue.isIndeterminant(), equalTo(true));
        methodCallExpressionVertex =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(
                methodCallExpressionVertex.getMethodName(), equalToIgnoringCase("isDeletable"));

        // Validate it is an DescribeSObjectResult that corresponds to the MyObject__c object
        describeSObjectResult = (DescribeSObjectResult) chain.get(chain.size() - 2);
        MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo(OBJECT_NAME));

        // Validate it is an SObjectType that corresponds to the MyObject__c object
        sObjectType = (SObjectType) chain.get(chain.size() - 3);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo(OBJECT_NAME));
    }

    /**
     * TODO
     *
     * @param apexBooleanValue
     */
    private void assertBooleanIsFieldCreateableWasCalled(ApexBooleanValue apexBooleanValue) {
        MethodCallExpressionVertex methodCallExpressionVertex;
        DescribeFieldResult describeFieldResult;
        DescribeSObjectResult describeSObjectResult;
        SObjectType sObjectType;

        // **************************************************
        // Validation Method 1. Use ApexValue#getReturnedFrom
        // **************************************************
        // Validate it is a boolean that was returned from the "isCreateable" method
        methodCallExpressionVertex =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(
                methodCallExpressionVertex.getMethodName(), equalToIgnoringCase("isCreateable"));

        // Validate it is a DescribeFieldResult that corresponds to the "Name" field
        describeFieldResult = (DescribeFieldResult) apexBooleanValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeFieldResult.getFieldName()), equalTo("Name"));

        // Validate it is a DescribeSObjectResult that corresponds to the "Account" object
        describeSObjectResult = describeFieldResult.getDescribeSObjectResult().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo("Account"));

        // **************************************************
        // Validation Method 2. Use ApexValue#getChain
        // **************************************************
        List<ApexValue<?>> chain = apexBooleanValue.getChain();
        MatcherAssert.assertThat(chain, hasSize(greaterThanOrEqualTo(2)));

        // Validate it is a boolean that was returned from the "isCreateable" method
        apexBooleanValue = (ApexBooleanValue) chain.get(chain.size() - 1);
        MatcherAssert.assertThat(apexBooleanValue.isIndeterminant(), equalTo(true));
        methodCallExpressionVertex =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(
                methodCallExpressionVertex.getMethodName(), equalToIgnoringCase("isCreateable"));

        // Validate it is an DescribeSObjectResult that corresponds to the MyObject__c object
        describeFieldResult = (DescribeFieldResult) chain.get(chain.size() - 2);
        MatcherAssert.assertThat(describeFieldResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeFieldResult.getFieldName()), equalTo("Name"));

        describeSObjectResult = describeFieldResult.getDescribeSObjectResult().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType().get().getType()),
                equalTo("Account"));
    }

    private void assertBooleanIsFieldCreateableWasCalled(
            ApexBooleanValue apexBooleanValue,
            String expectedFieldNameVariable,
            String expectedObjectNameVariable) {
        MethodCallExpressionVertex methodCallExpressionVertex;
        DescribeFieldResult describeFieldResult;
        DescribeSObjectResult describeSObjectResult;
        SObjectType sObjectType;

        // **************************************************
        // Validation Method 1. Use ApexValue#getReturnedFrom
        // **************************************************
        // Validate it is a boolean that was returned from the "isCreateable" method
        methodCallExpressionVertex =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(
                methodCallExpressionVertex.getMethodName(), equalToIgnoringCase("isCreateable"));

        // Validate it is a DescribeFieldResult that corresponds to the "Name" field
        describeFieldResult = (DescribeFieldResult) apexBooleanValue.getReturnedFrom().get();
        final Optional<ApexValue<?>> fieldNameValue = describeFieldResult.getFieldName();

        MatcherAssert.assertThat(
                fieldNameValue.get().getVariableName().get(), equalTo(expectedFieldNameVariable));

        // Validate it is a DescribeSObjectResult that corresponds to the "Account" object
        describeSObjectResult = describeFieldResult.getDescribeSObjectResult().get();
        final ApexValue<?> associatedObjectType =
                describeSObjectResult.getSObjectType().get().getType().get();
        MatcherAssert.assertThat(
                associatedObjectType.getVariableName().get(), equalTo(expectedObjectNameVariable));

        // **************************************************
        // Validation Method 2. Use ApexValue#getChain
        // **************************************************
        List<ApexValue<?>> chain = apexBooleanValue.getChain();
        MatcherAssert.assertThat(chain, hasSize(greaterThanOrEqualTo(2)));

        // Validate it is a boolean that was returned from the "isCreateable" method
        apexBooleanValue = (ApexBooleanValue) chain.get(chain.size() - 1);
        MatcherAssert.assertThat(apexBooleanValue.isIndeterminant(), equalTo(true));
        methodCallExpressionVertex =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        MatcherAssert.assertThat(
                methodCallExpressionVertex.getMethodName(), equalToIgnoringCase("isCreateable"));

        // Validate it is an DescribeSObjectResult that corresponds to the MyObject__c object
        describeFieldResult = (DescribeFieldResult) chain.get(chain.size() - 2);
        MatcherAssert.assertThat(describeFieldResult.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(
                fieldNameValue.get().getVariableName().get(), equalTo(expectedFieldNameVariable));

        describeSObjectResult = describeFieldResult.getDescribeSObjectResult().get();
        final ApexValue<?> associatedObjectType1 =
                describeSObjectResult.getSObjectType().get().getType().get();
        MatcherAssert.assertThat(
                associatedObjectType1.getVariableName().get(), equalTo(expectedObjectNameVariable));
    }

    /** Accumulates StandardConditions visited in the MyClass class, mapping htem to the */
    private static class StandardConditionAccumulator extends DefaultNoOpPathVertexVisitor {
        protected final Map<StandardConditionVertex, ApexValue<?>> standardConditionValues;

        private StandardConditionAccumulator() {
            this.standardConditionValues = new HashMap<>();
        }

        private void afterVisitStandardCondition(
                StandardConditionVertex vertex, SymbolProvider symbols) {
            if (vertex.getDefiningType().equals("MyClass")) {
                ApexValue<?> apexValue;
                List<BaseSFVertex> children = vertex.getChildren();
                if (children.size() > 1) {
                    throw new UnexpectedException(vertex);
                }
                BaseSFVertex child = children.get(0);
                if (child instanceof MethodCallExpressionVertex) {
                    MethodCallExpressionVertex methodCallExpression =
                            (MethodCallExpressionVertex) child;
                    apexValue = symbols.getReturnedValue(methodCallExpression).get();
                } else if (child instanceof VariableExpressionVertex) {
                    apexValue = symbols.getApexValue((VariableExpressionVertex) child).get();
                } else {
                    throw new UnexpectedException(vertex);
                }
                ApexValue<?> previous = standardConditionValues.put(vertex, apexValue);
                if (previous != null) {
                    // Make sure that we don't overwrite any vertices
                    throw new UnexpectedException(vertex);
                }
            }
        }

        @Override
        public void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
            afterVisitStandardCondition(vertex, symbols);
        }

        @Override
        public void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
            afterVisitStandardCondition(vertex, symbols);
        }
    }
}

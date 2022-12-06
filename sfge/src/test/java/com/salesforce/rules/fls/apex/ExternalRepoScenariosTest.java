package com.salesforce.rules.fls.apex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexIntegerValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexMapValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.DmlDeleteStatementVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsValidationRepresentation;
import com.salesforce.testutils.BaseFlsTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** This test contains issues that were encountered in repos we have tested with. */
public class ExternalRepoScenariosTest extends BaseFlsTest {
    private AbstractPathBasedRule rule = ApexFlsViolationRule.getInstance();

    /** This code is used across three tests. */
    private static final String[] CACHED_PERM_CHECK_FOR_DELETION = {
        "public class MyClass {\n"
                + "   public static void deleteObject(Id myId) {\n"
                + "       Boolean canDelete = PermissionsUtil.getInstance().canDelete(MyObject__c.SObjectType);\n"
                + "       if (!canDelete) {\n"
                + "           throw new AuraHandledException('No access');\n"
                + "       }\n"
                + "       System.debug(canDelete);\n"
                + "       delete new MyObject__c(Id = myId);\n"
                + "   }\n"
                + "}\n",
        "public class PermissionsUtil {\n"
                + "    private static PermissionsUtil INSTANCE;\n"
                + "    public static PermissionsUtil getInstance(){\n"
                + "        if (INSTANCE == null) {\n"
                + "            INSTANCE = new PermissionsUtil();\n"
                + "        }\n"
                + "        return INSTANCE;\n"
                + "    }\n"
                + "    public Boolean canDelete(SObjectType sObjectType) {\n"
                + "       return DescribeUtil.getObjectDescribe(sObjectType).isDeletable();\n"
                + "    }\n"
                + "}\n",
        "public class DescribeUtil {\n"
                + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> describes = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                + "   private static Map<String, Schema.DescribeSObjectResult> objectDescribes = new Map<String, Schema.DescribeSObjectResult>();\n"
                + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                + "       fillMap(objType.getDescribe().getName());\n"
                + "       return describes.get(objType);\n"
                + "   }\n"
                + "   public static void fillMap(String objectName) {\n"
                + "       objectName = objectName.toLowerCase();\n"
                + "       if (!objectDescribes.containsKey(objectName)) {\n"
                + "           Schema.DescribeSObjectResult describe = Schema.getGlobalDescribe().get(objectName).getDescribe();\n"
                + "           describes.put(describe.getSObjectType(), describe);\n"
                + "       }\n"
                + "   }\n"
                + "}\n"
    };

    @Test
    public void testCachedPermCheckIsVerified() {
        assertNoViolation(rule, CACHED_PERM_CHECK_FOR_DELETION, "deleteObject", "MyClass");
    }

    /** Verify the Apex values are populaed correctly while using the forward paths */
    @Test
    public void testForwardPathsCreateExpectedApexValues() {
        TestUtil.buildGraph(g, CACHED_PERM_CHECK_FOR_DELETION);

        MethodVertex method =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.DEFINING_TYPE, "MyClass")
                                .has(Schema.NAME, "deleteObject"));

        // Find the path with the delete
        List<ApexPath> paths =
                ApexPathUtil.getForwardPaths(
                                g, method, ApexPathUtil.getFullConfiguredPathExpanderConfig())
                        .stream()
                        .filter(p -> p.lastVertex() instanceof DmlDeleteStatementVertex)
                        .collect(Collectors.toList());
        assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);
        verifyApexValuesInCachedPermLogic(path);
    }

    /** Verify the Apex values are populated correctly while using the reverse paths */
    @Test
    public void testReversePathsCreateExpectedApexValues() {
        TestUtil.buildGraph(g, CACHED_PERM_CHECK_FOR_DELETION);

        DmlDeleteStatementVertex dmlDeleteStatementVertex =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.DML_DELETE_STATEMENT)
                                .has(Schema.DEFINING_TYPE, "MyClass"));
        // Find the path with the delete
        List<ApexPath> paths =
                ApexPathUtil.getReversePaths(
                                g,
                                dmlDeleteStatementVertex,
                                ApexPathUtil.getFullConfiguredPathExpanderConfig())
                        .stream()
                        .filter(p -> p.lastVertex() instanceof DmlDeleteStatementVertex)
                        .collect(Collectors.toList());
        assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        verifyApexValuesInCachedPermLogic(path);
    }

    private void verifyApexValuesInCachedPermLogic(ApexPath path) {
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        SystemDebugAccumulator visitor = new SystemDebugAccumulator();
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        ApexBooleanValue apexBooleanValue = visitor.getSingletonResult();
        assertThat(apexBooleanValue.isDeterminant(), equalTo(false));
        assertThat(apexBooleanValue.getValue().isPresent(), equalTo(false));
        MethodCallExpressionVertex methodCallExpression =
                (MethodCallExpressionVertex) apexBooleanValue.getInvocable().get();
        assertThat(methodCallExpression.getMethodName(), equalTo("isDeletable"));
        DescribeSObjectResult describeSObjectResult =
                (DescribeSObjectResult) apexBooleanValue.getReturnedFrom().get();
        // NOTE: This is checking for #toLowerCase because the Schema.getGlobalDescribe().get uses
        // lowercase
        assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType()),
                equalToIgnoringCase("MyObject__c".toLowerCase()));
    }

    @Test
    public void testDeleteOnSoqlQuery() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void deleteObject(Id myId) {\n"
                    + "       if (!Schema.getGlobalDescribe().get('MyObject__c').getDescribe().fields.getMap().get('Id').getDescribe().isAccessible()) {\n"
                    + "           throw new AuraHandledException('No access');\n"
                    + "       }\n"
                    + "       if (!Schema.getGlobalDescribe().get('MyObject__c').getDescribe().isDeletable()) {\n"
                    + "           throw new AuraHandledException('No access');\n"
                    + "       }\n"
                    + "       delete [select Id from MyObject__c];\n"
                    + "   }\n"
                    + "}\n"
        };

        assertNoViolation(rule, sourceCode, "deleteObject", "MyClass");
    }

    /** This was failing to determine that "'ToDelete: ' + accounts" is a string. */
    @Test
    public void testListToStringImpliedConversion() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void deleteObject(Id myId) {\n"
                        + "       List<Account> accounts = new List<Account>();\n"
                        + "       logMessage('ToDelete: ' + accounts);\n"
                        + "       delete accounts;\n"
                        + "   }\n"
                        + "   public static void logMessage(String s) {\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                new String[] {sourceCode},
                "deleteObject",
                "MyClass",
                TestUtil.FIRST_FILE,
                2,
                expect(5, FlsConstants.FlsValidationType.DELETE, "Account"));
    }

    /** This was failing to determine that boolean1 || boolean2 is an ApexBooleanValue */
    @Test
    public void testResolveOROperationAsBoolean() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static Boolean boolean1 = True;\n"
                    + "   public static Boolean boolean2 = True;\n"
                    + "   public static void deleteObject(Id myId) {\n"
                    + "       if (shouldDelete()) {\n"
                    + "           Account account = new Account(Id = myId);\n"
                    + "           delete account;\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public static boolean shouldDelete() {\n"
                    + "       return boolean1 || boolean2;\n"
                    + "   }\n"
                    + "}\n"
        };

        assertViolations(
                rule,
                sourceCode,
                "deleteObject",
                "MyClass",
                TestUtil.FIRST_FILE,
                4,
                expect(7, FlsConstants.FlsValidationType.DELETE, "Account"));
    }

    @Test
    public void testValueIsPopulatedWhileWalkingPaths() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething(Boolean b) {\n"
                    + "       getCustomSetting(b).CustomField__c = true;\n"
                    + "   }\n"
                    + "   public static MyCustomSetting__c getCustomSetting(Boolean b) {\n"
                    + "       if (b) {\n"
                    + "           return MyCustomSetting__c.getInstance();\n"
                    + "       } else {\n"
                    + "           return MyCustomSetting__c.getInstance();\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        // Ensure that we can set the value by walking the paths
        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        assertThat(results, hasSize(Matchers.equalTo(2)));
    }

    /**
     * Tests that instances with a single constructor have that constructor walked, even if the
     * constructor is not explicitly called. It is guaranteed that the constructor must have been
     * called.
     */
    @Test
    public void testConstructorIsAlwaysWalked() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private Map<Id, Account> toDelete;\n"
                    + "   public MyClass() {\n"
                    + "       toDelete = new Map<Id, Account>();\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(toDelete);\n"
                    + "       delete toDelete.values();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");

        List<ApexListValue> listValues = new ArrayList<>();
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        SystemDebugAccumulator visitor =
                new SystemDebugAccumulator() {
                    @Override
                    public void afterVisit(
                            DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
                        MethodCallExpressionVertex methodCallExpression = vertex.getChild(0);
                        listValues.add(
                                (ApexListValue)
                                        symbols.getReturnedValue(methodCallExpression)
                                                .orElse(null));
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        assertThat(path.getConstructorPath().isPresent(), equalTo(true));

        ApexMapValue apexMapValue = visitor.getSingletonResult();
        assertThat(apexMapValue.isDeterminant(), equalTo(true));
        assertThat(apexMapValue.getValues().entrySet(), hasSize(equalTo(0)));
        assertThat(apexMapValue.getKeyType(), equalTo("Id"));
        assertThat(apexMapValue.getValueType(), equalTo("Account"));
        assertThat(listValues, hasSize(equalTo(1)));
        ApexListValue apexListValue = listValues.get(0);
        assertThat(apexListValue.isDeterminant(), equalTo(true));
    }

    /**
     * Verifies that a constructor with multiple paths is correctly expanded. There are 7 paths in
     * total. type1() has 3 paths type2() has 3 paths the constructor has an implicit else when
     * neither if statement matches
     */
    @Test
    public void testConstructorPathIsInvokedInMethodCall() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private Map<Id, Account> toDelete;\n"
                    + "   private String type;\n"
                    + "   public MyClass() {\n"
                    + "       type = ApexPages.currentPage().getParameters().get('type');\n"
                    + "       if (type == 'type1') {\n"
                    + "           type1();\n"
                    + "       } else if (type == 'type2') {\n"
                    + "           type2();\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public void type1() {\n"
                    + "       if (type == 'type1') {\n"
                    + "           System.debug('type1');\n"
                    + "       } else if (type == 'type2') {\n"
                    + "           System.debug('type2');\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public void type2() {\n"
                    + "       if (type == 'type1') {\n"
                    + "           System.debug('type1');\n"
                    + "       } else if (type == 'type2') {\n"
                    + "           System.debug('type2');\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(toDelete);\n"
                    + "       delete toDelete.values();\n"
                    + "   }\n"
                    + "}\n"
        };

        List<ApexPath> paths = TestRunner.getPaths(g, sourceCode);
        assertThat(paths, hasSize(equalTo(7)));
    }

    @Test
    public void testInnerClass_VariableInitializationFromGetterAndObjToString() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    MyObject__c myObject {\n"
                    + "        get { return new MyObject__c(Foo__c = 'Bar'); }\n"
                    + "    }\n"
                    + "   private class InnerClass {\n"
                    + "       public void doSomething() {\n"
                    + "           MyObject__c obj;\n"
                    + "           obj = new MyClass().myObject;\n"
                    + "           System.debug(obj);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexValue = visitor.getSingletonResult();
        assertThat(apexValue.isDeterminant(), equalTo(true));
        assertThat(apexValue.getTypeVertex().get().getCanonicalType(), equalTo("MyObject__c"));
        assertThat(apexValue.getApexValueProperties().entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                apexValue.getApexValueProperties().entrySet().iterator().next();
        assertThat(TestUtil.apexValueToString(entry.getKey()), equalTo("Foo__c"));
        assertThat(TestUtil.apexValueToString(entry.getValue()), equalTo("Bar"));
    }

    @Test
    public void testInnerClassVariableFromGetter_SeparateInstantiationAndAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    MyObject__c myObject {\n"
                    + "        get { return new MyObject__c(Foo__c = 'Bar'); }\n"
                    + "    }\n"
                    + "   private class InnerClass {\n"
                    + "       public void doSomething() {\n"
                    + "           MyObject__c obj;\n"
                    + "           MyClass c = new MyClass();\n"
                    + "           obj = c.myObject;\n"
                    + "           System.debug(obj);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        assertThat(apexSingleValue.isDeterminant(), equalTo(true));
        assertThat(
                apexSingleValue.getTypeVertex().get().getCanonicalType(), equalTo("MyObject__c"));
        assertThat(apexSingleValue.getApexValueProperties().entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                apexSingleValue.getApexValueProperties().entrySet().iterator().next();
        assertThat(TestUtil.apexValueToString(entry.getKey()), equalTo("Foo__c"));
        assertThat(TestUtil.apexValueToString(entry.getValue()), equalTo("Bar"));
    }

    @Test
    public void testInnerClassVariableFromGetter_ObjectCreationInlineInsert() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public MyObject__c myObject {\n"
                    + "        get { return new MyObject__c(Foo__c = 'Bar'); }\n"
                    + "    }\n"
                    + "   private class InnerClass {\n"
                    + "       public void doSomething() {\n"
                    + "           insert new MyClass().myObject;\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        List<ApexSingleValue> apexSingleValues = new ArrayList<>();
        DefaultNoOpPathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public void afterVisit(
                            DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        VariableExpressionVertex.Single variableExpression = vertex.getOnlyChild();
                        apexSingleValues.add(
                                (ApexSingleValue)
                                        symbols.getReturnedValue(variableExpression).get());
                    }
                };
        TestRunner.get(g, sourceCode).withPathVertexVisitor(() -> visitor).walkPath();

        assertThat(apexSingleValues, hasSize(equalTo(1)));
        ApexSingleValue apexSingleValue = apexSingleValues.get(0);
        assertThat(apexSingleValue.isDeterminant(), equalTo(true));
        assertThat(
                apexSingleValue.getTypeVertex().get().getCanonicalType(), equalTo("MyObject__c"));
        assertThat(apexSingleValue.getApexValueProperties().entrySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                apexSingleValue.getApexValueProperties().entrySet().iterator().next();
        assertThat(TestUtil.apexValueToString(entry.getKey()), equalTo("Foo__c"));
        assertThat(TestUtil.apexValueToString(entry.getValue()), equalTo("Bar"));
    }

    @Test
    public void testInnerClassDmlInsert() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public MyObject__c myObject {\n"
                    + "        get { return new MyObject__c(Foo__c = 'Bar'); }\n"
                    + "   }\n"
                    + "   private class InnerClass {\n"
                    + "       public void doSomething(Boolean insertItem) {\n"
                    + "           if (insertItem) {\n"
                    + "               insert new MyClass().myObject;\n"
                    + "           }\n"
                    + "       }\n"
                    + "       public InnerClass() {\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        TestUtil.buildGraph(config);
        DmlInsertStatementVertex dmlInsertStatement =
                TestUtil.getVertexOnLine(g, DmlInsertStatementVertex.class, 8);

        // Find the path that includes the insert statement
        List<ApexPath> paths =
                ApexPathUtil.getReversePaths(
                        g, dmlInsertStatement, ApexPathUtil.getFullConfiguredPathExpanderConfig());
        assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);

        List<ApexBooleanValue> apexBooleanValues = new ArrayList<>();
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        DefaultNoOpPathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public void afterVisit(
                            StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
                        VariableExpressionVertex.Single variableExpression = vertex.getOnlyChild();
                        apexBooleanValues.add(
                                (ApexBooleanValue)
                                        ScopeUtil.resolveToApexValue(symbols, variableExpression)
                                                .get());
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        assertThat(apexBooleanValues, hasSize(equalTo(1)));
        ApexBooleanValue apexBooleanValue = apexBooleanValues.get(0);
        assertThat(apexBooleanValue.isIndeterminant(), equalTo(true));
        assertThat(apexBooleanValue.isDeterminant(), equalTo(false));
    }

    @Test
    public void testMultipleClausesInIFCondition() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "        MyObject__c myObject = new MyObject__c(Item_Id__c = 10, User__c = 20);\n"
                    + "        if (Schema.SObjectType.MyObject__c.fields.Item_Id__c.isCreateable() &&\n"
                    + "            Schema.SObjectType.MyObject__c.fields.User__c.isCreateable()) {\n"
                    + "            insert myObject;\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
        };

        assertNoViolation(rule, sourceCode, "doSomething", "MyClass");
    }

    @Test
    public void testDescribeFieldResultOperation() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "        String objectName = 'Account';\n"
                    + "        String fieldName = 'Phone';\n"
                    + "        Map<String, Schema.DescribeSObjectResult> objectDescribes = new Map<String, Schema.DescribeSObjectResult>();\n"
                    + "        Map<String, Map<String, Schema.SObjectField>> fieldTokens = new Map<String,Map<String, Schema.SObjectField>>();\n"
                    + "        Map<String, Map<String, Schema.DescribeFieldResult>> fieldDescribes = new Map<String,Map<String, Schema.DescribeFieldResult>>();\n"
                    + "        fieldDescribes.put(objectName, new Map<String, Schema.DescribeFieldResult>());\n"
                    + "        objectDescribes.put(objectName, Schema.getGlobalDescribe().get(objectName).getDescribe());\n"
                    + "        fieldTokens.put(objectName, objectDescribes.get(objectName).fields.getMap());\n"
                    + "        Schema.DescribeFieldResult dfr = fieldTokens.get(objectName).get(fieldName).getDescribe();\n"
                    + "        System.debug(dfr);\n"
                    + "        fieldDescribes.get(objectName).put(fieldName, dfr);\n"
                    + "        String s = dfr.getType().name();\n"
                    + "        System.debug(s);\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        DescribeFieldResult describeFieldResult =
                (DescribeFieldResult) visitor.getAllResults().get(0).get();
        assertThat(describeFieldResult, not(nullValue()));

        // ApexDisplayType#name returns an indeterminant value
        ApexStringValue apexStringValue = (ApexStringValue) visitor.getAllResults().get(1).get();
        assertThat(apexStringValue.isIndeterminant(), equalTo(true));
        assertThat(apexStringValue.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testInsertCustomSettingWithFieldEdit() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static Custom_Settings__c getCustomSettingsInstance () {\n"
                    + "      Custom_Settings__c settings = Custom_Settings__c.getInstance(UserInfo.getUserId());\n"
                    + "      return settings;\n"
                    + "   }\n"
                    + "   public static void upsertCustomSetting (Boolean hasViewedAlert) {\n"
                    + "      Custom_Settings__c settings = getCustomSettingsInstance();\n"
                    + "      if (!settings.Field__c) {\n"
                    + "         settings.Field__c = hasViewedAlert;\n"
                    + "         insert settings;\n"
                    + "      }\n"
                    + "   }\n"
                    + "}\n"
        };

        // By default, custom settings require no CRUD/FLS. So no violations are expected here.
        assertNoViolation(rule, sourceCode, "upsertCustomSetting", "MyClass");
    }

    @Test
    public void testSObjectTypeNameFromStaticInitialization() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static SObjectType sObjType = MyObject__c.SObjectType;\n"
                    + "   static MyOtherClass myOtherClass = new MyOtherClass(sObjType);\n"
                    + "   public static void doSomething() {\n"
                    + "      myOtherClass.doSomethingElse('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   SObjectType sObjType;\n"
                    + "   public MyOtherClass(SObjectType sObjType) {\n"
                    + "       this.sObjType = sObjType;\n"
                    + "   }\n"
                    + "   public void doSomethingElse(String a) {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(sObjType);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(2)));

        ApexValue<?> apexValue = visitor.getResult(0);
        assertThat(TestUtil.apexValueToString(apexValue), equalTo("Hello"));

        SObjectType sObjectType = visitor.getResult(1);
        assertThat(TestUtil.apexValueToString(sObjectType.getType()), equalTo("MyObject__c"));
    }

    @Test
    public void testIntValueCallOnDecimal() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "      Decimal value = MyOtherClass.getCustomSettings().MyValue__c;\n"
                    + "      System.debug(value.intValue());\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static MyCustomSettings__c getCustomSettings() {\n"
                    + "       return MyCustomSettings__c.getInstance();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexIntegerValue integerValue = visitor.getSingletonResult();
        assertThat(integerValue.isIndeterminant(), equalTo(true));
        assertThat(integerValue.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testIntegerToDecimalConversionFromStaticAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private static final Integer DEFAULT_VALUE = 25;\n"
                    + "   public static void doSomething() {\n"
                    + "      Decimal value = MyOtherClass.getCustomSettings().MyValue__c;\n"
                    + "      if (value == null) {\n"
                    + "           value = DEFAULT_VALUE;\n"
                    + "      }\n"
                    + "      System.debug(value.intValue());\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static MyCustomSettings__c getCustomSettings() {\n"
                    + "       return MyCustomSettings__c.getInstance();\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        assertThat(results, hasSize(equalTo(2)));

        List<ApexIntegerValue> integerValues = new ArrayList<>();
        List<ApexIntegerValue> indeterminantIntegerValues = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            ApexIntegerValue integerValue = result.getVisitor().getSingletonResult();
            if (integerValue.isIndeterminant()) {
                indeterminantIntegerValues.add(integerValue);
            } else {
                integerValues.add(integerValue);
            }
        }
        assertThat(integerValues, hasSize(equalTo(1)));
        assertThat(indeterminantIntegerValues, hasSize(equalTo(1)));

        assertThat(integerValues.get(0).getValue().get(), equalTo(Integer.valueOf(25)));
    }

    @Test
    public void testFieldParsingFromDeserializedObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String jobId) {\n"
                        + "       MyCustomSetting__c c = MyCustomSetting__c.getOrgDefaults();\n"
                        + "       Map<String, String> status = (Map<String, String>) JSON.deserialize(c.My_Status__c, Map<String, String>.class);\n"
                        + "       System.debug(status);\n"
                        + "       String jobStatus = status.get(jobId);\n"
                        + "       System.debug(jobStatus);\n"
                        + "       List<String> errors = jobStatus.split('\\n');\n"
                        + "       System.debug(errors);\n"
                        + "       String errorMessage = '';\n"
                        + "       for (String er : errors) {\n"
                        + "           if (er.contains('>')) {\n"
                        + "               er = er.split('>')[1];\n"
                        + "               errorMessage += er + '\\n';\n"
                        + "           }\n"
                        + "       }\n"
                        + "       System.debug(errorMessage);\n"
                        + "    }\n"
                        + "}";

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        assertThat(results, hasSize(equalTo(2)));

        List<ApexStringValue> indeterminantStringValues = new ArrayList<>();
        List<ApexStringValue> stringValues = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            assertThat(visitor.getAllResults(), hasSize(Matchers.equalTo(4)));

            ApexMapValue map = (ApexMapValue) visitor.getAllResults().get(0).get();
            assertThat(map.isIndeterminant(), equalTo(false));
            ApexStringValue jobStatus = (ApexStringValue) visitor.getAllResults().get(1).get();
            assertThat(jobStatus.isIndeterminant(), equalTo(true));
            ApexListValue errors = (ApexListValue) visitor.getAllResults().get(2).get();
            assertThat(errors.isIndeterminant(), equalTo(true));
            ApexStringValue errorMessage = (ApexStringValue) visitor.getAllResults().get(3).get();
            if (errorMessage.isIndeterminant()) {
                indeterminantStringValues.add(errorMessage);
            } else {
                stringValues.add(errorMessage);
            }
        }
        assertThat(indeterminantStringValues, hasSize(equalTo(1)));
        assertThat(stringValues, hasSize(equalTo(1)));
        assertThat(TestUtil.apexValueToString(stringValues.get(0)), equalTo(""));
    }

    /**
     * This covers the following code in {@link FlsValidationRepresentation
     * unravelApexValue(ApexValue, Function, Function)}
     *
     * <p>} else if (apexValue.getInvocable().orElse(null) instanceof MethodCallExpressionVertex) {
     * // Unresolvable method such as UserInfo.getUserId(). Use the method name
     * MethodCallExpressionVertex methodCallExpression = (MethodCallExpressionVertex)
     * apexValue.getInvocable().get(); apexStringOptional =
     * Optional.of(methodCallExpression.getFullMethodName());
     */
    @Test
    public void testInvokeUnresolvedMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String jobId) {\n"
                        + "       insert new MyObject__c(UserId__c = UserInfo.getUserId());\n"
                        + "    }\n"
                        + "}";

        assertViolations(
                rule,
                new String[] {sourceCode},
                "doSomething",
                "MyClass",
                TestUtil.FIRST_FILE,
                2,
                expect(3, FlsConstants.FlsValidationType.INSERT, "MyObject__c")
                        .withField("UserId__c"));
    }

    @Test
    public void testMultiplePathsToUpsert() {
        String sourceCode =
                "public class MyClass{\n"
                        + "    public void foo() {\n"
                        + "        doUpsert('First');\n"
                        + "        doUpsert('Second');\n"
                        + "    }\n"
                        + "    public void doUpsert(String input) {\n"
                        + "        My_Custom_Object__c cObject = new My_Custom_Object__c();\n"
                        + "        if (input.equals('First')) {\n"
                        + "			 cObject.field0 = true;\n"
                        + "            cObject.field1 = 'Hello';\n"
                        + "        } else {\n"
                        + "            cObject.field2 = 'Not hello';\n"
                        + "            cObject.field3 = false;\n"
                        + "        }\n"
                        + "        cObject.field4 = 'Common value';\n"
                        + "        upsert cObject;\n"
                        + "    }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(16, FlsConstants.FlsValidationType.INSERT, "My_Custom_Object__c")
                        .withFields(
                                new String[] {"field0", "field1", "field2", "field3", "field4"}),
                expect(16, FlsConstants.FlsValidationType.UPDATE, "My_Custom_Object__c")
                        .withFields(
                                new String[] {"field0", "field1", "field2", "field3", "field4"}));
    }

    @Test
    public void testMultiplePathsToDml_firstChecked() {
        String sourceCode =
                "public class MyClass{\n"
                        + "    public void foo() {\n"
                        + "		 Account a = new Account();\n"
                        + "        pathWithCheck(a);\n"
                        + "        pathWithoutCheck(a);\n"
                        + "    }\n"
                        + "	public void pathWithCheck(Account a) {\n"
                        + "		if (Schema.SObjectType.Account.Fields.Name.isCreateable()) {\n"
                        + "			a.Name = 'Acme Inc';\n"
                        + "			doInsert(a);\n"
                        + "		}\n"
                        + "	}\n"
                        + "	public void pathWithoutCheck(Account a) {\n"
                        + "		a.Phone = '415-555-1212';\n"
                        + "		doInsert(a);\n"
                        + "	}\n"
                        + "	public void doInsert(Account a) {\n"
                        + "		insert a;\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(18, FlsConstants.FlsValidationType.INSERT, "Account").withField("Phone"));
    }

    @Test
    public void testMultiplePathsToDml_secondChecked() {
        String sourceCode =
                "public class MyClass{\n"
                        + "    public void foo() {\n"
                        + "		 Account a = new Account();\n"
                        + "        pathWithoutCheck(a);\n"
                        + "        pathWithCheck(a);\n"
                        + "    }\n"
                        + "	public void pathWithCheck(Account a) {\n"
                        + "		if (Schema.SObjectType.Account.Fields.Name.isCreateable()) {\n"
                        + "			a.Name = 'Acme Inc';\n"
                        + "			doInsert(a);\n"
                        + "		}\n"
                        + "	}\n"
                        + "	public void pathWithoutCheck(Account a) {\n"
                        + "		a.Phone = '415-555-1212';\n"
                        + "		doInsert(a);\n"
                        + "	}\n"
                        + "	public void doInsert(Account a) {\n"
                        + "		insert a;\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(18, FlsConstants.FlsValidationType.INSERT, "Account").withField("Phone"));
    }

    @Test
    public void testDmlInForLoop_differentObjectTypes() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> sobjects = new List<SObject>();\n"
                        + "       Account a = new Account(Name = 'Acme Inc.');\n"
                        + "       sobjects.add(a);\n"
                        + "       Contact c = new Contact(FirstName = 'foo');\n"
                        + "       sobjects.add(c);\n"
                        + "		for (SObject item: sobjects) {\n"
                        + "			insert item;\n"
                        + "		}\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(9, FlsConstants.FlsValidationType.INSERT, "Account").withField("Name"),
                expect(9, FlsConstants.FlsValidationType.INSERT, "Contact").withField("FirstName"));
    }

    @Test
    public void testDmlInForLoop_sameObjectType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo() {\n"
                        + "       List<SObject> sobjects = new List<SObject>();\n"
                        + "       Account a1 = new Account(Name = 'Acme Inc.');\n"
                        + "       sobjects.add(a1);\n"
                        + "       Account a2 = new Account(Name = 'another');\n"
                        + "       sobjects.add(a2);\n"
                        + "		for (SObject item: sobjects) {\n"
                        + "			insert item;\n"
                        + "		}\n"
                        + "   }\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(9, FlsConstants.FlsValidationType.INSERT, "Account").withField("Name"));
    }

    @Test
    public void testNoReadViolationForDmlUpdate() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void foo() {\n"
                        + "		sObject s = new Account();\n"
                        + "		Database.update(s);\n"
                        + "	}\n"
                        + "}\n";

        // This is different from the common test case since the instance of ApexFlsViolationRule is
        // for both read and write
        assertViolations(
                rule, sourceCode, expect(4, FlsConstants.FlsValidationType.UPDATE, "Account"));
    }

    @Test
    public void testTwoPathsToSameOperationWithOnlyOneField() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void foo(boolean b) {\n"
                        + "		Account acct = new Account();\n"
                        + "		if (b) {\n"
                        + "			acct.name = 'Acme Inc.';\n"
                        + "		}\n"
                        + "		insert acct;\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(7, FlsConstants.FlsValidationType.INSERT, "Account").withField("name"));
    }

    @Test
    @Disabled // Multiple conditions in IF clause is not handled
    public void testMultipleConditions() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void foo() {\n"
                        + "		Account acct = new Account();\n"
                        + "		if (!Account.SObjectType.Field.Name.isCreateable() || !Account.SObjectType.Field.Name.isUpdateable()) {\n"
                        + "			throw new Exception();\n"
                        + "		}\n"
                        + "		acct.name = 'Acme Inc';\n"
                        + "		upsert acct;\n"
                        + "	}\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeListInitializedByAnotherMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo(Account origAcct) {\n"
                        + "       List<String> fieldsToCheck = getFields();\n"
                        + "		Account copyAcct = new Account();\n"
                        + "		for (String field : fieldsToCheck) {\n"
                        + "			copyAcct.put(field, origAcct.get(field));\n"
                        + "		}\n"
                        + "		copyAcct.my_field = 'Hello';\n"
                        + "		Database.insert(copyAcct);\n"
                        + "   }\n"
                        + "	List<String> getFields() {\n"
                        + "		return new List<String> {'Name', 'Phone'};\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(9, FlsConstants.FlsValidationType.INSERT, "Account")
                        .withFields(new String[] {"Name", "Phone", "my_field"}));
    }

    @Test
    public void testSoqlValueFromMethod() {
        String source =
                "public class MyClass {\n"
                        + "	public void foo() {\n"
                        + "		List<Account> accList = getAccounts();\n"
                        + "		update accList;\n"
                        + "	}\n"
                        + "	public List<Account> getAccounts() {\n"
                        + "		return [SELECT Id, Name FROM Account];\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                source,
                expect(4, FlsConstants.FlsValidationType.UPDATE, "Account").withField("Name"),
                expect(7, FlsConstants.FlsValidationType.READ, "Account").withField("Name"));
    }

    @Test
    public void testSafeListInitializedByAnotherMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo(Account origAcct) {\n"
                        + "       List<String> fieldsToCheck = getFields();\n"
                        + "		Account copyAcct = new Account();\n"
                        + "		Map<String,Schema.SObjectField> m = Schema.getGlobalDescribe().get('Account').getDescribe().fields.getMap();\n"
                        + "		for (String field : fieldsToCheck) {\n"
                        + "           DescribeFieldResult dfr = m.get(field).getDescribe();\n"
                        + "           if (!dfr.isCreateable()) {\n"
                        + "				throw new Exception();\n"
                        + "			}\n"
                        + "			copyAcct.put(field, origAcct.get(field));\n"
                        + "		}\n"
                        + "		Database.insert(copyAcct);\n"
                        + "   }\n"
                        + "	List<String> getFields() {\n"
                        + "		return new List<String> {'Name', 'Phone'};\n"
                        + "	}\n"
                        + "}\n";

        assertNoViolation(rule, sourceCode);
    }

    @Test
    public void testUnsafeListConstructorUsingAnotherMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void foo(Account origAcct) {\n"
                        + "       List<String> fieldsToCheck = new List<String>(getFields());\n"
                        + "		Account copyAcct = new Account();\n"
                        + "		for (String field : fieldsToCheck) {\n"
                        + "			copyAcct.put(field, origAcct.get(field));\n"
                        + "		}\n"
                        + "		Database.insert(copyAcct);\n"
                        + "   }\n"
                        + "	List<String> getFields() {\n"
                        + "		return new List<String> {'Name', 'Phone'};\n"
                        + "	}\n"
                        + "}\n";

        assertViolations(
                rule,
                sourceCode,
                expect(8, FlsConstants.FlsValidationType.INSERT, "Account")
                        .withFields(new String[] {"Name", "Phone"}));
    }

    @Test
    @Disabled // TODO: attempting to repro NOT_A_MATCH issue, but this isn't the same
    public void testNotAMatchIssue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething(String className) {\n"
                        + "        String jsonBatchJob;\n"
                        + "        BatchJob batchJob = getBatchJob(className);\n"
                        + "        System.debug(batchJob);\n"
                        + "    }\n"
                        + "	 public BatchJob getBatchJob(String className) {\n"
                        + "        AsyncApexJob batchJob = getAsyncApexJob(null);\n"
                        + "        return batchJob == null ? null : new BatchJob(batchJob);\n"
                        + "    }\n"
                        + "	public AsyncApexJob getAsyncApexJob(String className) {\n"
                        + "        List<AsyncApexJob> apexJobs = getAsyncApexJobs(className, 1);\n"
                        + "        return apexJobs.isEmpty() ? null : apexJobs[0];\n"
                        + "    }\n"
                        + "	public AsyncApexJob getAsyncApexJob(Id jobId) {\n"
                        + "		List<AsyncApexJob> apexJobs = [\n"
                        + "            SELECT\n"
                        + "                Status, ApexClass.Name,\n"
                        + "                CreatedDate, CompletedDate\n"
                        + "            FROM AsyncApexJob\n"
                        + "            WHERE Id = :jobId\n"
                        + "            LIMIT 1\n"
                        + "        ];\n"
                        + "        return apexJobs.isEmpty() ? null : apexJobs[0];"
                        + "	}\n"
                        + "	public List<AsyncApexJob> getAsyncApexJobs(String className, Integer jobCounts) {\n"
                        + "        return [\n"
                        + "            SELECT\n"
                        + "                Status, ApexClass.Name,\n"
                        + "                CreatedDate, CompletedDate\n"
                        + "            FROM AsyncApexJob\n"
                        + "            WHERE JobType = :JOB_TYPE_BATCH\n"
                        + "                AND ApexClass.Name = :className\n"
                        + "            LIMIT : jobCounts\n"
                        + "        ];\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
    }

    @Test
    public void testJsonDeserializeDml() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void foo(String payLoad) {\n"
                    + "		MyFormat myf = (MyFormat) JSON.deserializeStrict(payLoad, MyFormat.class);\n"
                    + "	My_Data__c myd = myf.myData;\n"
                    + "	insert myd;"
                    + "	}\n"
                    + "}\n",
            "public class MyFormat {\n" + "	public My_Data__c myData{get; set;}\n" + "}\n"
        };

        assertViolations(
                rule,
                sourceCode,
                expect(5, FlsConstants.FlsValidationType.INSERT, "My_Data__c")
                        .withField(SoqlParserUtil.UNKNOWN));
    }

    /**
     * Validate that an indeterminant value contains {@link SoqlParserUtil#UNKNOWN} in addition to
     * any other fields that were set on the object.
     */
    @Test
    public void testIndeterminantValueIncludesUnknownField() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void foo() {\n"
                    + "		update myObj;\n"
                    + "	}\n"
                    + "	private MyObject__c myObj;\n"
                    + "	MyClass(ApexPages.StandardController controller) {\n"
                    + "		myObj = (MyObject__c)controller.getRecord();\n"
                    + "		myObj.MyField3__c = 'aValue';\n"
                    + "	}\n"
                    + "}\n"
        };

        assertViolations(
                rule,
                sourceCode,
                expect(3, FlsConstants.FlsValidationType.UPDATE, "MyObject__c")
                        .withFields(new String[] {"MyField3__c", SoqlParserUtil.UNKNOWN})
                        .withSourceLine(14));
    }
}

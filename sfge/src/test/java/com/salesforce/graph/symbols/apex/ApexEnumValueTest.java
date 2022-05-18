package com.salesforce.graph.symbols.apex;

import static com.salesforce.matchers.OptionalMatcher.optEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.apex.ApexEnum;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// TODO: Some of these tests can be converted into parameterized tests
public class ApexEnumValueTest {
    private static final String SCHEMA_DISPLAY_TYPE = "Schema.DisplayType";

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testUninitializedValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       DisplayType dt;\n"
                        + "       System.debug(dt);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexEnumValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(true));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(false));
    }

    @Test
    public void testIndeterminantValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(DisplayType dt) {\n"
                        + "       System.debug(dt);\n"
                        + "       System.debug(dt.name());\n"
                        + "       System.debug(dt.ordinal());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexEnumValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(false));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isIndeterminant(), equalTo(true));

        ApexIntegerValue ordinal = visitor.getResult(2);
        MatcherAssert.assertThat(ordinal.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testInitializedValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       DisplayType dt = DisplayType.ANYTYPE;\n"
                        + "       System.debug(dt);\n"
                        + "       System.debug(dt.name());\n"
                        + "       System.debug(dt.ordinal());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexEnumValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        ApexEnum.Value enumValue = value.getValue().get();
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(1));
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo("ANYTYPE"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(name.getValue(), optEqualTo("ANYTYPE"));

        ApexIntegerValue ordinal = visitor.getResult(2);
        MatcherAssert.assertThat(ordinal.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(ordinal.getValue(), optEqualTo(1));
    }

    @Test
    public void testAssignedValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       DisplayType dt;\n"
                        + "       dt = DisplayType.ADDRESS;\n"
                        + "       System.debug(dt);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexEnumValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));

        ApexEnum.Value enumValue = value.getValue().get();
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(0));
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo("ADDRESS"));
    }

    @Test
    public void testValues() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<DisplayType> values = DisplayType.values();\n"
                        + "       System.debug(values);\n"
                        + "       System.debug(values.get(1));\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexListValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getValues(), hasSize(equalTo(28)));

        ApexEnumValue apexEnumValue = visitor.getResult(1);
        MatcherAssert.assertThat(apexEnumValue.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(apexEnumValue.isNull(), equalTo(false));
        MatcherAssert.assertThat(apexEnumValue.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));

        ApexEnum.Value enumValue = apexEnumValue.getValue().get();
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(1));
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo("ANYTYPE"));
    }

    @Test
    public void testValueOf() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       DisplayType dt = DisplayType.valueOf('anyType');\n"
                        + "       System.debug(dt);\n"
                        + "       System.debug(dt.name());\n"
                        + "       System.debug(dt.ordinal());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexEnumValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        ApexEnum.Value enumValue = value.getValue().get();
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(1));
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo("ANYTYPE"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(name.getValue(), optEqualTo("ANYTYPE"));

        ApexIntegerValue ordinal = visitor.getResult(2);
        MatcherAssert.assertThat(ordinal.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(ordinal.getValue(), optEqualTo(1));
    }

    @Test
    public void testValueOfIndeterminantArgument() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       DisplayType dt = DisplayType.valueOf(s);\n"
                        + "       System.debug(dt);\n"
                        + "       System.debug(dt.name());\n"
                        + "       System.debug(dt.ordinal());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexEnumValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        MatcherAssert.assertThat(value.getValue().isPresent(), equalTo(false));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isDeterminant(), equalTo(false));

        ApexIntegerValue ordinal = visitor.getResult(2);
        MatcherAssert.assertThat(ordinal.isDeterminant(), equalTo(false));
    }

    @Test
    public void testReturnedFromMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       DisplayType dt = getEnum('ANYTYPE');\n"
                        + "       System.debug(dt);\n"
                        + "       System.debug(dt.name());\n"
                        + "       System.debug(dt.ordinal());\n"
                        + "    }\n"
                        + "    public static DisplayType getEnum(String s) {\n"
                        + "    	return DisplayType.valueOf(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexEnumValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        ApexEnum.Value enumValue = value.getValue().get();
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(1));
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo("ANYTYPE"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(name.getValue(), optEqualTo("ANYTYPE"));

        ApexIntegerValue ordinal = visitor.getResult(2);
        MatcherAssert.assertThat(ordinal.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(ordinal.getValue(), optEqualTo(1));
    }

    @Test
    public void testEnumAsMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       logEnum(DisplayType.ANYTYPE);\n"
                        + "    }\n"
                        + "    public static void logEnum(DisplayType dt) {\n"
                        + "       System.debug(dt);\n"
                        + "       System.debug(dt.name());\n"
                        + "       System.debug(dt.ordinal());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexEnumValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(value.getCanonicalType(), equalTo(SCHEMA_DISPLAY_TYPE));
        ApexEnum.Value enumValue = value.getValue().get();
        MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(1));
        MatcherAssert.assertThat(enumValue.getValueName(), equalTo("ANYTYPE"));

        ApexStringValue name = visitor.getResult(1);
        MatcherAssert.assertThat(name.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(name.getValue(), optEqualTo("ANYTYPE"));

        ApexIntegerValue ordinal = visitor.getResult(2);
        MatcherAssert.assertThat(ordinal.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(ordinal.getValue(), optEqualTo(1));
    }
}

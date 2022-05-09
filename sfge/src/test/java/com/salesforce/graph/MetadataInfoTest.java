package com.salesforce.graph;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.apex.ApexEnum;
import com.salesforce.graph.ops.GraphUtil;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MetadataInfoTest {
    private static final Logger LOGGER = LogManager.getLogger(MetadataInfoTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testCustomSettingsGetInstance() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    static MyCustomSetting__c settings;\n"
                        + "    public static void foo() {\n"
                        + "       settings = MyCustomSetting__c.getInstance(10);\n"
                        + "    }\n"
                        + "}\n";

        // <MethodCallExpression BeginLine='4' DefiningType='MyClass' EndLine='4'
        // FullMethodName='MyCustomSetting__c.getInstance' Image='' MethodName='getInstance'
        // RealLoc='true'>
        //    <ReferenceExpression BeginLine='4' Context='' DefiningType='MyClass' EndLine='4'
        // Image='MyCustomSetting__c' Names='[MyCustomSetting__c]' RealLoc='true'
        // ReferenceType='METHOD' SafeNav='false' />
        // </MethodCallExpression>

        TestUtil.buildGraph(g, sourceCode);
        MatcherAssert.assertThat(
                MetadataInfoProvider.get().isCustomSetting("MyCustomSetting__c".toLowerCase()),
                equalTo(true));
        MatcherAssert.assertThat(
                MetadataInfoProvider.get().isCustomSetting("NonExistent__c".toLowerCase()),
                equalTo(false));
    }

    @Test
    public void testCustomSettingsGetOrgDefaults() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    static MyCustomSetting__c settings;\n"
                        + "    public static void foo() {\n"
                        + "       settings = MyCustomSetting__c.getOrgDefaults();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        MatcherAssert.assertThat(
                MetadataInfoProvider.get().isCustomSetting("MyCustomSetting__c".toLowerCase()),
                equalTo(true));
        MatcherAssert.assertThat(
                MetadataInfoProvider.get().isCustomSetting("NonExistent__c".toLowerCase()),
                equalTo(false));
    }

    @Test
    public void testCustomSettingsGetAll() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    static Map<String,MyCustomSetting__c> allSettings;\n"
                        + "    public static void foo() {\n"
                        + "       allSettings = MyCustomSetting__c.getAll();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);
        MatcherAssert.assertThat(
                MetadataInfoProvider.get().isCustomSetting("MyCustomSetting__c".toLowerCase()),
                equalTo(true));
        MatcherAssert.assertThat(
                MetadataInfoProvider.get().isCustomSetting("NonExistent__c".toLowerCase()),
                equalTo(false));
    }

    /**
     * This test attempts to test the singleton behavior of {@link MetadataInfoImpl}. This is hard
     * to test since using the actual singleton will conflict with other tests. Instead this test
     * verifies that the same MetadataInfo can be passed across thread boundaries and will maintain
     * its original information.
     */
    @Test
    public void testCustomSettingIssue(TestInfo testInfo) throws Exception {
        final MetadataInfo metadataInfo = MetadataInfoProvider.get();
        final GraphTraversalSource fullGraph = GraphUtil.getGraph();
        TestUtil.compileTestFiles(fullGraph, testInfo);

        Runnable runnable =
                () -> {
                    MatcherAssert.assertThat(
                            metadataInfo.isCustomSetting("MyCustomSetting__c"), equalTo(true));
                };

        // Execute the thread and wait for it to complete
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(runnable);
        future.get();
    }

    @Test
    public void testGetEnumStandardEnum() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);
        MetadataInfo metadataInfo = MetadataInfoProvider.get();
        MatcherAssert.assertThat(metadataInfo.getEnum("DisplayType").isPresent(), equalTo(true));
        MatcherAssert.assertThat(
                metadataInfo.getEnum("Schema.DisplayType").isPresent(), equalTo(true));
        MatcherAssert.assertThat(metadataInfo.getEnum("Unknown").isPresent(), equalTo(false));

        ApexEnum apexEnum = MetadataInfoProvider.get().getEnum("DisplayType").orElse(null);
        MatcherAssert.assertThat(apexEnum.getName(), equalTo("Schema.DisplayType"));
        MatcherAssert.assertThat(apexEnum.getValues(), hasSize(equalTo(28)));
        MatcherAssert.assertThat(apexEnum.isValid("ADDRESS"), equalTo(true));
        MatcherAssert.assertThat(apexEnum.isValid("BASE64"), equalTo(true));

        ApexEnum.Value value;
        value = apexEnum.getValue("ADDRESS");
        MatcherAssert.assertThat(value.getValueName(), equalTo("ADDRESS"));
        MatcherAssert.assertThat(value.getOrdinal(), equalTo(0));

        value = apexEnum.getValue("BASE64");
        MatcherAssert.assertThat(value.getValueName(), equalTo("BASE64"));
        MatcherAssert.assertThat(value.getOrdinal(), equalTo(2));
    }

    public static Stream<Arguments> testGetEnumUserEnum() {
        return Stream.of(Arguments.of("MyEnum"), Arguments.of("MYENUM"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: enumName=({0})")
    public void testGetEnumUserEnum(String enumName) {
        String sourceCode =
                "public enum MyEnum {\n" + "    VALUE0,\n" + "    VALUE1,\n" + "    VALUE2\n" + "}";

        TestUtil.buildGraph(g, sourceCode);

        // #getEnum is case-insensitive
        ApexEnum apexEnum = MetadataInfoProvider.get().getEnum(enumName).orElse(null);
        MatcherAssert.assertThat(apexEnum, not(nullValue()));
        MatcherAssert.assertThat(apexEnum.getName(), equalTo("MyEnum"));
        MatcherAssert.assertThat(apexEnum.getValues(), hasSize(equalTo(3)));

        for (int i = 0; i < 3; i++) {
            for (String value : Arrays.asList("VALUE", "value")) {
                final String valueName = value + i;
                MatcherAssert.assertThat(apexEnum.isValid(valueName), equalTo(true));
                ApexEnum.Value enumValue = apexEnum.getValue(valueName);
                MatcherAssert.assertThat(enumValue.getValueName(), equalTo("VALUE" + i));
                MatcherAssert.assertThat(enumValue.getOrdinal(), equalTo(i));
            }
        }
    }
}

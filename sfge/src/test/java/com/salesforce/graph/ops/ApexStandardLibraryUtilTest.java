package com.salesforce.graph.ops;

import static com.salesforce.graph.ops.ApexStandardLibraryUtil.convertArrayToList;
import static com.salesforce.graph.ops.ApexStandardLibraryUtil.getCanonicalName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.ArgumentsUtil;
import com.salesforce.TestUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class ApexStandardLibraryUtilTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @CsvSource(
            value = {
                "String[],List<String>",
                "\tString \t [\t],List<String>",
                "Integer[],List<Integer>",
                "\tInteger \t [\t],List<Integer>",
                "DescribeSObjectResult[],List<Schema.DescribeSObjectResult>",
                "\tDescribeSObjectResult \t [\t],List<Schema.DescribeSObjectResult>"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testConvertArrayToString(String value, String expected) {
        assertThat(convertArrayToList(value).get(), equalTo(expected));
    }

    public static Stream<Arguments> testGetCanonicalStandardEnum() {
        List<Arguments> arguments =
                Arrays.asList(
                        Arguments.of("DisplayType", "Schema.DisplayType"),
                        Arguments.of("Schema.DisplayType", "Schema.DisplayType"));

        return ArgumentsUtil.permuteArgumentsWithUpperCase(arguments, 0);
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: value=({0})-expected({1})")
    public void testGetCanonicalStandardEnum(String value, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);
        MatcherAssert.assertThat(getCanonicalName(value), equalTo(expected));
    }

    /** The original enum name should be returned */
    @Test
    public void testGetCanonicalEnumNameThatDoesNotExist() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);
        MatcherAssert.assertThat(getCanonicalName("UnknownType"), equalTo("UnknownType"));
    }
}

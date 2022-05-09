package com.salesforce.graph.symbols;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.ApexValueAccumulator;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApexStandardTypeTest {
    private static final Logger LOGGER = LogManager.getLogger(ApexStandardTypeTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testUserInfo() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       Id userId = UserInfo.getUserId();\n"
                        + "       System.debug(userId);\n"
                        + "       Id profileId = UserInfo.getProfileId();\n"
                        + "       System.debug(profileId);\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path = TestUtil.getSingleApexPath(config, "foo");
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(
                        Pair.of("System.debug", "userId"), Pair.of("System.debug", "profileId"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> values;
        ApexValue<?> apexValue;

        values = visitor.getSingleResultPerLineByName("userId");
        MatcherAssert.assertThat(values.entrySet(), hasSize(equalTo(1)));
        apexValue = values.get(4).orElse(null);
        MatcherAssert.assertThat(apexValue.isIndeterminant(), equalTo(true));

        values = visitor.getSingleResultPerLineByName("profileId");
        MatcherAssert.assertThat(values.entrySet(), hasSize(equalTo(1)));
        apexValue = values.get(6).orElse(null);
        MatcherAssert.assertThat(apexValue.isIndeterminant(), equalTo(true));
    }
}

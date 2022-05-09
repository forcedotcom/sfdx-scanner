package com.salesforce.graph.ops;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import com.salesforce.TestUtil;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApexClassUtilTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testGetField() {
        String sourceCode =
                "public class MySingleton {\n"
                        + "    private static MySingleton singleton;\n"
                        + "    public static MySingleton getInstance() {\n"
                        + "       if (singleton == null) {\n"
                        + "           singleton = new MySingleton();\n"
                        + "       }\n"
                        + "       return singleton;\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        FieldVertex fieldVertex =
                ApexClassUtil.getField(g, SyntheticTypedVertex.get("mysingLeton"), "Singleton")
                        .orElse(null);
        MatcherAssert.assertThat(fieldVertex, not(nullValue()));
        MatcherAssert.assertThat(fieldVertex.isStatic(), equalTo(true));
    }
}

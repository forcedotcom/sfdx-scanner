package com.salesforce.graph.build;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InheritanceEdgeBuilderTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testNoInheritance() {
        String[] sourceCodes = {"public class c1 {}", "public interface i1 {}"};

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that nothing has any inheritance edges.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
        verifyNoEdges(g, Schema.EXTENDED_BY);
        verifyNoEdges(g, Schema.EXTENSION_OF);
    }

    @Test
    public void testSimpleClassExtension() {
        String[] sourceCodes = {
            "public class c1 {}", "public class c2 extends c1{}",
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the extension looks right.
        verifyExtension(g, "c1", "c2");

        // Verify that there are no implementation edges.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
    }

    @Test
    public void testSimpleInterfaceExtension() {
        String[] sourceCodes = {
            "public interface i1 {}", "public interface i2 extends i1{}",
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the extension looks right.
        verifyExtension(g, "i1", "i2");

        // Verify that there are no implementation edges.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
    }

    @Test
    public void testSimpleImplementation() {
        String[] sourceCodes = {
            "public interface i1 {}", "public class c1 implements i1{}",
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the implementations look right.
        verifyImplementation(g, "i1", "c1");

        // Verify that there are no extension edges.
        verifyNoEdges(g, Schema.EXTENDED_BY);
        verifyNoEdges(g, Schema.EXTENSION_OF);
    }

    @Test
    public void testImplementationOfExternalInterface() {
        String[] sourceCodes = {"public class c1 implements ExternallyDeclaredInterface {}"};

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the right properties were set.
        verifyInterfaceDefiningTypeProperty(
                g, "ExternallyDeclaredInterface", Collections.singletonList("c1"));
        // Verify that no edges were created.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
        verifyNoEdges(g, Schema.EXTENDED_BY);
        verifyNoEdges(g, Schema.EXTENSION_OF);
    }

    @Test
    public void testExtensionOfInnerClass() {
        String[] sourceCodes = {
            // Extensions in this class don't use the outer class name.
            "public class OuterClass1 extends InnerClass1 {\n"
                    + "    public class InnerClass1 {}\n"
                    + "    public class InnerClass2 extends InnerClass1 {}\n"
                    + "}",
            // Extensions in this class use the outer class name.
            "public class OuterClass2 extends OuterClass2.InnerClass1 {\n"
                    + "    public class InnerClass1 {}\n"
                    + "    public class InnerClass2 extends InnerClass1 {}\n"
                    + "}",
            // Third class extends one of the inner classes in another class.
            "public class OuterClass3 extends OuterClass2.InnerClass1 {}"
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Make sure OuterClass1.InnerClass1 is extended by the right classes.
        verifyExtension(
                g,
                "OuterClass1.InnerClass1",
                Arrays.asList("OuterClass1", "OuterClass1.InnerClass2"));

        // Make sure OuterClass2.InnerClass1 is extended by the right classes.
        verifyExtension(
                g,
                "OuterClass2.InnerClass1",
                Arrays.asList("OuterClass2", "OuterClass2.InnerClass2", "OuterClass3"));

        // Verify that there are no implementation edges.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
    }

    @Test
    public void testImplementationOfInnerInterface() {
        String[] sourceCodes = {
            // Implementations in this class don't use the outer class name.
            "public class OuterClass1 implements InnerInterface1 {\n"
                    + "    public class InnerInterface1 {}\n"
                    + "    public class InnerClass1 implements InnerInterface1 {}\n"
                    + "}",
            // Implementations in this class use the outer class name.
            "public class OuterClass2 implements OuterClass2.InnerInterface1 {\n"
                    + "    public class InnerInterface1 {}\n"
                    + "    public class InnerClass1 implements InnerInterface1 {}\n"
                    + "}",
            // Third class extends one of the inner classes in another class.
            "public class OuterClass3 implements OuterClass2.InnerInterface1 {}"
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Make sure OuterClass1.InnerInterface1 is implemented by the right classes.
        verifyImplementation(
                g,
                "OuterClass1.InnerInterface1",
                Arrays.asList("OuterClass1", "OuterClass1.InnerClass1"));

        // Make sure OuterClass2.InnerInterface1 is implemented by the right classes.
        verifyImplementation(
                g,
                "OuterClass2.InnerInterface1",
                Arrays.asList("OuterClass2", "OuterClass2.InnerClass1", "OuterClass3"));

        // Verify that there are no extension edges.
        verifyNoEdges(g, Schema.EXTENDED_BY);
        verifyNoEdges(g, Schema.EXTENSION_OF);
    }

    @Test
    public void testExtensionOfOwnOuterClass() {
        String[] sourceCodes = {
            "public class OuterClass1 {\n"
                    + "    public class InnerClass1 extends OuterClass1 {}\n"
                    + "}"
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the right extension edges exist.
        verifyExtension(g, "OuterClass1", "OuterClass1.InnerClass1");

        // Verify that there are no implementation edges.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
    }

    @Test
    public void testClassNameOverlap() {
        String[] sourceCodes = {
            // This class extends its own inner class without using the outer class name.
            "public class OuterClass1 extends NameOverlappedClass {\n"
                    + "    public class NameOverlappedClass {}\n"
                    + "}",
            // This class has the same name as the inner class the other class extends.
            "public class NameOverlappedClass {}",
            // This class extends the outer class with the conflict name.
            "public class OuterClass2 extends NameOverlappedClass {}"
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the right extension edges exist.
        verifyExtension(g, "NameOverlappedClass", "OuterClass2");
        verifyExtension(g, "OuterClass1.NameOverlappedClass", "OuterClass1");

        // Verify that there are no implementation edges.
        verifyNoEdges(g, Schema.IMPLEMENTED_BY);
        verifyNoEdges(g, Schema.IMPLEMENTATION_OF);
    }

    @Test
    public void testInterfaceNameOverlap() {
        String[] sourceCodes = {
            // This class implements its own inner interface without using the outer class name.
            "public class OuterClass1 implements NameOverlappedInterface {\n"
                    + "    public class NameOverlappedInterface {}\n"
                    + "}",
            // This interface has the same name as the inner interface the other class extends.
            "public interface NameOverlappedInterface {}",
            // This class implements the outer class with the conflict name.
            "public class OuterClass2 implements NameOverlappedInterface {}"
        };

        // Build the graph.
        // TODO: Ideally, this would call the edge builder directly against an incomplete graph.
        TestUtil.buildGraph(g, sourceCodes);

        // Verify that the right implementation edges exist.
        verifyImplementation(g, "NameOverlappedInterface", "OuterClass2");
        verifyImplementation(g, "OuterClass1.NameOverlappedInterface", "OuterClass1");

        // Verify that there are no extension edges.
        verifyNoEdges(g, Schema.EXTENDED_BY);
        verifyNoEdges(g, Schema.EXTENSION_OF);
    }

    private void verifyNoEdges(GraphTraversalSource g, String edgeName) {
        List<Vertex> vertices = g.V().out(edgeName).toList();
        MatcherAssert.assertThat(vertices, hasSize(equalTo(0)));
    }

    private void verifyExtension(
            GraphTraversalSource g, String parentDefiningType, String childDefiningType) {
        verifyExtension(g, parentDefiningType, Collections.singletonList(childDefiningType));
    }

    private void verifyExtension(
            GraphTraversalSource g, String parentDefiningType, List<String> childDefiningTypes) {
        verifyEdges(
                g, Schema.EXTENDED_BY, Schema.EXTENSION_OF, parentDefiningType, childDefiningTypes);
    }

    private void verifyImplementation(
            GraphTraversalSource g, String parentDefiningType, String childDefiningType) {
        verifyImplementation(g, parentDefiningType, Collections.singletonList(childDefiningType));
    }

    private void verifyImplementation(
            GraphTraversalSource g, String parentDefiningType, List<String> childDefiningTypes) {
        verifyEdges(
                g,
                Schema.IMPLEMENTED_BY,
                Schema.IMPLEMENTATION_OF,
                parentDefiningType,
                childDefiningTypes);
        verifyInterfaceDefiningTypeProperty(g, parentDefiningType, childDefiningTypes);
    }

    private void verifyEdges(
            GraphTraversalSource g,
            String outgoingEdge,
            String incomingEdge,
            String parentDefiningType,
            List<String> childDefiningTypes) {
        List<Object> outgoingVertices =
                g.V().has(Schema.DEFINING_TYPE, parentDefiningType)
                        .out(outgoingEdge)
                        .values(Schema.DEFINING_TYPE)
                        .order(Scope.global)
                        .by(Order.asc)
                        .toList();
        List<Object> incomingVertices =
                g.V().has(Schema.DEFINING_TYPE, parentDefiningType)
                        .in(incomingEdge)
                        .values(Schema.DEFINING_TYPE)
                        .order(Scope.global)
                        .by(Order.asc)
                        .toList();

        // Both lists should have the same vertices in the same order.
        MatcherAssert.assertThat(outgoingVertices, hasSize(equalTo(childDefiningTypes.size())));
        MatcherAssert.assertThat(incomingVertices, hasSize(equalTo(childDefiningTypes.size())));
        for (int i = 0; i < childDefiningTypes.size(); i++) {
            MatcherAssert.assertThat(
                    outgoingVertices.get(i).toString(), equalTo(childDefiningTypes.get(i)));
            MatcherAssert.assertThat(
                    incomingVertices.get(i).toString(), equalTo(childDefiningTypes.get(i)));
        }
    }

    private void verifyInterfaceDefiningTypeProperty(
            GraphTraversalSource g, String parentDefiningType, List<String> childDefiningTypes) {
        // For each child type, get the property on that vertex.
        List<Object> propValues =
                g.V().where(
                                CaseSafePropertyUtil.H.hasWithin(
                                        NodeType.USER_CLASS,
                                        Schema.DEFINING_TYPE,
                                        childDefiningTypes))
                        .values(Schema.INTERFACE_DEFINING_TYPES)
                        .toList();
        for (Object propValue : propValues) {
            MatcherAssert.assertThat(
                (Object[])propValue,
                arrayContaining(parentDefiningType));
        }
    }
}

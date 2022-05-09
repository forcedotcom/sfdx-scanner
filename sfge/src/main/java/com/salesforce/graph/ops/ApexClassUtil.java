package com.salesforce.graph.ops;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Utility methods that provide information about ApexClasses contained in the graph. */
public final class ApexClassUtil {
    /**
     * Find the field named {@code fieldName} in the class identified by {@code typeable} Example
     * getField(g, "MyClass", "MyField"); public class MyClass { private String myField; }
     */
    public static Optional<FieldVertex> getField(
            GraphTraversalSource g, Typeable typeable, String fieldName) {
        Optional<FieldVertex> vertex = getField(g, typeable.getCanonicalType(), fieldName);

        if (!vertex.isPresent() && typeable instanceof NewObjectExpressionVertex) {
            NewObjectExpressionVertex newObjectExpression = (NewObjectExpressionVertex) typeable;
            String innerClassName = newObjectExpression.getResolvedInnerClassName().orElse(null);
            if (innerClassName != null) {
                vertex = getField(g, innerClassName, fieldName);
            }
        }

        return vertex;
    }

    /**
     * Get FieldVertex based on a field member name in a class
     *
     * @param g Graph to traverse
     * @param definingType Name of class that holds the field member
     * @param fieldName Symbolic name of the field member
     * @return Optional of FieldVertex if the value was found. Otherwise, Optional.empty().
     */
    public static Optional<FieldVertex> getField(
            GraphTraversalSource g, String definingType, String fieldName) {
        return Optional.ofNullable(
                SFVertexFactory.loadSingleOrNull(
                        g,
                        g.V().where(
                                        H.has(
                                                ASTConstants.NodeType.USER_CLASS,
                                                Schema.DEFINING_TYPE,
                                                definingType))
                                .out(Schema.CHILD)
                                .where(
                                        H.has(
                                                ASTConstants.NodeType.FIELD,
                                                Schema.NAME,
                                                fieldName))));
    }

    private ApexClassUtil() {}
}

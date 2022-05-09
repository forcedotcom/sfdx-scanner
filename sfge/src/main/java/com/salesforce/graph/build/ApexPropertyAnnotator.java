package com.salesforce.graph.build;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasLabel;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Apex properties can have user defined method bodies. These methods have special names that are
 * executed when the property is read or written too. This class finds all properties that have
 * method bodies defined and adds {@link Schema#HAS_GETTER_METHOD_BLOCK} and/or {@link
 * Schema#HAS_SETTER_METHOD_BLOCK} attributes to the corresponding field vertex. This information is
 * used by the Class scopes to specialize the read/write behavior for these properties.
 *
 * <p>See
 * https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_properties.htm
 */
public class ApexPropertyAnnotator {
    public static void apply(GraphTraversalSource g, Vertex userClassVertex) {
        // We only annotate user classes
        if (!userClassVertex.label().equals(ASTConstants.NodeType.USER_CLASS)) {
            return;
        }

        // Find all methods that are within a property AND contain a block statement. Add a new
        // property depending on
        // the arity of the method
        // Properties such as "String a { get; } will not have a block statement
        //  <UserClass
        //      <Property
        //          <Method
        //              <BlockStatement
        for (int arity = 0; arity < 2; arity++) {
            g.V(userClassVertex)
                    .repeat(out(Schema.CHILD))
                    .until(hasLabel(ASTConstants.NodeType.PROPERTY))
                    .where(
                            out(Schema.CHILD)
                                    .hasLabel(ASTConstants.NodeType.METHOD)
                                    .has(Schema.ARITY, arity)
                                    .where(
                                            out(Schema.CHILD)
                                                    .hasLabel(
                                                            ASTConstants.NodeType.BLOCK_STATEMENT)))
                    .out(Schema.CHILD)
                    .hasLabel(ASTConstants.NodeType.FIELD)
                    .property(
                            arity == 0
                                    ? Schema.HAS_GETTER_METHOD_BLOCK
                                    : Schema.HAS_SETTER_METHOD_BLOCK,
                            true)
                    .iterate();
        }
    }
}

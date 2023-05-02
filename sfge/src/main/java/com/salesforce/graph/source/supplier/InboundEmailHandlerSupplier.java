package com.salesforce.graph.source.supplier;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.ops.TraversalUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Supplier for the {@code handleIndboundEmail()} method on implementations of {@code
 * InboundEmailHandler}.
 */
public class InboundEmailHandlerSupplier extends AbstractSourceSupplier {
    @Override
    public List<MethodVertex> getVertices(GraphTraversalSource g, List<String> targetFiles) {
        return SFVertexFactory.loadVertices(
                g,
                // Get any target class that implements the email handler interface.
                TraversalUtil.traverseImplementationsOf(
                                g, targetFiles, Schema.INBOUND_EMAIL_HANDLER)
                        // Get every implementation of the handle email method.
                        .out(Schema.CHILD)
                        .where(
                                CaseSafePropertyUtil.H.has(
                                        ASTConstants.NodeType.METHOD,
                                        Schema.NAME,
                                        Schema.HANDLE_INBOUND_EMAIL))
                        // Filter the results by return type and arity to limit the possibility
                        // of
                        // getting unnecessary results.
                        .where(
                                CaseSafePropertyUtil.H.has(
                                        ASTConstants.NodeType.METHOD,
                                        Schema.RETURN_TYPE,
                                        Schema.INBOUND_EMAIL_RESULT))
                        .has(Schema.ARITY, 2));
    }

    @Override
    public boolean isPotentialSource(MethodVertex methodVertex) {
        // NOTE: This is a pretty cursory check, and may struggle with nested inheritance. This
        // isn't likely to happen, but if it does, we can make the check more robust.
        Optional<UserClassVertex> parentClass = methodVertex.getParentClass();
        return parentClass.isPresent()
                && parentClass.get().getInterfaceNames().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet())
                        // Does the parent class implement InboundEmailHandler?
                        .contains(Schema.INBOUND_EMAIL_HANDLER.toLowerCase())
                // Does the method return an InboundEmailResult?
                && methodVertex.getReturnType().equalsIgnoreCase(Schema.INBOUND_EMAIL_RESULT)
                // Is the method named handleInboundEmail?
                && methodVertex.getName().equalsIgnoreCase(Schema.HANDLE_INBOUND_EMAIL);
    }
}

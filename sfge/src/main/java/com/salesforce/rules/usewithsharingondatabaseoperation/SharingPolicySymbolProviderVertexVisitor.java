package com.salesforce.rules.usewithsharingondatabaseoperation;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.PathScopeVisitor;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.*;
import com.salesforce.rules.ops.boundary.SharingPolicyBoundary;
import com.salesforce.rules.ops.boundary.SharingPolicyBoundaryDetector;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class SharingPolicySymbolProviderVertexVisitor extends DefaultSymbolProviderVertexVisitor {

    private final SharingPolicyBoundaryDetector boundaryDetector;

    public SharingPolicySymbolProviderVertexVisitor(
            GraphTraversalSource g,
            SharingPolicyBoundaryDetector boundaryDetector,
            BaseSFVertex sourceVertex) {
        super(g);
        this.boundaryDetector = boundaryDetector;
        if (sourceVertex == null) {
            throw new ProgrammingException(
                    "SharingPolicySymbolProviderVertexVisitor cannot be instantiated with a null source vertex. "
                            + "A source is needed");
        }

        Optional<UserClassVertex> sourceParent = sourceVertex.getParentClass();
        if (!sourceParent.isPresent()) {
            throw new RuntimeException(
                    "SharingPolicySymbolProviderVertexVisitor cannot find parent class of source vertex "
                            + sourceVertex);
        }

        // first boundary records the sharing policy of the class containing the entry point
        // this is necessary if there are no method calls before the first database operation:
        // we'd need to reference this boundary.
        this.boundaryDetector.pushBoundary(new SharingPolicyBoundary(sourceParent.get(), g));
    }

    /**
     * Attempt to push an appropriate {@link SharingPolicyBoundary} to the {@link
     * SharingPolicyBoundaryDetector}
     */
    @Override
    public PathScopeVisitor beforeMethodCall(InvocableVertex invocable, MethodVertex method) {
        PathScopeVisitor ret = super.beforeMethodCall(invocable, method);
        if (!method.getParentClass().isPresent()) {
            throw new RuntimeException(
                    "SharingPolicySymbolProviderVertexVisitor could not find parent of vertex "
                            + method);
        }

        boundaryDetector.pushBoundary(new SharingPolicyBoundary(method.getParentClass().get(), g));

        return ret;
    }

    /**
     * After visiting the children vertices, attempt to pop the appropriate {@link
     * SharingPolicyBoundary} (corresponding to the given method vertex) from the {@link
     * SharingPolicyBoundaryDetector}
     */
    @Override
    public Optional<ApexValue<?>> afterMethodCall(InvocableVertex invocable, MethodVertex method) {

        Optional<ApexValue<?>> ret = super.afterMethodCall(invocable, method);

        if (method.getParentClass().isPresent()) {
            // after exiting the method in the path, pop its sharing policy boundary from the stack
            // and verify that it is what we are expecting (matches the method).
            this.boundaryDetector.popBoundary(method.getParentClass().get(), true);
        }

        return ret;
    }
}

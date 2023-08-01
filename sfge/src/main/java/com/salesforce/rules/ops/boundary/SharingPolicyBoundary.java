package com.salesforce.rules.ops.boundary;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.*;
import java.util.List;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

/**
 * This class stores a {@link UserClassVertex} boundary item and can try to find the nearest
 * superclass with an explicitly defined sharing policy (the "applicable superclass"). See {@link
 * #getApplicableSuperclassPolicyVertex()}.
 */
public class SharingPolicyBoundary implements Boundary<UserClassVertex> {
    private final GraphTraversalSource g;
    private final UserClassVertex boundaryItem;
    /**
     * stores the applicable superclass, containing a sharing policy (or omitted sharing policy)
     * that applies to the boundary item. See {@link #getApplicableSuperclassPolicyVertex()}.
     */
    private UserClassVertex applicableSuperclassVertex;

    /**
     * store the {@link UserClassVertex} associated with this boundary.
     *
     * @param boundaryItem the {@link UserClassVertex} that the boundary represents
     * @param g a {@link GraphTraversalSource} used to find the applicable superclass. See {@link
     *     #getApplicableSuperclassPolicyVertex()}.
     */
    public SharingPolicyBoundary(UserClassVertex boundaryItem, GraphTraversalSource g) {
        this.boundaryItem = boundaryItem;
        this.g = g;
        // save the work of recursively querying the graph until it's required
        this.applicableSuperclassVertex = null;
    }

    /**
     * @return the {@link UserClassVertex} boundary item
     */
    @Override
    public UserClassVertex getBoundaryItem() {
        return this.boundaryItem;
    }

    /**
     * Try to find the applicable superclass to the boundary item. The applicable superclass is
     * defined as follows:
     *
     * <ul>
     *   <li>If the boundary item has an explicitly defined sharing policy, the applicable
     *       superclass will be the boundary item.
     *   <li>If the boundary item has no explicitly defined sharing policy, the applicable
     *       superclass will be the nearest superclass with an explicitly defined sharing policy.
     *   <li>If the boundary item has no explicitly defined sharing policy and no superclass that
     *       have an explicitly defined sharing policy, the applicable superclass will be the
     *       supermost {@link UserClassVertex} superclass.
     *   <li>If the boundary item has no explicitly defined sharing policy and no superclasses, the
     *       applicable superclass will be the boundary item.
     *
     * @return a {@link UserClassVertex} that is the applicable superclass of the boundary item.
     */
    public UserClassVertex getApplicableSuperclassPolicyVertex() {
        // save the work of querying the graph until it's required
        if (this.applicableSuperclassVertex == null) {
            this.applicableSuperclassVertex =
                    findSuperclassWithExplicitSharingPolicy().orElse(boundaryItem);
        }
        return this.applicableSuperclassVertex;
    }

    /**
     * Attempt to find an applicable superclass of the boundary item that has an explicitly defined
     * sharing policy (with, without, or inherited sharing). See {@link
     * #getApplicableSuperclassPolicyVertex()}
     *
     * @return An optional of a {@link UserClassVertex} if an superclass with an explicitly defined
     *     sharing policy is found. Empty if there are no such superclasses (or no superclasses at
     *     all)
     */
    private Optional<UserClassVertex> findSuperclassWithExplicitSharingPolicy() {
        UserClassVertex child = this.boundaryItem;
        if (!child.getSharingPolicy().equals(ASTConstants.SharingPolicy.OMITTED_DECLARATION)) {
            // if there is an explicitly defined sharing policy, return the child.
            // inherited sharing falls under this category because it means we must look at the
            // calling classes to find the sharing policy, not the boundary item's superclasses.
            return Optional.of(child);
        } else {
            // query the graph to recursively find all the superclasses of child
            List<UserClassVertex> vertices =
                    SFVertexFactory.loadVertices(
                            g,
                            g.V(child.getId())
                                    .union(__.repeat(__.out(Schema.EXTENSION_OF)).emit()));

            // go through the list to find the first class with an explicitly defined sharing policy
            // at the top of the list is the youngest/closest class
            for (UserClassVertex u : vertices) {
                if (!u.getSharingPolicy().equals(ASTConstants.SharingPolicy.OMITTED_DECLARATION))
                    return Optional.of(u);
            }

            // found no explicit sharing policies, so return nothing
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "SharingPolicyBoundary{"
                + "userClass="
                + boundaryItem
                + ", applicableAncestralPolicyClass="
                + applicableSuperclassVertex
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SharingPolicyBoundary
                && this.boundaryItem.equals(((SharingPolicyBoundary) o).boundaryItem)
                && this.applicableSuperclassVertex.equals(
                        ((SharingPolicyBoundary) o).applicableSuperclassVertex);
    }
}

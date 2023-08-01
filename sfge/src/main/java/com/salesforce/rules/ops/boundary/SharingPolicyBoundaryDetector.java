package com.salesforce.rules.ops.boundary;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.rules.usewithsharingondatabaseoperation.SharingPolicySymbolProviderVertexVisitor;
import java.util.Stack;

/**
 * Manages boundaries for {@link SharingPolicySymbolProviderVertexVisitor} and {@link
 * SharingPolicySymbolProviderVertexVisitor}.
 */
public class SharingPolicyBoundaryDetector
        extends BoundaryDetector<SharingPolicyBoundary, UserClassVertex> {

    /**
     * Get the {@link SharingPolicyBoundary} at the top of the stack. Compare to {@link
     * #getEffectiveSharingBoundary()}. Note: the stack must not be empty.
     */
    public SharingPolicyBoundary getCurrentSharingBoundary() {
        if (super.boundaries.isEmpty()) {
            throw new ProgrammingException(
                    "SharingPolicyBoundaryDetector tried to find "
                            + "the current sharing boundary but the boundary stack is empty.");
        }
        return super.boundaries.peek();
    }

    /**
     * Find the currently effective sharing boundary, possibly deeper in the stack if the first
     * boundary has "inherited sharing" or no explicitly defined sharing policy. Note: the stack
     * must not be empty.
     *
     * @return a {@link SharingPolicyBoundary} where {@link
     *     SharingPolicyBoundary#getApplicableSuperclassPolicyVertex()} returns a {@link
     *     UserClassVertex} with an explicitly defined sharing policy. If there are none, returns
     *     the {@link SharingPolicyBoundary} at the top of the stack.
     */
    public SharingPolicyBoundary getEffectiveSharingBoundary() {
        if (boundaries.isEmpty()) {
            throw new ProgrammingException(
                    "SharingPolicyBoundaryDetector tried to find "
                            + "the current sharing boundary but the boundary stack is empty.");
        }
        final Stack<SharingPolicyBoundary> copyStack = new Stack<>();
        copyStack.addAll(boundaries);
        return this.getEffectiveSharingPolicyBoundary(copyStack);
    }

    /** Get the currently effective sharing policy boundary, recursively */
    private SharingPolicyBoundary getEffectiveSharingPolicyBoundary(
            Stack<? extends SharingPolicyBoundary> boundaryStack) {

        /*
         * Note: the currently effective sharing policy can come from a few places
         *
         * Option 1: the current class has an explicitly defined sharing policy that is
         * "with sharing" or "without sharing". Use that.
         *
         * Option 2: the current class has no explicitly defined sharing policy, but it
         * has an ancestor with a "with sharing" or "without sharing". Use the ancestor's
         * policy.
         *
         * Option 3: the current class has no explicitly defined sharing policy, but it
         * has an ancestor with an "inherited sharing". Look deeper in the stack to inherit
         * the most recently explicitly defined sharing policy.
         *
         * Option 4: the current class has no explicitly defined sharing policy, and neither
         * do its ancestors. Look deeper in the boundary stack to implicitly inherit the most
         * recently explicitly defined sharing policy.
         *
         */

        if (boundaryStack.isEmpty()) {
            throw new ProgrammingException(
                    "SharingPolicyBoundaryDetector cannot find current sharing policy on empty policy stack.");
        }
        SharingPolicyBoundary userClassBoundary = boundaryStack.pop();

        UserClassVertex applicableAncestor =
                userClassBoundary.getApplicableSuperclassPolicyVertex();

        switch (applicableAncestor.getSharingPolicy()) {
            case ASTConstants.SharingPolicy.WITHOUT_SHARING:
            case ASTConstants.SharingPolicy.WITH_SHARING:
                return userClassBoundary;
            case ASTConstants.SharingPolicy.INHERITED_SHARING:
            case ASTConstants.SharingPolicy.OMITTED_DECLARATION:
            default:
                // in cases of inherited sharing or an omitted sharing declaration, we only want
                // to return this user class if it is the entry point. Otherwise, walk through the
                // stack to find the first explicitly defined sharing policy. If there is none,
                // OK to return the entry point.
                if (boundaryStack.isEmpty()) {
                    return userClassBoundary;
                } else {
                    return getEffectiveSharingPolicyBoundary(boundaryStack);
                }
        }
    }
}

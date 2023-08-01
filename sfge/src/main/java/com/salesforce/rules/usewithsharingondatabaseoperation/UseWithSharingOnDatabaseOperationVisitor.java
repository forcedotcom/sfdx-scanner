package com.salesforce.rules.usewithsharingondatabaseoperation;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.config.SfgeConfigProvider;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.rules.Violation;
import com.salesforce.rules.ops.DatabaseOperationUtil;
import com.salesforce.rules.ops.boundary.SharingPolicyBoundary;
import com.salesforce.rules.ops.boundary.SharingPolicyBoundaryDetector;
import java.util.HashSet;
import java.util.Set;

public class UseWithSharingOnDatabaseOperationVisitor extends DefaultNoOpPathVertexVisitor {

    /** record if warnings are enabled or disabled */
    private final boolean IS_WARNING_VIOLATION_DISABLED =
            SfgeConfigProvider.get().isWarningViolationDisabled();

    /** keeps track of the {@link SharingPolicyBoundary}s */
    private final SharingPolicyBoundaryDetector sharingPolicyBoundaryDetector;

    /** represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /**
     * represents the database operation statement that is possibly inside an unsafe sharing policy
     */
    private final BaseSFVertex sinkVertex;

    /** collects violation information */
    private final HashSet<Violation.PathBasedRuleViolation> violations;

    /**
     * visits vertices along a {@link com.salesforce.graph.ApexPath} to determine if the sink
     * vertex, a database operation, is executed from a "with sharing" policy.
     *
     * @param boundaryDetector a {@link SharingPolicyBoundaryDetector#getEffectiveSharingBoundary()}
     *     that keeps track of the current sharing policy
     * @param sourceVertex a {@link BaseSFVertex} that is the entry point of the path containing the
     *     sink vertex
     * @param sinkVertex a vertex that is a database operation. See {@link
     *     DatabaseOperationUtil#isDatabaseOperation(BaseSFVertex)}
     */
    UseWithSharingOnDatabaseOperationVisitor(
            SharingPolicyBoundaryDetector boundaryDetector,
            BaseSFVertex sourceVertex,
            BaseSFVertex sinkVertex) {
        if (!(DatabaseOperationUtil.isDatabaseOperation(sinkVertex))) {
            throw new ProgrammingException(
                    "UseWithSharingOnDatabaseOperation Sink vertex must be a "
                            + "MethodCallExpressionVertex in the Database class, DmlStatementVertex, or "
                            + "SoqlExpressionVertex. Provided sink vertex="
                            + sinkVertex);
        }
        this.sharingPolicyBoundaryDetector = boundaryDetector;
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        // the handler's test() method prevents this from running on anything other than database
        // operations
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * For all the DmlStatementVertex implementations, we need these overloaded afterVisit methods
     * so that the method will resolve correctly for all child classes of {@link
     * DmlStatementVertex}, and not to the parent class' generic {@link
     * com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor#afterVisit(BaseSFVertex,
     * SymbolProvider)}
     */
    @Override
    public void afterVisit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * For a more in-depth explanation, see {@link #afterVisit(DmlInsertStatementVertex,
     * SymbolProvider)}
     */
    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * For a more in-depth explanation, see {@link #afterVisit(DmlInsertStatementVertex,
     * SymbolProvider)}
     */
    @Override
    public void afterVisit(DmlMergeStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * For a more in-depth explanation, see {@link #afterVisit(DmlInsertStatementVertex,
     * SymbolProvider)}
     */
    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * For a more in-depth explanation, see {@link #afterVisit(DmlInsertStatementVertex,
     * SymbolProvider)}
     */
    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * For a more in-depth explanation, see {@link #afterVisit(DmlInsertStatementVertex,
     * SymbolProvider)}
     */
    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    @Override
    public void afterVisit(SoqlExpressionVertex vertex, SymbolProvider symbols) {
        createViolationIfDatabaseOpUnsafe(vertex, symbols);
    }

    /**
     * @return the set of Violations compiled from all the post-visits
     */
    Set<Violation.PathBasedRuleViolation> getViolations() {
        return violations;
    }

    private void createViolationIfDatabaseOpUnsafe(SFVertex vertex, SymbolProvider symbols) {
        // afterVisit on all Database Operation vertices calls this method, but we only want to
        // log a violation if this is the sink vertex and it's unsafe
        if (vertex != null && vertex.equals(sinkVertex)) {
            final SharingPolicyBoundary currentBoundary =
                    sharingPolicyBoundaryDetector.getCurrentSharingBoundary();
            final SharingPolicyBoundary effectiveBoundary =
                    sharingPolicyBoundaryDetector.getEffectiveSharingBoundary();

            String policy =
                    effectiveBoundary.getApplicableSuperclassPolicyVertex().getSharingPolicy();

            switch (policy) {
                case ASTConstants.SharingPolicy.WITH_SHARING:
                case ASTConstants.SharingPolicy.INHERITED_SHARING:
                    // we know that the operation runs as "with sharing" so this is not a violation,
                    // however we need to check if the policy is implicitly inherited by the current
                    // class and throw a warning if so.
                    createWarningIfApplicable(currentBoundary, effectiveBoundary);
                    // no need to check if a warning was created. The effective policy is safe, so
                    // we just need to flag a warning if applicable.
                    return;

                case ASTConstants.SharingPolicy.OMITTED_DECLARATION:
                    // if there is a warning created, we know that this ends up running as "with
                    // sharing", and so we should not create a violation. Otherwise, do.
                    if (createWarningIfApplicable(currentBoundary, effectiveBoundary)) {
                        return;
                    }

                case ASTConstants.SharingPolicy.WITHOUT_SHARING:
                default:
                    // create a violation, this operation is unsafe.
                    violations.add(
                            new Violation.PathBasedRuleViolation(
                                    UserFacingMessages.SharingPolicyRuleTemplates.MESSAGE_TEMPLATE,
                                    sourceVertex,
                                    sinkVertex));
                    return;
            }
        }
    }

    /**
     * Given a current and effective {@link SharingPolicyBoundary}, check to see if an implicit
     * inheritance warning is necessary. If yes, create it only if the configuration file is set to
     * display warnings. This could be either because:
     *
     * <ul>
     *   <li>The current class (containing the database operation) implicitly inherits from a
     *       superclass class (its "applicable superclass"); or
     *   <li>The calling class (aka "effective") implicitly inherits from a superclass class (its
     *       "applicable superclass").
     * </ul>
     *
     * Performed via {@link #warnIfCurrentClassInheritsFromParent(SharingPolicyBoundary,
     * SharingPolicyBoundary)} and {@link
     * #warnIfCallingClassInheritsFromParent(SharingPolicyBoundary, SharingPolicyBoundary)}
     *
     * @param currentBoundary the boundary of the class containing the database operation
     * @param effectiveBoundary the effective boundary (see {@link SharingPolicyBoundaryDetector}
     * @return true if the situation warrants a warning, false otherwise. NOTE: even if the
     *     configuration file is set to ignore warnings, this method will still return true if a
     *     warning is appropriate in the situation.
     */
    private boolean createWarningIfApplicable(
            SharingPolicyBoundary currentBoundary, SharingPolicyBoundary effectiveBoundary) {

        // two ways that a sharing policy could be implicitly inherited

        // 1: inherited by superclass
        if (createParentWarningIfApplicable(currentBoundary, effectiveBoundary)) {
            return true;
        }

        // 2: by caller
        if (createCallingWarningIfApplicable(currentBoundary, effectiveBoundary)) {
            return true;
        }

        return false;
    }

    /**
     * Given a current and effective {@link SharingPolicyBoundary}, check to see if an implicit
     * inheritance warning necessary because either the current class or the calling ("effective")
     * inherits from its applicable superclass. If yes, create it only if the configuration file is
     * set to display warnings.
     *
     * <p>For the definition of applicable superclass, see {@link SharingPolicyBoundary}.
     *
     * @param currentBoundary the boundary of the class containing the database operation
     * @param effectiveBoundary the effective boundary (see {@link
     *     SharingPolicyBoundaryDetector#getEffectiveSharingBoundary()})
     * @return true if the situation warrants a warning, false otherwise. NOTE: even if the
     *     configuration file is set to ignore warnings, this method will still return true if a
     *     warning is appropriate in the situation.
     */
    private boolean createParentWarningIfApplicable(
            SharingPolicyBoundary currentBoundary, SharingPolicyBoundary effectiveBoundary) {

        if (warnIfCurrentClassInheritsFromParent(currentBoundary, effectiveBoundary)) {
            return true;
        }

        if (warnIfCallingClassInheritsFromParent(currentBoundary, effectiveBoundary)) {
            return true;
        }

        return false;
    }

    /**
     * Given a current and effective {@link SharingPolicyBoundary}, check to see if an implicit
     * inheritance warning necessary because the calling ("effective") class inherits from its
     * applicable superclass. If yes, create it only if the configuration file is set to display
     * warnings.
     *
     * <p>For the definition of applicable superclass, see {@link SharingPolicyBoundary}.
     *
     * @param currentBoundary the boundary of the class containing the database operation
     * @param effectiveBoundary the effective boundary (see {@link
     *     SharingPolicyBoundaryDetector#getEffectiveSharingBoundary()}
     * @return true if the situation warrants a warning, false otherwise. NOTE: even if the
     *     configuration file is set to ignore warnings, this method will still return true if a
     *     warning is appropriate in the situation.
     */
    private boolean warnIfCallingClassInheritsFromParent(
            SharingPolicyBoundary currentBoundary, SharingPolicyBoundary effectiveBoundary) {

        // spotless:off
        // for a warning where a calling class inherits from its parent...
        if (
            // the class containing the DB operation must have "inherited sharing",
            SharingPolicyUtil.hasInheritedSharing(currentBoundary.getBoundaryItem())
                // the calling class must have no sharing policy,
                && SharingPolicyUtil.hasNoSharingPolicy(effectiveBoundary.getBoundaryItem())
                // and the superclass of the calling class must have with/inherited sharing
                // (otherwise, it's a violation).
                && SharingPolicyUtil.hasWithOrInheritedSharing(effectiveBoundary.getApplicableSuperclassPolicyVertex())) {
            // spotless:on

            // we can't shortcut and skip the warning logic if warnings are disabled, because it is
            // this logic that determines whether a violation or warning should be produced. If
            // performance becomes an issue, consider looking into this in the future.
            if (!IS_WARNING_VIOLATION_DISABLED) {
                violations.add(
                        new Violation.PathBasedRuleViolation(
                                String.format(
                                        UserFacingMessages.SharingPolicyRuleTemplates
                                                .WARNING_TEMPLATE,
                                        SharingPolicyUtil.InheritanceType.PARENT,
                                        effectiveBoundary
                                                .getApplicableSuperclassPolicyVertex()
                                                .getDefiningType()),
                                sourceVertex,
                                sinkVertex));
            }
            return true;
        }
        return false;
    }

    /**
     * Given a current and effective {@link SharingPolicyBoundary}, check to see if an implicit
     * inheritance warning necessary because the current class inherits from its applicable
     * superclass. If yes, create it only if the configuration file is set to display warnings.
     *
     * <p>For the definition of applicable superclass, see {@link SharingPolicyBoundary}.
     *
     * @param currentBoundary the boundary of the class containing the database operation
     * @param effectiveBoundary the effective boundary (see {@link SharingPolicyBoundaryDetector}
     * @return true if the situation warrants a warning, false otherwise. NOTE: even if the
     *     configuration file is set to ignore warnings, this method will still return true if a
     *     warning is appropriate in the situation.
     */
    private boolean warnIfCurrentClassInheritsFromParent(
            SharingPolicyBoundary currentBoundary, SharingPolicyBoundary effectiveBoundary) {

        // in order to inherit from a parent, the current class must not have an explicitly defined
        // policy
        if (!SharingPolicyUtil.hasNoSharingPolicy(currentBoundary.getBoundaryItem())) {
            return false;
        }

        boolean callingClassHasWithOrInheritedSharing =
                SharingPolicyUtil.hasWithOrInheritedSharing(effectiveBoundary.getBoundaryItem());

        boolean callingClassInheritsWithOrInheritedSharing =
                SharingPolicyUtil.hasNoSharingPolicy(effectiveBoundary.getBoundaryItem())
                        && SharingPolicyUtil.hasWithOrInheritedSharing(
                                effectiveBoundary.getApplicableSuperclassPolicyVertex());

        boolean currentClassHasInheritedSharing =
                SharingPolicyUtil.hasInheritedSharing(
                        currentBoundary.getApplicableSuperclassPolicyVertex());

        boolean currentClassInheritsWithSharing =
                SharingPolicyUtil.hasWithSharing(
                        currentBoundary.getApplicableSuperclassPolicyVertex());

        // spotless:off
        if ((
                // current class inherits with sharing from its superclass = warning
                currentClassInheritsWithSharing
        ) || (
                // current class inherits "inherited sharing" from calling class -->
                // if calling class has with or inherited sharing, it's safe, but requires a warning
                // otherwise, this would be a violation
                currentClassHasInheritedSharing
                && callingClassHasWithOrInheritedSharing
        ) || (
                // current class inherits "inherited sharing" from superclass -->
                // if calling class inherits "with" or "inherited" sharing, it's safe, but requires a warning
                // otherwise, this would be a violation
                currentClassHasInheritedSharing
                && callingClassInheritsWithOrInheritedSharing
        )) {
            // spotless:on

            // we can't shortcut and skip the warning logic if warnings are disabled, because it is
            // this logic that determines whether a violation or warning should be produced. If
            // performance becomes an issue, consider looking into this in the future.
            if (!IS_WARNING_VIOLATION_DISABLED) {
                violations.add(
                        new Violation.PathBasedRuleViolation(
                                String.format(
                                        UserFacingMessages.SharingPolicyRuleTemplates
                                                .WARNING_TEMPLATE,
                                        SharingPolicyUtil.InheritanceType.PARENT,
                                        currentBoundary
                                                .getApplicableSuperclassPolicyVertex()
                                                .getDefiningType()),
                                sourceVertex,
                                sinkVertex));
            }
            return true;
        }
        return false;
    }

    /**
     * Given a current and effective {@link SharingPolicyBoundary}, heck to see if a warning
     * indicating implicit inheritance from a calling class is applicable. If it is, create it only
     * if the configuration file is set to display warnings.
     *
     * @param currentBoundary the boundary of the class containing the database operation
     * @param effectiveBoundary the effective boundary (see {@link
     *     SharingPolicyBoundaryDetector#getEffectiveSharingBoundary()}
     * @return true if the situation warrants a warning, false otherwise. NOTE: even if the
     *     configuration file is set to ignore warnings, this method will still return true if a
     *     warning is appropriate in the situation.
     */
    private boolean createCallingWarningIfApplicable(
            SharingPolicyBoundary currentBoundary, SharingPolicyBoundary effectiveBoundary) {

        // in order to inherit from a parent, current class must have no policy
        if (!SharingPolicyUtil.hasNoSharingPolicy(currentBoundary.getBoundaryItem())) {
            return false;
        }

        // to inherit from a calling class, the applicable superclass must also not have a policy
        if (!SharingPolicyUtil.hasNoSharingPolicy(
                currentBoundary.getApplicableSuperclassPolicyVertex())) {
            return false;
        }

        // spotless:off
        if ((
                // either the calling class has with/inherited sharing...
                SharingPolicyUtil.hasWithOrInheritedSharing(effectiveBoundary.getBoundaryItem())
        ) || (
                // ... or it inherits with/inherited sharing from its superclass
                SharingPolicyUtil.hasNoSharingPolicy(effectiveBoundary.getBoundaryItem())
                && SharingPolicyUtil.hasWithOrInheritedSharing(effectiveBoundary.getApplicableSuperclassPolicyVertex())
        )) {
            // spotless:on

            // we can't shortcut and skip the warning logic if warnings are disabled, because it is
            // this logic that determines whether a violation or warning should be produced. If
            // performance becomes an issue, consider looking into this in the future.
            if (!IS_WARNING_VIOLATION_DISABLED) {
                UserClassVertex classInheritedFrom =
                        SharingPolicyUtil.hasNoSharingPolicy(effectiveBoundary.getBoundaryItem())
                                ? effectiveBoundary.getBoundaryItem()
                                : effectiveBoundary.getApplicableSuperclassPolicyVertex();

                violations.add(
                        new Violation.PathBasedRuleViolation(
                                String.format(
                                        UserFacingMessages.SharingPolicyRuleTemplates
                                                .WARNING_TEMPLATE,
                                        SharingPolicyUtil.InheritanceType.CALLING,
                                        classInheritedFrom.getDefiningType()),
                                sourceVertex,
                                sinkVertex));
            }
            return true;
        }

        return false;
    }
}

package com.salesforce.graph.symbols;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ArrayLoadExpressionVertex;
import com.salesforce.graph.vertex.AssignmentExpressionVertex;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.CatchBlockStatementVertex;
import com.salesforce.graph.vertex.DmlDeleteStatementVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.DmlUndeleteStatementVertex;
import com.salesforce.graph.vertex.DmlUpdateStatementVertex;
import com.salesforce.graph.vertex.DmlUpsertStatementVertex;
import com.salesforce.graph.vertex.ElseWhenBlockVertex;
import com.salesforce.graph.vertex.EmptyReferenceExpressionVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.FieldDeclarationStatementsVertex;
import com.salesforce.graph.vertex.FieldDeclarationVertex;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.ForEachStatementVertex;
import com.salesforce.graph.vertex.ForLoopStatementVertex;
import com.salesforce.graph.vertex.IdentifierCaseVertex;
import com.salesforce.graph.vertex.IfBlockStatementVertex;
import com.salesforce.graph.vertex.IfElseBlockStatementVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ModifierNodeVertex;
import com.salesforce.graph.vertex.NewKeyValueObjectExpressionVertex;
import com.salesforce.graph.vertex.NewListInitExpressionVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewMapInitExpressionVertex;
import com.salesforce.graph.vertex.NewMapLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.NewSetInitExpressionVertex;
import com.salesforce.graph.vertex.NewSetLiteralExpressionVertex;
import com.salesforce.graph.vertex.ParameterVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.ReferenceExpressionVertex;
import com.salesforce.graph.vertex.ReturnStatementVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.SuperMethodCallExpressionVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import com.salesforce.graph.vertex.TryCatchFinallyBlockStatementVertex;
import com.salesforce.graph.vertex.TypeWhenBlockVertex;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VariableDeclarationVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.vertex.WhileLoopStatementVertex;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

// TODO: This isn't used for anything except its static values
/**
 * A scope that does nothing. It returns true for all elements except those that are block
 * statements such as BlockStatementVertex, ForEachStatementVertex, ForLoopStatementVertex,
 * IfBlockStatementVertex, and IfElseBlockStatementVertex. See {@link #shouldVisitChildren}.
 */
public final class DefaultNoOpScopeVisitor extends AbstractDefaultNoOpScope
        implements ScopeVisitor {
    /**
     * Certain vertices are included in paths for marking points in time, however we typically don't
     * want to descend into the children of these vertices. There children will be visited when the
     * path is traversed, which children should be visited is highly path dependent.
     */
    public static final Set<String> PATH_WALKER_MARKER_LABELS =
            new HashSet<>(
                    // WARNING: IF YOU ADD A VALUE HERE YOU LIKELY WANT TO ADD A NEW VISIT METHOD TO
                    // PathScopeVisitor that
                    // returns false from the visit method.
                    // TODO: Make this more cohesive with PathScopeVisitor to avoid the strong
                    // coupling
                    Arrays.asList(
                            ASTConstants.NodeType.BLOCK_STATEMENT,
                            ASTConstants.NodeType.CATCH_BLOCK_STATEMENT,
                            ASTConstants.NodeType.FOR_EACH_STATEMENT,
                            ASTConstants.NodeType.FOR_LOOP_STATEMENT,
                            ASTConstants.NodeType.IF_BLOCK_STATEMENT,
                            ASTConstants.NodeType.IF_ELSE_BLOCK_STATEMENT,
                            ASTConstants.NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT,
                            ASTConstants.NodeType.WHILE_LOOP_STATEMENT));

    public static boolean shouldVisitChildren(BaseSFVertex vertex) {
        // These are marker blocks for scope, the children will be visited in the path
        // TODO: This is almost the same as vertex#startsInnerScope. The outlier is
        // IF_BLOCK_STATEMENT
        // see if IF_BLOCK_STATEMENT can start an inner scope.
        return !PATH_WALKER_MARKER_LABELS.contains(vertex.getLabel());
    }

    /**
     * Keep track of scopes that are pushed and popped, so that this class can honor the contract of
     * {@link ScopeVisitor#popMethodInvocationScope(InvocableVertex)}
     */
    private final Stack<MethodInvocationScope> methodInvocationScopes;

    public DefaultNoOpScopeVisitor() {
        this.methodInvocationScopes = new Stack<>();
    }

    @Override
    public MutableSymbolProvider getMutableSymbolProvider() {
        return this;
    }

    @Override
    public void pushMethodInvocationScope(MethodInvocationScope methodInvocationScope) {
        methodInvocationScopes.push(methodInvocationScope);
    }

    @Override
    public MethodInvocationScope popMethodInvocationScope(InvocableVertex invocable) {
        return methodInvocationScopes.pop();
    }

    @Override
    public Optional<PathScopeVisitor> getImplementingScope(
            InvocableVertex invocable, MethodVertex method) {
        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> afterMethodCall(
            InvocableVertex invocable, MethodVertex method, ApexValue<?> returnValue) {
        return Optional.empty();
    }

    @Override
    public boolean visit(ArrayLoadExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(AssignmentExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(BaseSFVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(BlockStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(CatchBlockStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlDeleteStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlInsertStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlUndeleteStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlUpdateStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlUpsertStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ElseWhenBlockVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(EmptyReferenceExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ExpressionStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationStatementsVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(FieldVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(IdentifierCaseVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(IfBlockStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(IfElseBlockStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(LiteralCaseVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(LiteralExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(MethodCallExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(MethodVertex.ConstructorVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(MethodVertex.InstanceMethodVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ModifierNodeVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(NewKeyValueObjectExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(NewListLiteralExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(NewObjectExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ParameterVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(PrefixExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ReferenceExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ReturnStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Negative vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Positive vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Unknown vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(SuperMethodCallExpressionVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(SwitchStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ThrowStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(TypeWhenBlockVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ValueWhenBlockVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableDeclarationVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableDeclarationStatementsVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableExpressionVertex.ForLoop vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableExpressionVertex.Single vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(WhileLoopStatementVertex vertex) {
        return shouldVisitChildren(vertex);
    }

    @Override
    public void afterVisit(ArrayLoadExpressionVertex vertex) {}

    @Override
    public void afterVisit(AssignmentExpressionVertex vertex) {}

    @Override
    public void afterVisit(BaseSFVertex vertex) {}

    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlInsertStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex) {}

    @Override
    public void afterVisit(FieldDeclarationVertex vertex) {}

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewListInitExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewListLiteralExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewMapInitExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewMapLiteralExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewObjectExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewSetInitExpressionVertex vertex) {}

    @Override
    public void afterVisit(NewSetLiteralExpressionVertex vertex) {}

    @Override
    public void afterVisit(ReturnStatementVertex vertex) {}

    @Override
    public void afterVisit(SoqlExpressionVertex vertex) {}

    @Override
    public void afterVisit(ThrowStatementVertex vertex) {}

    @Override
    public void afterVisit(VariableDeclarationVertex vertex) {}

    @Override
    public void afterVisit(VariableExpressionVertex.Single vertex) {}
}

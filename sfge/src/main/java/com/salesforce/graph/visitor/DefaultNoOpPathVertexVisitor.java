package com.salesforce.graph.visitor;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultNoOpScopeVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;

/**
 * A visitor that does nothing, useful for deriving visitors. Delegates to {@link
 * DefaultNoOpScopeVisitor#shouldVisitChildren(BaseSFVertex)} for responses.
 */
public class DefaultNoOpPathVertexVisitor implements PathVertexVisitor {
    @Override
    public void recursionDetected(
            ApexPath currentPath,
            MethodCallExpressionVertex methodCallExpressionVertex,
            ApexPath recursivePath) {}

    @Override
    public boolean visit(AssignmentExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(BaseSFVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(BlockStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(CatchBlockStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlMergeStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlUpdateStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(DmlUpsertStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ElseWhenBlockVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(EmptyReferenceExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ExpressionStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationStatementsVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(FieldVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(IfBlockStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(IfElseBlockStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(LiteralExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(MethodVertex.ConstructorVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(MethodVertex.InstanceMethodVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ModifierNodeVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(NewKeyValueObjectExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(NewListLiteralExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(NewObjectExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ParameterVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(PrefixExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ReferenceExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ReturnStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(SoqlExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Unknown vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(SuperMethodCallExpressionVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(SwitchStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ThrowStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatementVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(ValueWhenBlockVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableDeclarationVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableDeclarationStatementsVertex vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableExpressionVertex.ForLoop vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public boolean visit(VariableExpressionVertex.Single vertex, SymbolProvider symbols) {
        return DefaultNoOpScopeVisitor.shouldVisitChildren(vertex);
    }

    @Override
    public void afterVisit(BaseSFVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(DoLoopStatementVertex vertex, SymbolProvider symbols) {

    }

    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(DmlMergeStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(ForEachStatementVertex vertex, SymbolProvider symbols) {

    }

    @Override
    public void afterVisit(ForLoopStatementVertex vertex, SymbolProvider symbols) {

    }

    @Override
    public void afterVisit(FieldDeclarationVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(NewObjectExpressionVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(SoqlExpressionVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(ThrowStatementVertex vertex, SymbolProvider symbols) {}

    @Override
    public void afterVisit(WhileLoopStatementVertex vertex, SymbolProvider symbols) {

    }
}

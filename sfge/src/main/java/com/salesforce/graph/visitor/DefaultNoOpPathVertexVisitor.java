package com.salesforce.graph.visitor;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultNoOpScopeVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.AssignmentExpressionVertex;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.CatchBlockStatementVertex;
import com.salesforce.graph.vertex.DmlDeleteStatementVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.DmlMergeStatementVertex;
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
import com.salesforce.graph.vertex.IfBlockStatementVertex;
import com.salesforce.graph.vertex.IfElseBlockStatementVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ModifierNodeVertex;
import com.salesforce.graph.vertex.NewKeyValueObjectExpressionVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
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
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VariableDeclarationVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;

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
}

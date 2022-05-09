package com.salesforce.graph.visitor;

import com.salesforce.exception.UnexpectedException;
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

/** Useful when you want ensure certain methods aren't being called. */
public class DefaultThrowVertexVisitor implements VertexVisitor {
    protected boolean defaultVisit(BaseSFVertex vertex) {
        throw new UnexpectedException(
                "DefaultThrowVertexVisitor#defaultVisit. this=" + this + ", vertex=" + vertex);
    }

    protected void defaultAfterVisit(BaseSFVertex vertex) {
        throw new UnexpectedException(
                "DefaultThrowVertexVisitor#defaultAfterVisit. this=" + this + ", vertex=" + vertex);
    }

    @Override
    public boolean visit(ArrayLoadExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(AssignmentExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(BaseSFVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(BlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(CatchBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(DmlDeleteStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(DmlInsertStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(DmlUndeleteStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(DmlUpdateStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(DmlUpsertStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ElseWhenBlockVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(EmptyReferenceExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ExpressionStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationStatementsVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(FieldVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(IdentifierCaseVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(IfBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(IfElseBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(LiteralCaseVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(LiteralExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(MethodCallExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(MethodVertex.ConstructorVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(MethodVertex.InstanceMethodVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ModifierNodeVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(NewKeyValueObjectExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(NewListLiteralExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(NewObjectExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ParameterVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(PrefixExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ReferenceExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ReturnStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Negative vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Positive vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(StandardConditionVertex.Unknown vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(SwitchStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(SuperMethodCallExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ThrowStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(TypeWhenBlockVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(ValueWhenBlockVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(VariableDeclarationVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(VariableDeclarationStatementsVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(VariableExpressionVertex.ForLoop vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(VariableExpressionVertex.Single vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public boolean visit(WhileLoopStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    @Override
    public void afterVisit(ArrayLoadExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(AssignmentExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(BaseSFVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlInsertStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(FieldDeclarationVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewListInitExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewListLiteralExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewMapInitExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewMapLiteralExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewObjectExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewSetInitExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewSetLiteralExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(ReturnStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(SoqlExpressionVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(ThrowStatementVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(VariableDeclarationVertex vertex) {
        defaultAfterVisit(vertex);
    }

    @Override
    public void afterVisit(VariableExpressionVertex.Single vertex) {
        defaultAfterVisit(vertex);
    }
}

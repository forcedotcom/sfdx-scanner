package com.salesforce.graph.visitor;

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

public interface VertexVisitor {
    boolean visit(ArrayLoadExpressionVertex vertex);

    boolean visit(AssignmentExpressionVertex vertex);

    boolean visit(BaseSFVertex vertex);

    boolean visit(BlockStatementVertex vertex);

    boolean visit(CatchBlockStatementVertex vertex);

    boolean visit(DmlDeleteStatementVertex vertex);

    boolean visit(DmlInsertStatementVertex vertex);

    boolean visit(DmlUndeleteStatementVertex vertex);

    boolean visit(DmlUpdateStatementVertex vertex);

    boolean visit(DmlUpsertStatementVertex vertex);

    boolean visit(ElseWhenBlockVertex vertex);

    boolean visit(EmptyReferenceExpressionVertex vertex);

    boolean visit(ExpressionStatementVertex vertex);

    boolean visit(FieldDeclarationStatementsVertex vertex);

    boolean visit(FieldDeclarationVertex vertex);

    boolean visit(FieldVertex vertex);

    boolean visit(ForEachStatementVertex vertex);

    boolean visit(ForLoopStatementVertex vertex);

    boolean visit(IdentifierCaseVertex vertex);

    boolean visit(IfBlockStatementVertex vertex);

    boolean visit(IfElseBlockStatementVertex vertex);

    boolean visit(LiteralCaseVertex vertex);

    boolean visit(LiteralExpressionVertex vertex);

    boolean visit(MethodCallExpressionVertex vertex);

    boolean visit(MethodVertex.ConstructorVertex vertex);

    boolean visit(MethodVertex.InstanceMethodVertex vertex);

    boolean visit(ModifierNodeVertex vertex);

    boolean visit(NewKeyValueObjectExpressionVertex vertex);

    boolean visit(NewListLiteralExpressionVertex vertex);

    boolean visit(NewObjectExpressionVertex vertex);

    boolean visit(ParameterVertex vertex);

    boolean visit(PrefixExpressionVertex vertex);

    boolean visit(ReferenceExpressionVertex vertex);

    boolean visit(ReturnStatementVertex vertex);

    boolean visit(StandardConditionVertex.Negative vertex);

    boolean visit(StandardConditionVertex.Positive vertex);

    boolean visit(StandardConditionVertex.Unknown vertex);

    boolean visit(SuperMethodCallExpressionVertex vertex);

    boolean visit(SwitchStatementVertex vertex);

    boolean visit(ThrowStatementVertex vertex);

    boolean visit(TryCatchFinallyBlockStatementVertex vertex);

    boolean visit(TypeWhenBlockVertex vertex);

    boolean visit(ValueWhenBlockVertex vertex);

    boolean visit(VariableDeclarationVertex vertex);

    boolean visit(VariableDeclarationStatementsVertex vertex);

    boolean visit(VariableExpressionVertex.ForLoop vertex);

    boolean visit(VariableExpressionVertex.Single vertex);

    boolean visit(WhileLoopStatementVertex vertex);

    void afterVisit(ArrayLoadExpressionVertex vertex);

    void afterVisit(AssignmentExpressionVertex vertex);

    void afterVisit(BaseSFVertex vertex);

    void afterVisit(DmlDeleteStatementVertex vertex);

    void afterVisit(DmlInsertStatementVertex vertex);

    void afterVisit(DmlUndeleteStatementVertex vertex);

    void afterVisit(DmlUpdateStatementVertex vertex);

    void afterVisit(DmlUpsertStatementVertex vertex);

    void afterVisit(FieldDeclarationVertex vertex);

    void afterVisit(MethodCallExpressionVertex vertex);

    void afterVisit(NewListInitExpressionVertex vertex);

    void afterVisit(NewListLiteralExpressionVertex vertex);

    void afterVisit(NewMapInitExpressionVertex vertex);

    void afterVisit(NewMapLiteralExpressionVertex vertex);

    void afterVisit(NewObjectExpressionVertex vertex);

    void afterVisit(NewSetInitExpressionVertex vertex);

    void afterVisit(NewSetLiteralExpressionVertex vertex);

    void afterVisit(ReturnStatementVertex vertex);

    void afterVisit(SoqlExpressionVertex vertex);

    void afterVisit(ThrowStatementVertex vertex);

    void afterVisit(VariableDeclarationVertex vertex);

    void afterVisit(VariableExpressionVertex.Single vertex);
}

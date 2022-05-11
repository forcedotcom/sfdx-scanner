package com.salesforce.graph.visitor;

import com.salesforce.graph.ApexPath;
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
 * Visits vertices in a particular path. Return true if the visitor does not want to visit the
 * children.
 */
public interface PathVertexVisitor {
    /** */
    void recursionDetected(
            ApexPath currentPath,
            MethodCallExpressionVertex methodCallExpressionVertex,
            ApexPath recursivePath);

    boolean visit(AssignmentExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(BaseSFVertex vertex, SymbolProvider symbols);

    boolean visit(BlockStatementVertex vertex, SymbolProvider symbols);

    boolean visit(CatchBlockStatementVertex vertex, SymbolProvider symbols);

    boolean visit(DmlDeleteStatementVertex vertex, SymbolProvider symbols);

    boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols);

    boolean visit(DmlMergeStatementVertex vertex, SymbolProvider symbols);

    boolean visit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols);

    boolean visit(DmlUpdateStatementVertex vertex, SymbolProvider symbols);

    boolean visit(DmlUpsertStatementVertex vertex, SymbolProvider symbols);

    boolean visit(ElseWhenBlockVertex vertex, SymbolProvider symbols);

    boolean visit(EmptyReferenceExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(ExpressionStatementVertex vertex, SymbolProvider symbols);

    boolean visit(FieldDeclarationStatementsVertex vertex, SymbolProvider symbols);

    boolean visit(FieldDeclarationVertex vertex, SymbolProvider symbols);

    boolean visit(FieldVertex vertex, SymbolProvider symbols);

    boolean visit(ForEachStatementVertex vertex, SymbolProvider symbols);

    boolean visit(ForLoopStatementVertex vertex, SymbolProvider symbols);

    boolean visit(IfBlockStatementVertex vertex, SymbolProvider symbols);

    boolean visit(IfElseBlockStatementVertex vertex, SymbolProvider symbols);

    boolean visit(LiteralExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(MethodCallExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(MethodVertex.ConstructorVertex vertex, SymbolProvider symbols);

    boolean visit(MethodVertex.InstanceMethodVertex vertex, SymbolProvider symbols);

    boolean visit(ModifierNodeVertex vertex, SymbolProvider symbols);

    boolean visit(NewKeyValueObjectExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(NewListLiteralExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(NewObjectExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(ParameterVertex vertex, SymbolProvider symbols);

    boolean visit(PrefixExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(ReferenceExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(ReturnStatementVertex vertex, SymbolProvider symbols);

    boolean visit(SoqlExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(StandardConditionVertex.Negative vertex, SymbolProvider symbols);

    boolean visit(StandardConditionVertex.Positive vertex, SymbolProvider symbols);

    boolean visit(StandardConditionVertex.Unknown vertex, SymbolProvider symbols);

    boolean visit(SuperMethodCallExpressionVertex vertex, SymbolProvider symbols);

    boolean visit(SwitchStatementVertex vertex, SymbolProvider symbols);

    boolean visit(ThrowStatementVertex vertex, SymbolProvider symbols);

    boolean visit(TryCatchFinallyBlockStatementVertex vertex, SymbolProvider symbols);

    boolean visit(ValueWhenBlockVertex vertex, SymbolProvider symbols);

    boolean visit(VariableDeclarationVertex vertex, SymbolProvider symbols);

    boolean visit(VariableDeclarationStatementsVertex vertex, SymbolProvider symbols);

    boolean visit(VariableExpressionVertex.ForLoop vertex, SymbolProvider symbols);

    boolean visit(VariableExpressionVertex.Single vertex, SymbolProvider symbols);

    void afterVisit(BaseSFVertex vertex, SymbolProvider symbols);

    void afterVisit(DmlDeleteStatementVertex vertex, SymbolProvider symbols);

    void afterVisit(DmlInsertStatementVertex vertex, SymbolProvider symbols);

    void afterVisit(DmlMergeStatementVertex vertex, SymbolProvider symbols);

    void afterVisit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols);

    void afterVisit(DmlUpdateStatementVertex vertex, SymbolProvider symbols);

    void afterVisit(DmlUpsertStatementVertex vertex, SymbolProvider symbols);

    void afterVisit(FieldDeclarationVertex vertex, SymbolProvider symbols);

    void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols);

    void afterVisit(NewObjectExpressionVertex vertex, SymbolProvider symbols);

    void afterVisit(SoqlExpressionVertex vertex, SymbolProvider symbols);

    void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols);

    void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols);

    void afterVisit(ThrowStatementVertex vertex, SymbolProvider symbols);
}

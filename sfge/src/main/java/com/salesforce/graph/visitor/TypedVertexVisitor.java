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
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.ParameterVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.ReferenceExpressionVertex;
import com.salesforce.graph.vertex.ReturnStatementVertex;
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

/**
 * A visitor that allows for distinct return values. Use this class to avoid "instanceof" pattern.
 * TODO: Have VertexVisitor follow this same pattern
 */
public abstract class TypedVertexVisitor<T> {
    /** {@link #defaultVisit} is a no-op that returns null */
    public abstract static class DefaultNoOp<T> extends TypedVertexVisitor<T> {
        @Override
        public T defaultVisit(BaseSFVertex value) {
            return null;
        }
    }

    /** {@link #defaultVisit} throws an exception */
    public abstract static class DefaultThrow<T> extends TypedVertexVisitor<T> {
        @Override
        public T defaultVisit(BaseSFVertex value) {
            throw new UnexpectedException(value);
        }
    }

    abstract T defaultVisit(BaseSFVertex value);

    public T visit(ArrayLoadExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(AssignmentExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(BaseSFVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(BlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(CatchBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(DmlDeleteStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(DmlInsertStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(DmlUndeleteStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(DmlUpdateStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(DmlUpsertStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ElseWhenBlockVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(EmptyReferenceExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ExpressionStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(FieldDeclarationStatementsVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(FieldDeclarationVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(FieldVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ForEachStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ForLoopStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(IdentifierCaseVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(IfBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(IfElseBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(LiteralCaseVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(LiteralExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(MethodCallExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(MethodVertex.ConstructorVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(MethodVertex.InstanceMethodVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ModifierNodeVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(NewKeyValueObjectExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(NewListLiteralExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(NewObjectExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ParameterVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(PrefixExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ReferenceExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ReturnStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(StandardConditionVertex.Negative vertex) {
        return defaultVisit(vertex);
    }

    public T visit(StandardConditionVertex.Positive vertex) {
        return defaultVisit(vertex);
    }

    public T visit(StandardConditionVertex.Unknown vertex) {
        return defaultVisit(vertex);
    }

    public T visit(SwitchStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(SuperMethodCallExpressionVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ThrowStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(TryCatchFinallyBlockStatementVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(TypeWhenBlockVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(ValueWhenBlockVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(VariableDeclarationVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(VariableDeclarationStatementsVertex vertex) {
        return defaultVisit(vertex);
    }

    public T visit(VariableExpressionVertex.ForLoop vertex) {
        return defaultVisit(vertex);
    }

    public T visit(VariableExpressionVertex.Single vertex) {
        return defaultVisit(vertex);
    }

    public T visit(WhileLoopStatementVertex vertex) {
        return defaultVisit(vertex);
    }
}

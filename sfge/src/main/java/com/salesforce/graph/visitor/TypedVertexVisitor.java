package com.salesforce.graph.visitor;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.vertex.*;

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

    public T visit(ThisMethodCallExpressionVertex vertex) {
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

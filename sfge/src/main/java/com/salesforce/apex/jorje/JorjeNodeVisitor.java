package com.salesforce.apex.jorje;

/**
 * Use this class to avoid the need for "instanceof" calls. See {@link
 * NewKeyValueObjectExpressionWrapper.KeyNameVisitor}
 */
public class JorjeNodeVisitor {
    // BEGIN JorjeNode Subclasses
    public void defaultVisit(JorjeNode node) {
        // Intentionally left blank
    }

    public void visit(JorjeNode node) {
        defaultVisit(node);
    }

    public void visit(EngineDirectiveNode node) {
        defaultVisit(node);
    }

    // BEGIN AstNodeWrapper Subclasses
    public void defaultVisit(AstNodeWrapper<?> wrapper) {
        // Intentionally left blank
    }

    public void visit(AstNodeWrapper<?> wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(AnnotationWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(AnnotationParameterWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(AssignmentExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(BinaryExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(BooleanExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(CastExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(ClassRefExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(DefaultWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(ExpressionStatementWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(FieldWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(FieldDeclarationWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(LiteralExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(MethodCallExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(MethodWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(ModifierNodeWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewKeyValueObjectExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewListLiteralExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewListInitExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewMapLiteralExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewMapInitExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewObjectExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewSetLiteralExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(NewSetInitExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(ParameterWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(PostfixExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(PrefixExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(ReferenceExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(SoqlExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(UserClassWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(UserEnumWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(UserInterfaceWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(UserTriggerWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(VariableDeclarationWrapper wrapper) {
        defaultVisit(wrapper);
    }

    public void visit(VariableExpressionWrapper wrapper) {
        defaultVisit(wrapper);
    }
}

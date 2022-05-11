package com.salesforce.apex.jorje;

import apex.jorje.data.Identifier;
import apex.jorje.semantic.ast.expression.Expression;
import apex.jorje.semantic.ast.expression.NestedExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression.NameValueParameter;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ReflectionUtil;
import java.util.Map;

final class NewKeyValueObjectExpressionWrapper extends AstNodeWrapper<NewKeyValueObjectExpression> {
    NewKeyValueObjectExpressionWrapper(NewKeyValueObjectExpression node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.TYPE, typeRefToString(getNode().getTypeRef()));
    }

    /**
     * Returns the name of a parameters that is passed to the NewKeyValueObject.
     *
     * <p>MyObject__c obj = new MyObject__c(Item_Id__c = itemId);
     *
     * <p>Would return {@code Item_Id__c} for the parameter
     */
    private String getKeyName(AstNodeWrapper<?> astNodeWrapper) {
        for (NameValueParameter parameter : getNode().getParameters()) {
            Expression expression = parameter.getExpression();
            if (expression instanceof NestedExpression) {
                // This happens in cases where the value is surrounded in braces
                // MyKey = (String.valueOf(someMethod()).right(2))
                // The caller of this method is the MethodCallExpression, not the NestedExpression
                NestedExpression nestedExpression = (NestedExpression) expression;
                expression = nestedExpression.getExpression();
            }
            if (expression == astNodeWrapper.getNode()) {
                final Identifier identifier = ReflectionUtil.getFieldValue(parameter, "name");
                return identifier.getValue();
            }
        }
        throw new UnexpectedException(astNodeWrapper);
    }

    /**
     * Assigns the {@link Schema#KEY_NAME} property for children of {@link
     * NewKeyValueObjectExpressionWrapper}
     */
    static final class KeyNameVisitor extends JorjeNodeVisitor {
        private final AstNodeWrapper<?> child;
        private final Map<String, Object> properties;

        KeyNameVisitor(AstNodeWrapper<?> child, Map<String, Object> properties) {
            this.child = child;
            this.properties = properties;
        }

        @Override
        public void visit(NewKeyValueObjectExpressionWrapper node) {
            properties.put(Schema.KEY_NAME, node.getKeyName(child));
        }
    }
}

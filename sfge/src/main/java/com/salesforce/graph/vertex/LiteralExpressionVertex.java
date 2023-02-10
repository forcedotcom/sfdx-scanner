package com.salesforce.graph.vertex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** All of the types as defined in {@link apex.jorje.data.ast.LiteralType} */
public abstract class LiteralExpressionVertex<T> extends ChainedVertex
        implements KeyVertex, Typeable {
    protected final ExpressionType expressionType;

    private LiteralExpressionVertex(Map<Object, Object> properties, ExpressionType expressionType) {
        super(properties);
        this.expressionType = expressionType;
    }

    @Override
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    @Override
    public String getCanonicalType() {
        return (String) properties.get(Schema.LITERAL_TYPE);
    }

    @Override
    public Optional<String> getKeyName() {
        if (expressionType.equals(ExpressionType.KEY_VALUE)) {
            return Optional.of(getString(Schema.KEY_NAME));
        } else {
            return Optional.of(getLiteralAsString());
        }
    }

    public abstract T getLiteral();

    @Override
    public boolean isResolvable() {
        return false;
    }

    @Override
    public Optional<String> getSymbolicName() {
        return Optional.empty();
    }

    public final String getLiteralAsString() {
        return getString(Schema.VALUE);
    }

    private abstract static class BooleanLiteralExpressionVertex
            extends LiteralExpressionVertex<Boolean> {
        private final Boolean value;

        private BooleanLiteralExpressionVertex(
                Map<Object, Object> properties, ExpressionType type, Boolean value) {
            super(properties, type);
            this.value = value;
        }

        @Override
        public String getCanonicalType() {
            return "Boolean";
        }

        @Override
        public Boolean getLiteral() {
            return value;
        }

        protected static boolean isExpectedLiteral(
                BaseSFVertex vertex,
                Class<? extends BooleanLiteralExpressionVertex> expectation,
                Class<? extends BooleanLiteralExpressionVertex> opposite) {
            // If we're given a negation expression, we can check whether the thing being negated is
            // the opposite of the value we expect.
            // E.g., we can determine whether !X is literally false by checking if X is literally
            // true.
            if (vertex instanceof PrefixExpressionVertex) {
                PrefixExpressionVertex prefix = (PrefixExpressionVertex) vertex;
                if (prefix.isOperatorNegation()) {
                    return isExpectedLiteral(prefix.getChild(0), opposite, expectation);
                }
            }
            // For anything else, just check if it's an instance of the expected literal.
            return expectation.isInstance(vertex);
        }
    }

    public static final class False extends BooleanLiteralExpressionVertex {
        private False(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type, Boolean.FALSE);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        /**
         * Return true if the provided vertex is equivalent to the literal False.
         * E.g., `false`, `!true`, `!!false`.
         */
        public static boolean isLiterallyFalse(BaseSFVertex vertex) {
            return isExpectedLiteral(vertex, False.class, True.class);
        }
    }

    public static final class True extends BooleanLiteralExpressionVertex {
        private True(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type, Boolean.FALSE);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        /**
         * Return true if the provided vertex is equivalent to the literal True.
         * E.g., `true`, `!false`, `!!true`.
         */
        public static boolean isLiterallyTrue(BaseSFVertex vertex) {
            return isExpectedLiteral(vertex, True.class, False.class);
        }
    }

    public static final class Decimal extends LiteralExpressionVertex<BigDecimal> {
        private Decimal(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public BigDecimal getLiteral() {
            return new BigDecimal(getLiteralAsString());
        }
    }

    public static final class Double extends LiteralExpressionVertex<java.lang.Double> {
        private Double(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public java.lang.Double getLiteral() {
            return java.lang.Double.valueOf(getLiteralAsString());
        }
    }

    public static final class Integer extends LiteralExpressionVertex<java.lang.Integer> {
        private Integer(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public java.lang.Integer getLiteral() {
            return getInteger(Schema.VALUE);
        }
    }

    public static final class Long extends LiteralExpressionVertex<java.lang.Long> {
        private Long(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public java.lang.Long getLiteral() {
            return getLong(Schema.VALUE);
        }
    }

    public static final class Null extends LiteralExpressionVertex<Object> {
        private Null(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public Object getLiteral() {
            return null;
        }

        @Override
        public boolean matchesParameterType(Typeable parameterVertex) {
            // Null matches everything
            return true;
        }
    }

    public static final class SFString extends LiteralExpressionVertex<String> {
        private SFString(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public String getLiteral() {
            return getLiteralAsString();
        }
    }

    @SuppressWarnings(
            "PMD.PrimitiveWrapperInstantiation") // Rule does not distinguish classes based on
    // import and these are not Java Language boxed
    // classes
    public static final class Builder {
        public static LiteralExpressionVertex create(Map<Object, Object> vertex) {
            return create(vertex, ExpressionType.SIMPLE);
        }

        public static LiteralExpressionVertex create(Map<Object, Object> vertex, Object type) {
            String literalType = (String) vertex.get(Schema.LITERAL_TYPE);
            // These are intentionally not defined as constants in the ASTConstants in order to
            // avoid code relying on them
            switch (literalType) {
                case "DECIMAL":
                    return new Decimal(vertex, (ExpressionType) type);
                case "DOUBLE":
                    return new Double(vertex, (ExpressionType) type);
                case "INTEGER":
                    return new Integer(vertex, (ExpressionType) type);
                case "LONG":
                    return new Long(vertex, (ExpressionType) type);
                case "NULL":
                    return new Null(vertex, (ExpressionType) type);
                case "FALSE":
                    return new BooleanLiteralExpressionVertex.False(vertex, (ExpressionType) type);
                case "TRUE":
                    return new BooleanLiteralExpressionVertex.True(vertex, (ExpressionType) type);
                case "STRING":
                    return new SFString(vertex, (ExpressionType) type);
                default:
                    throw new UnexpectedException(vertex);
            }
        }
    }
}

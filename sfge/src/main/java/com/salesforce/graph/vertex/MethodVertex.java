package com.salesforce.graph.vertex;

import com.salesforce.Collectible;
import com.salesforce.NullCollectible;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;

public abstract class MethodVertex extends FieldWithModifierVertex implements NamedVertex {
    private final LazyVertexList<AnnotationVertex> annotations;
    private final LazyVertexList<ParameterVertex> parameters;
    private final SignatureInitializer signatureInitializer;

    private MethodVertex(Map<Object, Object> properties) {
        super(properties);
        this.parameters = _getParameters();
        this.annotations = _getAnnotations();
        this.signatureInitializer = new SignatureInitializer();
    }

    public List<ParameterVertex> getParameters() {
        return this.parameters.get();
    }

    @Override
    public String getName() {
        return getString(Schema.NAME);
    }

    public String getSignature() {
        return signatureInitializer.get();
    }

    private final class SignatureInitializer extends UncheckedLazyInitializer<String> {
        @Override
        protected String initialize() throws ConcurrentException {
            StringBuilder sb =
                    new StringBuilder(getReturnType()).append(' ').append(getName()).append('(');
            List<String> parameterTypes =
                    getParameters().stream()
                            .map(p -> p.getCanonicalType())
                            .collect(Collectors.toList());
            sb.append(String.join(",", parameterTypes)).append(')');
            return sb.toString();
        }
    }

    public boolean isConstructor() {
        return getBoolean(Schema.CONSTRUCTOR);
    }

    public int getArity() {
        return getInteger(Schema.ARITY);
    }

    public String getReturnType() {
        return getString(Schema.RETURN_TYPE);
    }

    public boolean isTest() {
        return getBoolean(Schema.IS_TEST);
    }

    public String toSimpleString() {
        return getDefiningType() + ":" + getName() + ":" + getBeginLine();
    }

    @Override
    public List<AnnotationVertex> getAnnotations() {
        return annotations.get();
    }

    public LazyVertexList<AnnotationVertex> _getAnnotations() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.ANNOTATION)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }

    private LazyVertexList<ParameterVertex> _getParameters() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.PARAMETER) // Invocation of method
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }

    public static final class ConstructorVertex extends MethodVertex
            implements Collectible<ConstructorVertex> {
        public static final NullCollectible<ConstructorVertex> NULL_VALUE =
                new NullCollectible<>(ConstructorVertex.class);

        private ConstructorVertex(Map<Object, Object> properties) {
            super(properties);
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

        @Nullable
        @Override
        public ConstructorVertex getCollectible() {
            return this;
        }
    }

    public static final class StaticBlockVertex extends MethodVertex
            implements Collectible<StaticBlockVertex> {
        public static final NullCollectible<StaticBlockVertex> NULL_VALUE =
                new NullCollectible<>(StaticBlockVertex.class);

        private StaticBlockVertex(Map<Object, Object> properties) {
            super(properties);
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

        @Nullable
        @Override
        public StaticBlockVertex getCollectible() {
            return this;
        }
    }

    public static final class InstanceMethodVertex extends MethodVertex {
        private InstanceMethodVertex(Map<Object, Object> properties) {
            super(properties);
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
    }

    public static final class Builder {
        public static MethodVertex create(Map<Object, Object> vertex) {
            final boolean isStaticBlockMethod =
                    BaseSFVertex.toBoolean(vertex.get(Schema.IS_STATIC_BLOCK_METHOD));
            boolean isConstructor = BaseSFVertex.toBoolean(vertex.get(Schema.CONSTRUCTOR));
            if (isStaticBlockMethod) {
                return new StaticBlockVertex(vertex);
            } else if (isConstructor) {
                return new ConstructorVertex(vertex);
            }
            return new InstanceMethodVertex(vertex);
        }
    }
}

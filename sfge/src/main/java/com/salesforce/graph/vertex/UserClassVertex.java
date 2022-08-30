package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;

public class UserClassVertex extends BaseSFVertex implements InheritableSFVertex, NamedVertex {
    private final LazyVertexList<AnnotationVertex> annotations;

    UserClassVertex(Map<Object, Object> properties) {
        super(properties);
        this.annotations = _getAnnotations();
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
     * TODO: Check whether this class can be safely made to extend {@link FieldWithModifierVertex}.
     */
    public boolean isAbstract() {
        return ((ModifierNodeVertex) getOnlyChild(ASTConstants.NodeType.MODIFIER_NODE))
                .isAbstract();
    }

    /**
     * TODO: Check whether this class can be safely made to extend {@link FieldWithModifierVertex}.
     */
    public boolean isVirtual() {
        return ((ModifierNodeVertex) getOnlyChild(ASTConstants.NodeType.MODIFIER_NODE)).isVirtual();
    }

    @Override
    public String getName() {
        return getString(Schema.NAME);
    }

    @Override
    public Optional<String> getSuperClassName() {
        String superClassName = getString(Schema.SUPER_CLASS_NAME);
        if (StringUtils.isNotEmpty(superClassName)) {
            return Optional.of(superClassName);
        } else {
            return Optional.empty();
        }
    }

    // TODO: Treeset for case insensitivity
    public List<String> getInterfaceNames() {
        return getStrings(Schema.INTERFACE_NAMES);
    }

    public boolean isTest() {
        return getBoolean(Schema.IS_TEST);
    }

    @Override
    public List<AnnotationVertex> getAnnotations() {
        return annotations.get();
    }

    private LazyVertexList<AnnotationVertex> _getAnnotations() {
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
}

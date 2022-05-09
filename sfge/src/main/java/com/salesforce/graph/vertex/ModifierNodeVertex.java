package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public class ModifierNodeVertex extends BaseSFVertex {
    private final LazyVertexList<AnnotationVertex> annotations;

    ModifierNodeVertex(Map<Object, Object> properties) {
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

    // TODO: The vertices also have explicit Private/Public attributes.
    // Which should we use? If we don't use the explicit ones we can exclude them from the graph
    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    public boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public boolean isGlobal() {
        return getBoolean(Schema.GLOBAL);
    }

    public Integer getModifiers() {
        return (Integer) properties.get(Schema.MODIFIERS);
    }

    @Override
    public List<AnnotationVertex> getAnnotations() {
        return annotations.get();
    }

    private LazyVertexList<AnnotationVertex> _getAnnotations() {
        return new LazyVertexList<>(
                () -> g().V(getId()).out(Schema.CHILD).hasLabel(ASTConstants.NodeType.ANNOTATION));
    }
}

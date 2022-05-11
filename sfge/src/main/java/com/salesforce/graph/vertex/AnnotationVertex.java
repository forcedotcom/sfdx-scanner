package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Map;

public class AnnotationVertex extends BaseSFVertex implements NamedVertex {
    private final LazyVertexList<AnnotationParameterVertex> parameters;

    public AnnotationVertex(Map<Object, Object> properties) {
        super(properties);
        this.parameters = _getParameters();
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

    public List<AnnotationParameterVertex> getParameters() {
        return parameters.get();
    }

    private LazyVertexList<AnnotationParameterVertex> _getParameters() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.ANNOTATION_PARAMETER));
    }

    /** @return the value after the @ sign. @TestVisible will return 'TestVisible' */
    @Override
    public String getName() {
        return getString(Schema.NAME);
    }
}

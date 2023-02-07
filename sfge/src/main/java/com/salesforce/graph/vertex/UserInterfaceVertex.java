package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class UserInterfaceVertex extends FieldWithModifierVertex
        implements InheritableSFVertex, NamedVertex {
    UserInterfaceVertex(Map<Object, Object> properties) {
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

    @Override
    public String getName() {
        return getString(Schema.NAME);
    }

    @Override
    public Optional<String> getSuperClassName() {
        String superClassName = getString(Schema.SUPER_INTERFACE_NAME);
        if (StringUtils.isNotEmpty(superClassName)) {
            return Optional.of(superClassName);
        } else {
            return Optional.empty();
        }
    }
}

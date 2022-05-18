package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NewSetLiteralExpressionVertex extends AbstractCollectionExpressionVertex {
    private static final Logger LOGGER = LogManager.getLogger(NewSetLiteralExpressionVertex.class);

    NewSetLiteralExpressionVertex(Map<Object, Object> properties) {
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
    public String getTypePrefix() {
        return ASTConstants.TypePrefix.SET;
    }

    @Override
    public boolean matchesParameterType(Typeable parameterVertex) {
        final String parameterVertexCanonicalType = parameterVertex.getCanonicalType();

        if (parameterVertexCanonicalType
                .toLowerCase(Locale.ROOT)
                .startsWith(getTypePrefix().toLowerCase(Locale.ROOT))) {
            // Unlike List, Sets in Apex do not abide by type hierarchy to choose method overload.
            // The type has to be exactly identical.
            if (parameterVertexCanonicalType.equalsIgnoreCase(this.getCanonicalType())) {
                // Not returning the check directly so that the LOGGER below can be invoked when the
                // check didn't work.
                return true;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Parameter canonical type ("
                            + parameterVertexCanonicalType
                            + ") did not match current set's canonical type ("
                            + this.getCanonicalType()
                            + ")");
        }
        return false;
    }
}

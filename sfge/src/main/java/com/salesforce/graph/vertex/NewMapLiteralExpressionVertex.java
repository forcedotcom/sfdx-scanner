package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NewMapLiteralExpressionVertex extends AbstractCollectionExpressionVertex {
    private static final Logger LOGGER = LogManager.getLogger(NewMapLiteralExpressionVertex.class);

    NewMapLiteralExpressionVertex(Map<Object, Object> properties) {
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

    /** Casts the results of {@link #getParameters()} to {@link MapEntryNodeVertex} instances */
    public List<MapEntryNodeVertex> getEntries() {
        return getParameters().stream()
                .map(v -> (MapEntryNodeVertex) v)
                .collect(Collectors.toList());
    }

    @Override
    public boolean matchesParameterType(Typeable parameterVertex) {
        final String parameterVertexCanonicalType = parameterVertex.getCanonicalType();

        if (parameterVertexCanonicalType
                .toLowerCase(Locale.ROOT)
                .startsWith(getTypePrefix().toLowerCase(Locale.ROOT))) {
            // TODO: Oddly, Maps support class hierarchy but not SObject hierarchy
            //  For now, we are dealing with no hierarchy in Maps type matching
            // TODO: Type matching should use patterns and not equalsIgnoreCase()
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
                            + ") did not match current map's canonical type ("
                            + this.getCanonicalType()
                            + ")");
        }
        return false;
    }

    @Override
    public String getTypePrefix() {
        return ASTConstants.TypePrefix.MAP;
    }

    /** Get Key Type and Value Type based on the first entry in the map */
    @SuppressWarnings("PMD.UnusedPrivateMethod") // Will be used in future
    private Optional<Pair<Typeable, Typeable>> getKeyValueTypes() {
        final List<BaseSFVertex> children = this.getChildren();
        if (!children.isEmpty()) {
            final BaseSFVertex firstChild = children.get(0);
            if (firstChild instanceof MapEntryNodeVertex) {
                final BaseSFVertex keyVertex = ((MapEntryNodeVertex) firstChild).getKey();
                final BaseSFVertex valueVertex = ((MapEntryNodeVertex) firstChild).getValue();

                if (keyVertex instanceof Typeable && valueVertex instanceof Typeable) {
                    // Return a Pair only if we have both types
                    return Optional.of(Pair.of((Typeable) keyVertex, (Typeable) valueVertex));
                }
            } else {
                throw new UnexpectedException(
                        "First child of NewMapLiteralExpressionVertex is not a MapEntryNode: "
                                + firstChild);
            }
        }
        return Optional.empty();
    }
}

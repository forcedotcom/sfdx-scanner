package com.salesforce.graph.vertex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.ApexIntegerValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class ArrayLoadExpressionVertex extends InvocableWithParametersVertex {
    private static final Consumer<GraphTraversal<Vertex, Vertex>> TRAVERSAL_CONSUMER =
            traversal ->
                    traversal
                            .out(Schema.CHILD)
                            .order(Scope.global)
                            .by(Schema.CHILD_INDEX, Order.asc);

    ArrayLoadExpressionVertex(Map<Object, Object> properties) {
        super(properties, TRAVERSAL_CONSUMER);
        if (getParameters().size() != 2) {
            throw new UnexpectedException(this);
        }
    }

    ArrayLoadExpressionVertex(Map<Object, Object> properties, Object supplementalParam) {
        super(properties, TRAVERSAL_CONSUMER);
        if (supplementalParam != null) {
            throw new UnexpectedException(supplementalParam);
        }
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

    public Optional<String> getSymbolicName() {
        return getParameters().get(0).getSymbolicName();
    }

    public ChainedVertex getArray() {
        return getParameters().get(0);
    }

    public ChainedVertex getIndex() {
        return getParameters().get(1);
    }

    public Optional<Integer> getIndexAsInteger(SymbolProvider symbols) {
        ChainedVertex vertex = getIndex();
        ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(symbols, vertex).orElse(null);
        if (apexValue instanceof ApexIntegerValue) {
            ApexIntegerValue apexIntegerValue = (ApexIntegerValue) apexValue;
            if (apexIntegerValue.isValuePresent()) {
                return Optional.of(apexIntegerValue.getValue().get());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}

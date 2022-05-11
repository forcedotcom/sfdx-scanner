package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.SoqlQueryInfo;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class SoqlExpressionVertex extends InvocableWithParametersVertex implements Typeable {
    private static final Logger LOGGER = LogManager.getLogger(SoqlExpressionVertex.class);

    private static final Consumer<GraphTraversal<Vertex, Vertex>> TRAVERSAL_CONSUMER =
            traversal ->
                    traversal
                            .out(Schema.CHILD)
                            .order(Scope.global)
                            .by(Schema.CHILD_INDEX, Order.asc);

    private final LazyOptionalVertex<BaseSFVertex> nextInvocableVertex;
    private final HashSet<SoqlQueryInfo> queryInfos;

    // Used only through reflection
    SoqlExpressionVertex(Map<Object, Object> properties) {
        super(properties, TRAVERSAL_CONSUMER);
        this.nextInvocableVertex = _getNext();
        this.queryInfos = new HashSet<>();
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
    public Optional<InvocableVertex> getNext() {
        InvocableVertex invocableVertex = (InvocableVertex) nextInvocableVertex.get().orElse(null);
        return Optional.ofNullable(invocableVertex);
    }

    private LazyOptionalVertex<BaseSFVertex> _getNext() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.PARENT)
                                .hasLabel(ASTConstants.NodeType.REFERENCE_EXPRESSION)
                                .out(Schema.PARENT)
                                .hasLabel(
                                        ASTConstants.NodeType.METHOD_CALL_EXPRESSION,
                                        ASTConstants.NodeType.VARIABLE_EXPRESSION));
    }

    /**
     * Gets parsed information about the query. Lazy load query information to avoid parsing unless
     * necessary.
     *
     * @return
     */
    public HashSet<SoqlQueryInfo> getQueryInfo() {
        if (queryInfos.isEmpty()) {
            final String rawQuery = getRawQuery();
            queryInfos.addAll(SoqlParserUtil.parseQuery(rawQuery));
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query parsed into queryInfoList: " + queryInfos);
            }
        }
        return queryInfos;
    }

    private String getRawQuery() {
        final String rawQuery = this.getOrDefault(Schema.QUERY, "");
        if (StringUtils.isAnyEmpty(rawQuery)) {
            throw new UnexpectedException("No query found on SoqlExpressionVertex: " + this);
        }
        return rawQuery;
    }

    @Override
    public String getCanonicalType() {
        return SoqlParserUtil.getObjectName(getQueryInfo());
    }

    @Override
    public TypeableUtil.OrderedTreeSet getTypes() {
        if (SoqlParserUtil.isSingleSObject(getQueryInfo())) {
            return TypeableUtil.getTypeHierarchy(getCanonicalType());
        }
        return TypeableUtil.getTypeHierarchy(
                ApexStandardLibraryUtil.getListDeclaration(getCanonicalType()));
    }
}

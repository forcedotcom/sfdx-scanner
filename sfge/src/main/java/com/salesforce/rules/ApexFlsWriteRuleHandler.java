package com.salesforce.rules;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.fls.apex.AbstractFlsVisitor;
import com.salesforce.rules.fls.apex.DmlDeleteFlsRuleVisitor;
import com.salesforce.rules.fls.apex.DmlInsertFlsRuleVisitor;
import com.salesforce.rules.fls.apex.DmlMergeFlsRuleVisitor;
import com.salesforce.rules.fls.apex.DmlUndeleteFlsRuleVisitor;
import com.salesforce.rules.fls.apex.DmlUpdateFlsRuleVisitor;
import com.salesforce.rules.fls.apex.DmlUpsertFlsRuleVisitor;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Handler invoked by {@link ApexFlsViolationRule} to notice Write operation vertices and apply the
 * correct {@link AbstractFlsVisitor} to detect violations.
 */
final class ApexFlsWriteRuleHandler implements FlsRuleHandler {
    private static final Logger LOGGER = LogManager.getLogger(ApexFlsWriteRuleHandler.class);

    private static final TreeMap<String, Supplier<AbstractFlsVisitor>>
            DML_STATEMENT_TYPE_TO_VISITOR =
                    CollectionUtil.newTreeMapOf(
                            FlsConstants.FlsValidationType.DELETE.dmlStatementType,
                                    DmlDeleteFlsRuleVisitor::new,
                            FlsConstants.FlsValidationType.INSERT.dmlStatementType,
                                    DmlInsertFlsRuleVisitor::new,
                            FlsConstants.FlsValidationType.MERGE.dmlStatementType,
                                    DmlMergeFlsRuleVisitor::new,
                            FlsConstants.FlsValidationType.UPDATE.dmlStatementType,
                                    DmlUpdateFlsRuleVisitor::new,
                            FlsConstants.FlsValidationType.UPSERT.dmlStatementType,
                                    DmlUpsertFlsRuleVisitor::new,
                            FlsConstants.FlsValidationType.UNDELETE.dmlStatementType,
                                    DmlUndeleteFlsRuleVisitor::new);

    private static final TreeMap<String, Supplier<AbstractFlsVisitor>> DATABASE_METHOD_TO_VISITOR =
            CollectionUtil.newTreeMapOf(
                    FlsConstants.FlsValidationType.DELETE.databaseOperationMethod,
                            DmlDeleteFlsRuleVisitor::new,
                    FlsConstants.FlsValidationType.INSERT.databaseOperationMethod,
                            DmlInsertFlsRuleVisitor::new,
                    FlsConstants.FlsValidationType.MERGE.databaseOperationMethod,
                            DmlMergeFlsRuleVisitor::new,
                    FlsConstants.FlsValidationType.UPDATE.databaseOperationMethod,
                            DmlUpdateFlsRuleVisitor::new,
                    FlsConstants.FlsValidationType.UPSERT.databaseOperationMethod,
                            DmlUpsertFlsRuleVisitor::new,
                    FlsConstants.FlsValidationType.UNDELETE.databaseOperationMethod,
                            DmlUndeleteFlsRuleVisitor::new);

    @Override
    public Set<FlsViolationInfo> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Examining path. path=" + path);
        }
        // Get a new instance of the visitor we are interested in running
        // We won't have an instance in case the vertex is a read operation and is not covered here.
        final Optional<Supplier<AbstractFlsVisitor>> visitorSupplier = getSupplier(vertex);
        if (visitorSupplier.isPresent()) {
            final AbstractFlsVisitor flsRuleVisitor = visitorSupplier.get().get();
            flsRuleVisitor.setTargetVertex(vertex);

            DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, flsRuleVisitor, symbols);

            return flsRuleVisitor.getViolations();
        }

        return new HashSet<>();
    }

    private Optional<Supplier<AbstractFlsVisitor>> getSupplier(BaseSFVertex vertex) {
        if (vertex instanceof MethodCallExpressionVertex) {
            MethodCallExpressionVertex methodCallExpression = (MethodCallExpressionVertex) vertex;
            return Optional.ofNullable(
                    DATABASE_METHOD_TO_VISITOR.get(methodCallExpression.getFullMethodName()));
        } else {
            return Optional.ofNullable(DML_STATEMENT_TYPE_TO_VISITOR.get(vertex.getLabel()));
        }
    }

    @Override
    public boolean test(BaseSFVertex vertex) {
        return getSupplier(vertex).isPresent();
    }

    static ApexFlsWriteRuleHandler getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final ApexFlsWriteRuleHandler INSTANCE = new ApexFlsWriteRuleHandler();
    }

    private ApexFlsWriteRuleHandler() {}
}

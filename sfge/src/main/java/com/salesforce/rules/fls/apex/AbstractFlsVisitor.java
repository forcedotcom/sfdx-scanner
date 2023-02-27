package com.salesforce.rules.fls.apex;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.DmlStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.rules.fls.apex.operations.FlsValidationCentral;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.rules.fls.apex.operations.StandardConditionDecomposer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractFlsVisitor extends BooleanStateDetectorVisitor {
    protected static final Logger LOGGER = LogManager.getLogger(AbstractFlsVisitor.class);

    protected final FlsValidationCentral validationCentral;
    private final FlsValidationType validationType;

    /** Specifies the specific vertex that the visitor should gather information about. */
    private BaseSFVertex targetVertex;

    AbstractFlsVisitor(FlsValidationType validationType) {
        this(validationType, new FlsValidationCentral(validationType));
    }

    AbstractFlsVisitor(FlsValidationType validationType, FlsValidationCentral validationCentral) {
        super();
        this.validationType = validationType;
        this.validationCentral = validationCentral;
    }

    public Set<FlsViolationInfo> getViolations() {
        return validationCentral.getViolations();
    }

    public boolean isSafe() {
        return validationCentral.getViolations().isEmpty();
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (shouldCollectInfo(vertex)) {
            // Change gears and check if we encountered a new DML operation invoked through Database
            // namespace
            final String fullMethodName = vertex.getFullMethodName();
            if (validationType.databaseOperationMethod.equalsIgnoreCase(fullMethodName)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Encountered database operation: " + fullMethodName);
                }
                validationCentral.createExpectedValidations(vertex, symbols);
                validationCentral.tallyValidations(vertex);
            }
        }
    }

    @Override
    public void afterVisit(StandardConditionVertex.Negative vertex, SymbolProvider symbols) {
        if (containsPotentiallyValidCheck()) {
            List<BaseSFVertex> relevantClauses =
                    StandardConditionDecomposer.decomposeStandardCondition(vertex);
            for (BaseSFVertex clause : relevantClauses) {
                validationCentral.checkSchemaBasedFlsValidation(
                        clause.getParent(), clause, symbols);
            }
        }
        super.afterVisit(vertex, symbols);
    }

    @Override
    public void afterVisit(StandardConditionVertex.Positive vertex, SymbolProvider symbols) {
        if (containsPotentiallyValidCheck()) {
            List<BaseSFVertex> relevantClauses =
                    StandardConditionDecomposer.decomposeStandardCondition(vertex);
            for (BaseSFVertex clause : relevantClauses) {
                validationCentral.checkSchemaBasedFlsValidation(
                        clause.getParent(), clause, symbols);
            }
        }
        super.afterVisit(vertex, symbols);
    }

    protected boolean shouldCollectInfo(BaseSFVertex vertex) {
        // Only gather information if the target vertex hasn't been set or it has been set and it
        // matches
        return !getTargetVertex().isPresent() || getTargetVertex().get().equals(vertex);
    }

    protected void afterVisitDmlStatementVertex(DmlStatementVertex vertex, SymbolProvider symbols) {
        if (shouldCollectInfo(vertex)) {
            validationCentral.createExpectedValidations(vertex, symbols);
            validationCentral.tallyValidations(vertex);
        }
    }

    public void setTargetVertex(BaseSFVertex targetVertex) {
        this.targetVertex = targetVertex;
    }

    protected Optional<BaseSFVertex> getTargetVertex() {
        return Optional.ofNullable(targetVertex);
    }
}

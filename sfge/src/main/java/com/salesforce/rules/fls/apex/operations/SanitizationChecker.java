package com.salesforce.rules.fls.apex.operations;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.AbstractSanitizableValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.MethodBasedSanitization;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.DmlStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.rules.fls.apex.operations.FlsConstants.FlsValidationType;
import com.salesforce.rules.fls.apex.operations.FlsConstants.StripInaccessibleAccessType;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks if stripInaccessible sanitization was performed on the DML operation. This cannot be used
 * for Read operations. Reason for this decision is that in read, stripInaccessible check happens
 * after the Read operation. We won't have any sanitization performed on the apex value yet.
 */
public class SanitizationChecker {
    private static final Logger LOGGER = LogManager.getLogger(SanitizationChecker.class);

    private SanitizationChecker() {}

    static boolean checkSanitization(DmlStatementVertex dmlVertex, SymbolProvider symbols) {
        final FlsValidationType validationType = FlsValidationType.getValidationType(dmlVertex);
        final Optional<StripInaccessibleAccessType> expectedAccessType =
                getExpectedAccessType(validationType);
        final Optional<ApexValue<?>> dmlValueOptional = getValue(dmlVertex, symbols);

        if (dmlValueOptional.isPresent() && expectedAccessType.isPresent()) {
            if (checkSanitization(dmlValueOptional.get(), expectedAccessType.get())) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Detected stripInaccessible check {} for DML statement {}",
                            dmlValueOptional.get(),
                            dmlVertex);
                }
                return true;
            }
        }

        return false;
    }

    static boolean checkSanitization(
            MethodCallExpressionVertex methodCallVertex, SymbolProvider symbols) {
        final Optional<FlsValidationType> validationType =
                FlsValidationType.getValidationType(methodCallVertex);
        if (validationType.isPresent()) {
            final Optional<StripInaccessibleAccessType> expectedAccessType =
                    getExpectedAccessType(validationType.get());
            final Optional<ApexValue<?>> dmlValue = getValue(methodCallVertex, symbols);

            if (dmlValue.isPresent() && expectedAccessType.isPresent()) {
                if (checkSanitization(dmlValue.get(), expectedAccessType.get())) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "Detected stripInaccessible check {} for DML method {}",
                                dmlValue.get(),
                                methodCallVertex);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkSanitization(
            ApexValue<?> dmlValue, StripInaccessibleAccessType expectedAccessType) {
        if (dmlValue instanceof AbstractSanitizableValue) {
            return ((AbstractSanitizableValue) dmlValue)
                    .isSanitized(
                            MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE,
                            expectedAccessType);
        }

        return false;
    }

    private static Optional<StripInaccessibleAccessType> getExpectedAccessType(
            FlsValidationType validationType) {
        // We want to proceed only if we have a stripInaccessible-supported operation
        // and the operation is not READ
        if (validationType.isStripInaccessibleSupported
                && !FlsValidationType.READ.equals(validationType)) {
            return Optional.of(validationType.stripInaccessibleAccessType);
        }
        return Optional.empty();
    }

    private static Optional<ApexValue<?>> getValue(
            DmlStatementVertex dmlVertex, SymbolProvider symbols) {
        final List<BaseSFVertex> children = dmlVertex.getChildren();
        if (!children.isEmpty()) {
            // TODO: are all types covered here?
            return ScopeUtil.resolveToApexValue(symbols, (ChainedVertex) children.get(0));
        } else {
            throw new UnexpectedException("DML vertex has no children: " + dmlVertex);
        }
    }

    private static Optional<ApexValue<?>> getValue(
            MethodCallExpressionVertex methodCall, SymbolProvider symbols) {
        final List<ChainedVertex> parameters = methodCall.getParameters();
        if (!parameters.isEmpty()) {
            return ScopeUtil.resolveToApexValue(symbols, parameters.get(0));
        } else {
            throw new UnexpectedException("No parameters found on Database call: " + methodCall);
        }
    }
}

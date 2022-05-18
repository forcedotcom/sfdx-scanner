package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.vertex.ChainedVertex;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SObjectAccessDecisionFactory {
    private static final Logger LOGGER = LogManager.getLogger(SObjectAccessDecisionFactory.class);

    private static final String EXPECTED_METHOD_NAME = "Security.stripInaccessible";
    private static final int ACCESS_CHECK_TYPE_PARAM_INDEX = 0;
    private static final int LIST_VALUE_PARAM_INDEX = 1;

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            ((g, vertex, symbols) -> {
                final String fullMethodName = vertex.getFullMethodName();

                if (EXPECTED_METHOD_NAME.equalsIgnoreCase(fullMethodName)) {

                    // We can expect one of two overrides:
                    // stripInaccessible(accessCheckType, sourceRecords)
                    // stripInaccessible(accessCheckType, sourceRecords, enforceRootObjectCRUD)
                    final List<ChainedVertex> parameters = vertex.getParameters();

                    if (parameters.size() == 2 || parameters.size() == 3) {
                        // We have a match

                        // Examine the parameters
                        final Optional<ApexValue<?>> accessTypeParamValue =
                                ScopeUtil.resolveToApexValue(
                                        symbols, parameters.get(ACCESS_CHECK_TYPE_PARAM_INDEX));
                        final Optional<ApexValue<?>> dataParamValue =
                                ScopeUtil.resolveToApexValue(
                                        symbols, parameters.get(LIST_VALUE_PARAM_INDEX));

                        final ApexValueBuilder apexValueBuilder =
                                ApexValueBuilder.get(symbols).valueVertex(vertex);

                        if (accessTypeParamValue.isPresent()
                                && !(accessTypeParamValue.get() instanceof ApexEnumValue)) {
                            throw new UnexpectedException(
                                    "AccessType param value should be an ApexEnumValue. value="
                                            + accessTypeParamValue.get());
                        }

                        if (dataParamValue.isPresent()
                                && !(dataParamValue.get() instanceof AbstractSanitizableValue)) {
                            throw new TodoException(
                                    "Parameter value on stripInaccessible() call should be handled: "
                                            + dataParamValue.get());
                        }

                        if (!accessTypeParamValue.isPresent() && !dataParamValue.isPresent()) {
                            // Create indeterminant value
                            return Optional.of(
                                    apexValueBuilder
                                            .withStatus(ValueStatus.INDETERMINANT)
                                            .buildSObjectAccessDecision(null, null));
                        }

                        if (!accessTypeParamValue.isPresent() && dataParamValue.isPresent()) {
                            // Create indeterminant value
                            return Optional.of(
                                    apexValueBuilder
                                            .withStatus(ValueStatus.INDETERMINANT)
                                            .buildSObjectAccessDecision(
                                                    null,
                                                    (AbstractSanitizableValue)
                                                            dataParamValue.get()));
                        }

                        if (accessTypeParamValue.isPresent() && dataParamValue.isPresent()) {
                            return Optional.of(
                                    apexValueBuilder.buildSObjectAccessDecision(
                                            (ApexEnumValue) accessTypeParamValue.get(),
                                            (AbstractSanitizableValue) dataParamValue.get()));
                        }

                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(
                                    "List value could not be resolved for AccessDecision: "
                                            + vertex);
                        }
                    } else {
                        throw new UnexpectedException(
                                "Expected only 2 or 3 parameters for stripInaccessible(): "
                                        + vertex);
                    }
                }
                return Optional.empty();
            });
}

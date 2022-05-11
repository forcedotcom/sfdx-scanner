package com.salesforce.graph.ops.expander;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Immutable;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Collapser that looks at the value returned from the same method that caused a {@link ForkEvent}.
 * Results are ranked by their specificity as a sign of quality. If any results are more specific
 * than the others, only the specific values are retained, the rest are collapsed. As an example, a
 * method that returns a string will examining two paths, one path returned an ApexStringValue, the
 * other an ApexSingleValue. The ApexStringValue will be retained and the path that returned the
 * ApexSingleValue will be collapsed. NOTE: This is a blunt instrument that may not be necessary if
 * the other Excluders and Constrainers do a perfect job.
 */
public final class ReturnResultPathCollapser
        implements ApexDynamicPathCollapser, Immutable<ReturnResultPathCollapser> {
    private static final Logger LOGGER = LogManager.getLogger(ReturnResultPathCollapser.class);

    public static ReturnResultPathCollapser getInstance() {
        return ReturnResultPathCollapser.LazyHolder.INSTANCE;
    }

    @Override
    public boolean mightCollapse(MethodVertex method) {
        String type =
                ApexStandardLibraryUtil.getCanonicalName(method.getReturnType())
                        .toLowerCase(Locale.ROOT);

        if (type.equalsIgnoreCase(ASTConstants.TYPE_VOID)) {
            return false;
        }

        return true;
    }

    @Override
    public List<ApexPathCollapseCandidate> collapse(
            MethodVertex method, List<ApexPathCollapseCandidate> candidates) {
        // ApexSingleValue is a default which is the least specific. If there are more specific
        // values, then those
        // values should be preferred
        List<ApexPathCollapseCandidate> determinantNonApexSingleValue =
                candidates.stream()
                        .filter(
                                c -> {
                                    ApexValue<?> returnValue = c.getReturnValue().orElse(null);
                                    return returnValue != null
                                            && returnValue.isDeterminant()
                                            && !(returnValue instanceof ApexSingleValue);
                                })
                        .collect(Collectors.toList());
        List<ApexPathCollapseCandidate> resolvedResults =
                candidates.stream()
                        .filter(
                                c -> {
                                    ApexValue<?> returnValue = c.getReturnValue().orElse(null);
                                    return returnValue != null && returnValue.isDeterminant();
                                })
                        .collect(Collectors.toList());
        List<ApexPathCollapseCandidate> unResolvedResults =
                candidates.stream()
                        .filter(
                                c -> {
                                    ApexValue<?> returnValue = c.getReturnValue().orElse(null);
                                    return returnValue == null || !returnValue.isDeterminant();
                                })
                        .collect(Collectors.toList());

        if (!determinantNonApexSingleValue.isEmpty()) {
            if (determinantNonApexSingleValue.size() != candidates.size()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "determinant="
                                    + determinantNonApexSingleValue.stream()
                                            .map(c -> c.getReturnValue().orElse(null))
                                            .collect(Collectors.toList())
                                    + ", resolvedResults="
                                    + resolvedResults.stream()
                                            .map(c -> c.getReturnValue().orElse(null))
                                            .collect(Collectors.toList())
                                    + ", unResolvedResults="
                                    + unResolvedResults.stream()
                                            .map(c -> c.getReturnValue().orElse(null))
                                            .collect(Collectors.toList()));
                }
            }
            return determinantNonApexSingleValue;
        } else if (!resolvedResults.isEmpty()) {
            if (resolvedResults.size() != candidates.size()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "resolvedResults="
                                    + resolvedResults.stream()
                                            .map(c -> c.getReturnValue().orElse(null))
                                            .collect(Collectors.toList())
                                    + ", unResolvedResults="
                                    + unResolvedResults.stream()
                                            .map(c -> c.getReturnValue().orElse(null))
                                            .collect(Collectors.toList()));
                }
            }
            // Use any resolved results if they exist. If not use all of the unresolved results
            return resolvedResults;
        } else {
            // TODO: Should we just pick one of them? Maybe allow the user to choose?
            return unResolvedResults;
        }
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final ReturnResultPathCollapser INSTANCE = new ReturnResultPathCollapser();
    }

    private ReturnResultPathCollapser() {}
}

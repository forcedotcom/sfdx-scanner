package com.salesforce.graph.vertex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StandardConditionVertex extends BaseSFVertex {
    private static final Logger LOGGER = LogManager.getLogger(StandardConditionVertex.class);
    private static final String NEEDS_CONVERSION_MESSAGE =
            "UnknownStandardConditionVertex needs to be converted. vertex=";

    public enum ConditionType {
        NEGATIVE,
        POSITIVE,
        UNKNOWN
    }

    private final ConditionType conditionType;
    private final int hash;

    private StandardConditionVertex(Map<Object, Object> properties, ConditionType conditionType) {
        super(properties);
        this.conditionType = conditionType;
        this.hash = Objects.hash(super.hashCode(), this.conditionType);
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{"
                + "conditionType="
                + conditionType
                + ", properties="
                + properties
                + '}';
    }

    /**
     * Include the base vertex equality of id plus include the type that it was converted to in the
     * path.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StandardConditionVertex that = (StandardConditionVertex) o;
        return conditionType == that.conditionType;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public static final class Negative extends StandardConditionVertex {
        private Negative(Map<Object, Object> properties) {
            super(properties, ConditionType.NEGATIVE);
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
    }

    public static final class Positive extends StandardConditionVertex {
        private Positive(Map<Object, Object> properties) {
            super(properties, ConditionType.POSITIVE);
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
    }

    public static final class Unknown extends StandardConditionVertex {
        private Unknown(Map<Object, Object> properties) {
            super(properties, ConditionType.UNKNOWN);
        }

        //        /** @description Allocations settings object.*/
        //        private Allocations_Settings__c alloSettings =
        // UTIL_CustomSettingsFacade.getAllocationsSettings();
        //
        //        /** @description Which opportunity types to exclude. We need to split this into a
        // set
        //         * to avoid a partial match, e.g. Donor could be matched to the string Matching
        // Donor*/
        //        private Set<String> oppTypesToExclude =
        // String.isBlank(alloSettings.Excluded_Opp_Types__c) ? new Set<String>() : new
        // Set<String>(alloSettings.Excluded_Opp_Types__c.split(';'));

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            // These can be visited in ternary cases
            // i.e. return templateList.isEmpty() ? false : true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(NEEDS_CONVERSION_MESSAGE + this);
            }
            return false;
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            // These can be visited in ternary cases
            // i.e. return templateList.isEmpty() ? false : true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(NEEDS_CONVERSION_MESSAGE + this);
            }
            return false;
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            // These can be visited in ternary cases
            // i.e. return templateList.isEmpty() ? false : true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(NEEDS_CONVERSION_MESSAGE + this);
            }
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            // These can be visited in ternary cases
            // i.e. return templateList.isEmpty() ? false : true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(NEEDS_CONVERSION_MESSAGE + this);
            }
        }

        public Negative convertToNegative() {
            return SFVertexFactory.load(g(), getId(), ConditionType.NEGATIVE);
        }

        public Positive convertToPositive() {
            return SFVertexFactory.load(g(), getId(), ConditionType.POSITIVE);
        }
    }

    public static final class Builder {
        // Type returned when parsing in a non-path context
        public static StandardConditionVertex create(Map<Object, Object> vertex) {
            return new Unknown(vertex);
        }

        // Use extra info to create the correct vertex type
        public static StandardConditionVertex create(
                Map<Object, Object> vertex, Object conditionType) {
            if (conditionType == ConditionType.NEGATIVE) {
                return new Negative(vertex);
            } else if (conditionType == ConditionType.POSITIVE) {
                return new Positive(vertex);
            } else {
                throw new UnexpectedException(conditionType);
            }
        }
    }
}

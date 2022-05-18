package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneableApexValue;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.ObjectPropertiesUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.symbols.DeepCloneContextProvider;
import com.salesforce.graph.symbols.ObjectProperties;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Represents an object returned from a SOQL query. This class can be a single item or list
 * depending on the context.
 *
 * <p>{@code Account a = [SELECT Name from Account]; } results in a single ApexSoqlValue
 *
 * <p>{@code List<Account> accounts = [SELECT Name from Account]; } results in an ApexValueList with
 * a single ApexSoqlValue item
 */
// TODO: Refactor common code between ApexSingleValue and ApexSoqlValue into a common base class
public final class ApexSoqlValue extends AbstractSanitizableValue<ApexSoqlValue>
        implements ComplexAssignable, ObjectProperties, DeepCloneableApexValue<ApexSoqlValue> {
    private static final String METHOD_PUT = "put";

    /**
     * We want to parse the query and extract fields. Fields accessed outside of query would be
     * tracked by ObjectPropertiesHolder
     *
     * <p>Account a = [SELECT Name, Phone FROM Account]; // tracked by queryInfoList a.Description =
     * '415-555-1212'; // tracked by objectProperties
     */
    private ObjectPropertiesHolder objectProperties;

    private HashSet<SoqlQueryInfo> queryInfos = new HashSet<>();

    ApexSoqlValue(ApexValueBuilder builder) {
        super(builder);
        setValue(builder.getValueVertex(), builder.getSymbolProvider());
    }

    ApexSoqlValue(ApexSoqlValue other) {
        this(other, other.getReturnedFrom().orElse(null), other.getInvocable().orElse(null));
    }

    public ApexSoqlValue(
            ApexSoqlValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.objectProperties = CloneUtil.clone(other.objectProperties);
        this.queryInfos = CloneUtil.cloneHashSet(other.queryInfos);
    }

    @Override
    public ApexSoqlValue deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new ApexSoqlValue(this));
    }

    @Override
    public ApexSoqlValue deepCloneForReturn(
            @Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable) {
        return new ApexSoqlValue(this, returnedFrom, invocable);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public void setValue(@Nullable ChainedVertex valueVertex, SymbolProvider symbolProvider) {
        super.setValueVertex(valueVertex, symbolProvider);
        setObjectProperties(symbolProvider);
    }

    @Override
    public void putConstrainedApexValue(ApexValue<?> key, ApexValue<?> value) {
        if (getApexValue(key).isPresent()) {
            throw new UnexpectedException(key);
        }
        objectProperties.put(key, value);
    }

    @Override
    public void putApexValue(ApexValue<?> key, ApexValue<?> value) {
        objectProperties.put(key, value);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(ApexValue<?> key) {
        return objectProperties.getApexValue(key);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        return objectProperties.getApexValue(key);
    }

    @Override
    public Optional<ApexValue<?>> getOrAddDefault(String key) {
        final Optional<ApexValue<?>> valueOptional = getApexValue(key);
        // TODO: Handle scenarios that correlate the field values to the fields present in the query
        //  This has some challenges since we deal with a set of SoqlQueryInfo rather than a single
        // query.
        if (!valueOptional.isPresent() && this.isIndeterminant()) {
            return Optional.of(
                    ObjectPropertiesUtil.getDefaultIndeterminantValue(key, objectProperties));
        }
        return valueOptional;
    }

    /**
     * Initializes a new ObjectPropertiesHolder object with the type and values contained in the
     * value vertex
     */
    private void setObjectProperties(SymbolProvider symbols) {
        this.objectProperties = new ObjectPropertiesHolder();
        if (valueVertex instanceof SoqlExpressionVertex) {
            this.queryInfos = ((SoqlExpressionVertex) valueVertex).getQueryInfo();
        } else {
            ApexValue<?> apexValue =
                    ScopeUtil.resolveToApexValue(symbols, valueVertex).orElse(null);
            if (apexValue instanceof ApexStringValue && apexValue.isValuePresent()) {
                final String query = ((ApexStringValue) apexValue).getValue().get();
                this.queryInfos = SoqlParserUtil.parseQuery(query);
            }
        }
    }

    public HashSet<SoqlQueryInfo> getProcessedQueries() {
        return this.queryInfos;
    }

    @Override
    public void assign(ChainedVertex lhs, ChainedVertex rhs, SymbolProvider symbols) {
        putApexValue(lhs, rhs, symbols);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        String methodName = vertex.getMethodName();

        if (METHOD_PUT.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 2);
            List<ChainedVertex> parameters = vertex.getParameters();
            ApexValue<?> key = ScopeUtil.resolveToApexValueOrBuild(builder, parameters.get(0));
            ApexValue<?> value = ScopeUtil.resolveToApexValueOrBuild(builder, parameters.get(1));
            putApexValue(key, value);
        } else if (ApexListValue.METHOD_IS_EMPTY.equalsIgnoreCase(methodName)) {
            // TODO: This should be more advanced. If a user has manually added something, then it
            // should return true
            return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildBoolean());
        } else if (ApexListValue.METHOD_SIZE.equalsIgnoreCase(methodName)) {
            return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildInteger());
        }
        // TODO: Does this return the previous value?
        return Optional.empty();
    }

    /**
     * Handles #put methods such as
     *
     * <p>Account a = new Account(); a.put('Name', 'Acme. Inc.');
     *
     * <p>or
     *
     * <p>a.Name = 'Acme Inc.';
     */
    private ApexValue<?> putApexValue(
            ChainedVertex key, ChainedVertex value, SymbolProvider symbols) {
        return this.objectProperties.putApexValue(this, key, value, symbols);
    }

    @Override
    public Map<ApexValue<?>, ApexValue<?>> getApexValueProperties() {
        return this.objectProperties.getApexValueProperties();
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.of(SoqlParserUtil.getObjectName(this.queryInfos));
    }
}

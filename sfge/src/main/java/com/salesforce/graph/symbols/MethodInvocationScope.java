package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A scope used while parsing a method invocation. This scope has limited visibility only into the
 * parameters passed to the method.
 */
public final class MethodInvocationScope extends AbstractDefaultNoOpScope
        implements DeepCloneable<MethodInvocationScope> {
    /** Keep track of where the invocation occurred for debugging purposes. */
    private final InvocableVertex invocable;

    /**
     * Map of parameter name to ObjectProperties that were passed to the method. Any objects passed
     * in are mutable.
     */
    private final TreeMap<String, Pair<Typeable, ApexValue<?>>> apexValues;

    /** The ApexValue that was returned via a ReturnStatementVertex */
    private ApexValue<?> returnedApexValue;

    public MethodInvocationScope(
            @Nullable InvocableVertex invocable,
            TreeMap<String, Pair<Typeable, ApexValue<?>>> apexValues) {
        this.invocable = invocable;
        this.apexValues = apexValues;
    }

    private MethodInvocationScope(MethodInvocationScope other) {
        this.invocable = other.invocable;
        this.apexValues = CloneUtil.cloneTreeMap(other.apexValues);
        this.returnedApexValue = CloneUtil.cloneApexValue(other.returnedApexValue);
    }

    @Override
    public MethodInvocationScope deepClone() {
        return new MethodInvocationScope(this);
    }

    /** Set the return value for the method that corresponds to this invocation */
    public void setReturnValue(ApexValue<?> apexValue) {
        // TODO: Use Guava assertion libraries?
        if (this.returnedApexValue != null) {
            // This should only be set once
            throw new UnexpectedException(this.returnedApexValue);
        }
        // Save the value without cloning. This instance should be shared between the callee and
        // caller, for instance
        // singleton values.
        this.returnedApexValue = apexValue;
    }

    /** @return Any values that were returned from the method invocation. */
    public Optional<ApexValue<?>> getReturnValue() {
        return Optional.ofNullable(returnedApexValue);
    }

    @Override
    public Optional<ChainedVertex> getValue(String key) {
        Pair<Typeable, ApexValue<?>> apexPair = apexValues.get(key);
        if (apexPair != null) {
            ApexValue apexValue = apexPair.getRight();
            return apexValue != null ? apexValue.getValueVertex() : Optional.empty();
        } else {
            return super.getValue(key);
        }
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String symbolicName) {
        Optional<ApexValue<?>> rhsValue = super.getApexValue(symbolicName);

        if (rhsValue.isPresent()) {
            return rhsValue;
        }

        if (this.apexValues.containsKey(symbolicName)) {
            return Optional.ofNullable(this.apexValues.get(symbolicName).getRight());
        }

        return Optional.empty();
    }

    @Override
    public Optional<Typeable> getTypedVertex(String key) {
        if (apexValues.containsKey(key)) {
            return Optional.ofNullable(apexValues.get(key).getLeft());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public ApexValue<?> updateVariable(
            String key, ApexValue<?> value, SymbolProvider currentScope) {
        if (apexValues.containsKey(key)) {
            Pair<Typeable, ApexValue<?>> previous = apexValues.get(key);
            apexValues.put(key, Pair.of(previous.getLeft(), value)).getRight();
            return previous.getRight();
        } else {
            throw new UnexpectedException("Undefined variable. key=" + key + ", value=" + value);
        }
    }

    @Override
    public String toString() {
        return "MethodInvocationScope{"
                + "invocableWithParameters="
                + invocable
                + ", apexValues="
                + apexValues
                + ", returnedApexValue="
                + returnedApexValue
                + "} "
                + super.toString();
    }
}

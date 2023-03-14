package com.salesforce.graph.symbols.apex;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.collections.NonNullHashMap;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.ExpressionType;
import com.salesforce.graph.vertex.KeyVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewKeyValueObjectExpressionVertex;
import com.salesforce.graph.vertex.TernaryExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Variables can have values assigned to them in many ways. This class encapsulates those
 * properties.
 *
 * <p>TODO: the current code is tracking this for classes also, this isn't harmful but it might
 * waste resources.
 *
 * <p>Some examples of setting the 'Name' field on Account
 *
 * <p>Account a = new Account(Name = 'Acme Inc.');
 *
 * <p>Account a = new Account(); a.Name = 'Acme Inc.';
 *
 * <p>Account a = new Account(); a.put('Name', 'Acme Inc.');
 *
 * <p>Account a = new Account(); String fieldName = 'Name'; a.put(fieldName, 'Acme Inc.');
 *
 * <p>SObject obj = Schema.getGlobalDescribe().get('Account').newSObject(); obj.put('Name', 'Acme
 * Inc.');
 */
public final class ObjectPropertiesHolder
        implements com.salesforce.graph.DeepCloneable<ObjectPropertiesHolder> {
    /** TODO: Ternary expressions don't have the name of the key. Use this + child index */
    public static final String TODO_TERNARY_KEY_VALUE_PREFIX = "TODO_SFGE_Ternary_";

    private static final Logger LOGGER = LogManager.getLogger(ObjectPropertiesHolder.class);
    private final NonNullHashMap<ApexValue<?>, ApexValue<?>> apexValueProperties;
    /**
     * Users can store and retrieve fields in case insensitive ways. This currently handles the case
     * of a user retrieving a field using a different case than when it was stored. But it doesn't
     * handle the case of putting multiple times with a different case. The fix for that is probably
     * a case-insensitive ApexStringValue
     */
    private TreeMap<String, ApexValue<?>> caseInsensitiveKeyToOriginalKey;

    public ObjectPropertiesHolder() {
        this.apexValueProperties = CollectionUtil.newNonNullHashMap();
        this.caseInsensitiveKeyToOriginalKey = CollectionUtil.newTreeMap();
    }

    private ObjectPropertiesHolder(ObjectPropertiesHolder other) {
        this.apexValueProperties = CloneUtil.cloneNonNullHashMap(other.apexValueProperties);
        this.caseInsensitiveKeyToOriginalKey =
                CloneUtil.cloneTreeMap(other.caseInsensitiveKeyToOriginalKey);
    }

    @Override
    public ObjectPropertiesHolder deepClone() {
        return new ObjectPropertiesHolder(this);
    }

    /**
     * Clears all values and initializes them with the value of the vertex if possible
     *
     * @param owner used to ensure that {@code vertex} isn't a circular reference
     */
    void setValues(
            ApexValue<?> owner, @Nullable ChainedVertex vertex, SymbolProvider symbolProvider) {
        this.apexValueProperties.clear();
        this.caseInsensitiveKeyToOriginalKey.clear();
        if (vertex instanceof NewKeyValueObjectExpressionVertex) {
            NewKeyValueObjectExpressionVertex newKeyValueObjectExpression =
                    (NewKeyValueObjectExpressionVertex) vertex;
            List<ChainedVertex> items = newKeyValueObjectExpression.getItems();
            for (ChainedVertex item : items) {
                ChainedVertex key = item;
                ChainedVertex value = item;
                putApexValue(owner, key, value, symbolProvider);
            }
        }
    }

    /**
     * Converts the {@code key} and {@code value} to ApexValues and stores them in the map.
     *
     * @param owner used to ensure that the key and value aren't circular references
     * @return the previous value if one existed
     */
    ApexValue<?> putApexValue(
            ApexValue<?> owner, ChainedVertex key, ChainedVertex value, SymbolProvider symbols) {
        final ApexValue<?> lhsValue;
        final ApexValue<?> rhsValue;
        final ApexValueBuilder builder = ApexValueBuilder.get(symbols);

        if (key instanceof LiteralExpressionVertex || key instanceof VariableExpressionVertex) {
            KeyVertex keyVertex = (KeyVertex) key;
            String keyName = keyVertex.getKeyName().orElse(null);
            if (keyName != null) {
                lhsValue = builder.deepClone().buildString(keyName);
            } else {
                // This occurs if the key is an indeterminant value which can't be resolved to a
                // string
                lhsValue = builder.deepClone().valueVertex(key).buildUnknownType();
            }
            if (ExpressionType.KEY_VALUE.equals(keyVertex.getExpressionType())) {
                if (key instanceof LiteralExpressionVertex) {
                    LiteralExpressionVertex literalExpression = (LiteralExpressionVertex) key;
                    if (key instanceof LiteralExpressionVertex.Null) {
                        rhsValue = builder.valueVertex(key).buildUnknownType();
                    } else {
                        rhsValue = builder.buildString(literalExpression.getLiteralAsString());
                    }
                } else if (key instanceof VariableExpressionVertex) {
                    VariableExpressionVertex variableExpression = (VariableExpressionVertex) key;
                    rhsValue = builder.buildString(variableExpression.getName());
                } else {
                    throw new UnexpectedException("Impossible");
                }
            } else {
                rhsValue = ScopeUtil.resolveToApexValueOrBuild(builder, value);
            }
        } else if (key instanceof MethodCallExpressionVertex) {
            MethodCallExpressionVertex methodCallExpression = (MethodCallExpressionVertex) key;
            lhsValue = builder.deepClone().buildString(methodCallExpression.getKeyName().get());
            rhsValue = ScopeUtil.resolveToApexValueOrBuild(builder, value);
        } else if (key instanceof TernaryExpressionVertex) {
            lhsValue =
                    builder.deepClone()
                            .buildString(TODO_TERNARY_KEY_VALUE_PREFIX + key.getChildIndex());
            rhsValue = ScopeUtil.resolveToApexValueOrBuild(builder, value);
        } else {
            lhsValue = ScopeUtil.resolveToApexValueOrBuild(builder, key);
            rhsValue = ScopeUtil.resolveToApexValueOrBuild(builder, value);
        }

        ApexValueUtil.assertNotCircular(owner, lhsValue, null);
        ApexValueUtil.assertNotCircular(owner, rhsValue, null);
        return put(lhsValue, rhsValue);
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters") // Non trivial fix
    public ApexValue<?> put(ApexValue<?> key, ApexValue<?> value) {
        if (key.isNull()) {
            /**
             * Throwing this exception will cause invalid paths to be excluded. For example, the
             * following code has two paths. One path is invalid because keyName is null when #put
             * is called. This path would result in a NullPointerException at runtime.
             *
             * <p>SObject sObj = new Account(); String keyName = null; for (some iteration) { if
             * (some comparison) { keyName = 'field1'; } } sObj.put(keyName, 'value');
             */
            throw new NullValueAccessedException(key, null);
        }

        String stringKey = getKeyForCaseInsensitiveMap(key).orElse(null);
        if (stringKey != null) {
            if (caseInsensitiveKeyToOriginalKey.containsKey(stringKey)) {
                // We've already seen this stringKey before. Update the corresponding ApexValue to
                // the value we already know about.
                // This resolves issues from case insensitivity such as:
                // Account.name = 'Value1'
                // Account.Name = 'Value2'
                // Both "name" and "Name" would translate to "Value2"
                // TODO: Since we are replacing the key, are we creating new trouble?
                key = caseInsensitiveKeyToOriginalKey.get(stringKey);
            } else {
                caseInsensitiveKeyToOriginalKey.put(stringKey, key);
            }
        }

        return apexValueProperties.put(key, value);
    }

    Map<ApexValue<?>, ApexValue<?>> getApexValueProperties() {
        return Collections.unmodifiableMap(apexValueProperties);
    }

    Optional<ApexValue<?>> getApexValue(ApexValue<?> key) {
        return Optional.ofNullable(getApexValueProperties().get(key));
    }

    Optional<ApexValue<?>> getApexValue(String key) {
        ApexStringValue apexStringValue =
                ApexValueBuilder.getWithoutSymbolProvider().buildString(key);
        Optional<ApexValue<?>> result = getApexValue(apexStringValue);
        if (!result.isPresent()) {
            ApexValue<?> originalKey = caseInsensitiveKeyToOriginalKey.get(key);
            if (originalKey != null) {
                result = Optional.ofNullable(getApexValueProperties().get(originalKey));
            }
        }
        return result;
    }

    /**
     * Convert the value to a string that can be stored in {@link #caseInsensitiveKeyToOriginalKey}.
     * This will be used to look up values if the user requests the key using a different case.
     */
    private Optional<String> getKeyForCaseInsensitiveMap(ApexValue<?> apexValue) {
        ChainedVertex valueVertex = apexValue.getValueVertex().orElse(null);
        if (valueVertex instanceof KeyVertex) {
            return ((KeyVertex) valueVertex).getKeyName();
        } else if (apexValue instanceof ApexStringValue) {
            ApexStringValue apexStringValue = (ApexStringValue) apexValue;
            return apexStringValue.getValue();
        } else if (apexValue instanceof ApexSingleValue) {
            ChainedVertex chainedVertex = apexValue.getValueVertex().orElse(null);
            if (chainedVertex instanceof KeyVertex) {
                return ((KeyVertex) chainedVertex).getKeyName();
            }
        }
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Unable to determine key for value=" + apexValue);
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectPropertiesHolder that = (ObjectPropertiesHolder) o;
        return Objects.equals(apexValueProperties, that.apexValueProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apexValueProperties);
    }
}

package com.salesforce.apex;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * POJO representation of an enum after it is retrieved from the graph. This is used for both
 * StandardApexLibrary and User defined enums.
 */
public final class ApexEnum {
    /** Name such as "DisplayType" */
    private final String name;

    /**
     * The list of values in the order that they are declared in the source code. Order is important
     * since one of the methods is #ordinal.
     */
    private final List<Value> values;

    /** Case insensitive mapping of value to the the original value and ordinal */
    private final TreeMap<String, Value> valueMap;

    public ApexEnum(String name, List<String> values) {
        this.name = name;
        this.values =
                CollectionUtil.newImmutableListOf(
                        values, (index, value) -> new Value(index, value));
        this.valueMap = CollectionUtil.newTreeMapOf(this.values, Value::getValueName);
    }

    public String getName() {
        return name;
    }

    public List<Value> getValues() {
        return values;
    }

    public Value getValue(String valueName) {
        final Value value = valueMap.get(valueName);
        if (value == null) {
            throw new UnexpectedException("Invalid value. value=" + valueName);
        }
        return value;
    }

    public boolean isValid(String valueName) {
        return valueMap.containsKey(valueName);
    }

    public boolean matchesType(String inputType) {
        return this.name.equalsIgnoreCase(inputType);
    }

    // IMPORTANT: This was modified to exclude valueMap. valueMap is derived from values
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexEnum apexEnum = (ApexEnum) o;
        return Objects.equals(name, apexEnum.name) && Objects.equals(values, apexEnum.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, values);
    }

    public static class Value {
        /** The index order in which this value was declared */
        private final int ordinal;
        /** The value, such as "ADDRESS" for DisplayType.ADDRESS */
        private final String valueName;

        private Value(int ordinal, String valueName) {
            this.ordinal = ordinal;
            this.valueName = valueName;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public String getValueName() {
            return valueName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Value value1 = (Value) o;
            return ordinal == value1.ordinal && Objects.equals(valueName, value1.valueName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ordinal, valueName);
        }
    }
}

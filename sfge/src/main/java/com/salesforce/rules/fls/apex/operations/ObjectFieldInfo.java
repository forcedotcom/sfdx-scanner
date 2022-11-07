package com.salesforce.rules.fls.apex.operations;

import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import java.util.Optional;
import java.util.TreeSet;

/**
 * POJO to hold basics of a soql/dml operation: 1. The Object that an operation is performed on 2.
 * Fields on the Object that the operation works on
 */
public abstract class ObjectFieldInfo<T extends ObjectFieldInfo>
        implements DeepCloneable<ObjectFieldInfo> {
    protected final String objectName;
    protected final TreeSet<String> fields;
    protected final boolean allFields;

    public ObjectFieldInfo(String objectName, TreeSet<String> fields, boolean allFields) {
        this.objectName = objectName;
        this.fields = fields;
        this.allFields = allFields;
    }

    protected ObjectFieldInfo(ObjectFieldInfo other) {
        this.objectName = other.objectName;
        this.fields = CloneUtil.cloneTreeSet(other.fields);
        this.allFields = other.allFields;
    }

    /**
     * @return object name such as Account which is being operated on
     */
    public String getObjectName() {
        return objectName;
    }

    public TreeSet<String> getFields() {
        return fields;
    }

    public boolean isAllFields() {
        return allFields;
    }

    /**
     * Merge two objects that extend {@link ObjectFieldInfo} based on the object names
     *
     * @return merged value of an object that extends {@link ObjectFieldInfo}
     */
    public Optional<T> merge(T other) {
        if (canMerge(other)) {
            return Optional.of(_merge(other));
        }

        return Optional.empty();
    }

    /**
     * @param other instance of {@link ObjectFieldInfo} that we want to merge with this instance
     * @return true if the other instance fits all the conditions that allow it to be merged.
     */
    public boolean canMerge(T other) {
        return this.objectName.equalsIgnoreCase(other.objectName) && _canMerge(other);
    }

    /**
     * Matches merge criteria specific to implementation
     *
     * @param other instance of {@link ObjectFieldInfo} that we want to merge with this instance
     * @return true if the other instance fits all the conditions that allow it to be merged.
     */
    protected abstract boolean _canMerge(T other);

    /** Handles details of merging an ObjectFieldInfo into another */
    protected abstract T _merge(T other);
}

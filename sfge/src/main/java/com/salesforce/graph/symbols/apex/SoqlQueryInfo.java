package com.salesforce.graph.symbols.apex;

import com.google.common.base.Objects;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.fls.apex.operations.ObjectFieldInfo;
import java.util.TreeSet;

/** Represents information within a Soql query. Use this POJO through SoqlParserUtil.java */
public class SoqlQueryInfo extends ObjectFieldInfo<SoqlQueryInfo> {
    private final String queryStr; // Only for debugging purposes
    private final boolean count;
    private final boolean limit1;
    private boolean securityEnforced;
    private final boolean outermost;

    public SoqlQueryInfo(
            String queryStr,
            String objectName,
            TreeSet<String> fields,
            boolean isAllFields,
            boolean isCount,
            boolean isLimit1,
            boolean isSecurityEnforced,
            boolean isOutermost) {
        super(objectName, fields, isAllFields);
        this.queryStr = queryStr;
        this.count = isCount;
        this.limit1 = isLimit1;
        this.securityEnforced = isSecurityEnforced;
        this.outermost = isOutermost;
    }

    SoqlQueryInfo(SoqlQueryInfo other) {
        super(other);
        this.queryStr = other.queryStr;
        this.count = other.count;
        this.limit1 = other.limit1;
        this.securityEnforced = other.securityEnforced;
        this.outermost = other.outermost;
    }

    /**
     * @return all the fields used in the query including the fields that were queried, fields that
     *     were invoked in WHERE clause or GROUP BY clause.
     */
    public TreeSet<String> getFields() {
        // Return a copy of the original field list so that the fields
        // aren't externally modifiable.
        return CollectionUtil.newTreeSetOf(fields);
    }

    @Override
    protected SoqlQueryInfo _merge(SoqlQueryInfo other) {
        return new SoqlQueryInfo(
                this.queryStr
                        + ","
                        + other.queryStr, // since this is just for logging information, we are
                // storing both the origin queries
                this.objectName,
                CollectionUtil.newTreeSetOf(this.fields, other.fields),
                this.allFields
                        || other.allFields, // If one of them is allFields, the check needs to be on
                // all fields
                this.count || other.count,
                this.limit1
                        && other.limit1
                        && (this.outermost
                                || other.outermost), // Limit 1 value is governed by the outermost
                // query
                this.securityEnforced
                        && other.securityEnforced, // Both portions need to be security enforced
                this.outermost || other.outermost // Either can be outermost
                );
    }

    /** @return true if the query uses fields(all), fields(standard), or fields(custom) */
    public boolean isAllFields() {
        return allFields;
    }

    /** @return true if any of the fields in the query require access check */
    public boolean getFieldsRequireAccessCheck() {
        final TreeSet<String> myFields = getFields();
        myFields.removeAll(SoqlParserUtil.ALWAYS_ACCESSIBLE_FIELDS);
        return !myFields.isEmpty() || isAllFields();
    }

    /**
     * @param other instance of {@link ObjectFieldInfo} that we want to merge with this instance
     * @return true always since {@link SoqlQueryInfo} does not have criteria other than those
     *     covered by in {@link ObjectFieldInfo#merge(ObjectFieldInfo)}.
     */
    @Override
    protected boolean _canMerge(SoqlQueryInfo other) {
        return true;
    }

    /** @return true if the query uses a COUNT() phrase. */
    public boolean isCount() {
        return count;
    }

    /**
     * @return true if query has a LIMIT 1 phrase, which means this query would return only a single
     *     record and doesn't need a List variable to capture the value.
     */
    public boolean isLimit1() {
        return limit1;
    }

    /**
     * @return true if the query has a WITH SECURITY_ENFORCED clause to filter out fields that are
     *     not accessible. This inherently brings in FLS validation.
     */
    public boolean isSecurityEnforced() {
        return securityEnforced;
    }

    /**
     * @return true if the query information is the outermost in a complex query with one or more
     *     sub queries.
     */
    public boolean isOutermost() {
        return outermost;
    }

    @Override
    public SoqlQueryInfo deepClone() {
        return new SoqlQueryInfo(this);
    }

    /**
     * NOTE: equals() and hashcode() don't use queryStr field since it is only for debugging
     * purposes. However, there's no manual modification in how these two methods were generated.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoqlQueryInfo)) return false;
        SoqlQueryInfo queryInfo = (SoqlQueryInfo) o;
        return allFields == queryInfo.allFields
                && count == queryInfo.count
                && limit1 == queryInfo.limit1
                && securityEnforced == queryInfo.securityEnforced
                && outermost == queryInfo.outermost
                && Objects.equal(objectName, queryInfo.objectName)
                && Objects.equal(fields, queryInfo.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                objectName, fields, allFields, count, limit1, securityEnforced, outermost);
    }

    @Override
    public String toString() {
        return "SoqlQueryInfo{"
                + "queryStr='"
                + queryStr
                + '\''
                + ", objectName='"
                + objectName
                + '\''
                + ", fields="
                + fields
                + ", isAllFields="
                + allFields
                + ", isCount="
                + count
                + ", isLimit1="
                + limit1
                + ", isSecurityEnforced="
                + securityEnforced
                + ", isOutermost="
                + outermost
                + '}';
    }
}

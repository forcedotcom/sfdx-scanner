package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;

/**
 * Represents the status of an ApexValue. The status is dependent upon how much information we have
 * accumulated about an ApexValue and which actions have been applied to the ApexValue.
 *
 * <p>The status of a Standard ApexValue is unique. A Standard ApexValue is an ApexValue that
 * represents an ApexClass which is stubbed out such as {@link DescribeSObjectResult} or {@link
 * SObjectType}. These objects are considered {@link #INITIALIZED} if they were returned from a call
 * such as "Account.SObjectType.getDescribe()". These same ApexValues are {@link #INDETERMINANT} if
 * they are method parameters for the first method in a path. Standard ApexValues returned from
 * other Standard ApexValues are always {@link #INITIALIZED}, even if the original value was {@link
 * #INDETERMINANT}. {@link ApexSimpleValue}'s returned from Standards ApexValues are almost always
 * {@link #INDETERMINANT} except in a few special cases such as {@link
 * DescribeFieldResult#getFieldName()} where that value might be known based on how the
 * DescribeFieldResult was acquired.
 */
public enum ValueStatus {
    /**
     * Indicates that this object's state can't be known. This is typically because of one of the
     * following reasons.
     *
     * <ul>
     *   <li>The value is a parameter passed into a method, where that method is the starting point
     *       of a path
     *   <li>It is a value returned from another indeterminant value. For instance String#length on
     *       an indeterminant ApexStringValue will return an indeterminant Integer.
     *   <li>A variable is assigned the value from a method which we can't determine the return
     *       value. This can happen if the method is a System method or in a class where we don't
     *       have the source.
     * </ul>
     *
     * ApexValues in this state tend to return other ApexValues with an INDETERMINANT state. For
     * instance, String#length will return and indeterminant Integer if it itself is INDETERMINANT.
     */
    INDETERMINANT,

    /**
     * Represents a value that has had a value explicitly set to and the value value is resolved.
     * i.e. "String x = 'Hello';" "String x = someMethodWhichIsInTheGraph();"
     */
    INITIALIZED,

    /** A value that was declared but has not been assigned to i.e. "String x;" */
    UNINITIALIZED
}

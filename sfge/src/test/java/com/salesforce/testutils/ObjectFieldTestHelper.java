package com.salesforce.testutils;

import com.salesforce.rules.fls.apex.operations.ObjectFieldInfo;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/** Helps with testing {@link ObjectFieldInfo} and its utilities */
public class ObjectFieldTestHelper {
    private static final Comparator<ObjectFieldInfo> QUERY_INFO_COMPARATOR =
            Comparator.comparing(ObjectFieldInfo::getObjectName);

    /** Sort query information by object name to get a predictable order */
    public static <T extends ObjectFieldInfo> Iterator<T> getSortedIterator(Set<T> queryInfos) {
        TreeSet<T> sortedQueryInfos = new TreeSet<T>(QUERY_INFO_COMPARATOR);
        sortedQueryInfos.addAll(queryInfos);

        return sortedQueryInfos.iterator();
    }
}

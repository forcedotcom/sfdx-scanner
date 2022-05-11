package com.salesforce.graph.ops;

import com.google.common.collect.HashMultimap;
import com.salesforce.rules.fls.apex.operations.ObjectFieldInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

public final class ObjectFieldUtil {
    private ObjectFieldUtil() {}

    /**
     * Consolidate a set of {@link ObjectFieldInfo} by object name so that there are no
     * duplications.
     *
     * @param inputObjectFieldInfos to regroup. Usually from a freshly parsed query.
     * @return a set of {@link ObjectFieldInfo} that have been regrouped.
     */
    public static <T extends ObjectFieldInfo<T>> HashSet<T> regroupByObject(
            HashSet<T> inputObjectFieldInfos) {
        final HashMultimap<String, T> objectGrouping = HashMultimap.create();

        for (T queryInfo : inputObjectFieldInfos) {
            final String objectName = queryInfo.getObjectName().toLowerCase(Locale.ROOT);
            boolean isMerged = false;
            if (objectGrouping.containsKey(objectName)) {
                final Collection<T> currentQueryInfos = objectGrouping.get(objectName);
                for (Iterator<T> iterator = currentQueryInfos.iterator(); iterator.hasNext(); ) {
                    final T currentQueryInfo = iterator.next();
                    final Optional<T> mergedObject = queryInfo.merge(currentQueryInfo);
                    if (mergedObject.isPresent()) {
                        currentQueryInfos.remove(currentQueryInfo);
                        currentQueryInfos.add(mergedObject.get());
                        isMerged = true;
                        break;
                    }
                }
                if (!isMerged) {
                    objectGrouping.put(objectName, queryInfo);
                }
            } else {
                objectGrouping.put(objectName, queryInfo);
            }
        }

        return new HashSet<>(objectGrouping.values());
    }
}

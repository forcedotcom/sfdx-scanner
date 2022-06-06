package com.salesforce.graph.ops;

import com.salesforce.Collectible;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.collections.NonNullArrayList;
import com.salesforce.collections.NonNullHashMap;
import com.salesforce.collections.NonNullHashSet;
import com.salesforce.exception.SfgeInterruptedException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.Immutable;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.symbols.AbstractClassInstanceScope;
import com.salesforce.graph.symbols.ClassInstanceScope;
import com.salesforce.graph.symbols.DeserializedClassInstanceScope;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.BaseSFVertex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Set of utilities used for objects that implement the {@link DeepCloneable} interface. Cloning
 * null objects result in null return values.
 */
@SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
public final class CloneUtil {
    public static <T extends DeepCloneable<T>> T clone(@Nullable T from) {
        if (from == null) {
            return null;
        }

        return from.deepClone();
    }

    public static <T extends Collectible<T> & DeepCloneable<T>> Collectible<T> cloneCollectible(
            @Nullable Collectible<T> from) {
        if (from == null) {
            return null;
        }

        if (from.getCollectible() == null) {
            // This is a singleton, return it as is
            return from;
        } else {
            T t = from.getCollectible();
            return CloneUtil.clone(t);
        }
    }

    /** TODO: Have AbstractClassInstanceScope extend DeepClone in a generic manner */
    public static AbstractClassInstanceScope clone(@Nullable AbstractClassInstanceScope from) {
        if (from == null) {
            return null;
        }

        if (from instanceof ClassInstanceScope) {
            ClassInstanceScope classInstanceScope = (ClassInstanceScope) from;
            return classInstanceScope.deepClone();
        } else if (from instanceof DeserializedClassInstanceScope) {
            DeserializedClassInstanceScope deserializedClassInstanceScope =
                    (DeserializedClassInstanceScope) from;
            return deserializedClassInstanceScope.deepClone();
        } else {
            throw new UnexpectedException(from);
        }
    }

    public static <T extends ApexValue<?>> T cloneApexValue(@Nullable ApexValue<?> from) {
        if (from == null) {
            return null;
        }

        return (T) from.deepClone();
    }

    public static <T extends Immutable<T>> T cloneImmutable(T from) {
        // This is immutable. No need to clone
        return from;
    }

    public static <T> ArrayList<T> cloneArrayList(@Nullable List<T> from) {
        return cloneCollection(from, new ArrayList<>());
    }

    public static <T> LinkedList<T> cloneLinkedList(@Nullable List<T> from) {
        return cloneCollection(from, new LinkedList<>());
    }

    public static <T> NonNullArrayList<T> cloneNonNullArrayList(
            @Nullable NonNullArrayList<T> from) {
        return cloneCollection(from, CollectionUtil.newNonNullArrayList());
    }

    public static TreeSet<String> cloneTreeSet(@Nullable TreeSet<String> from) {
        return cloneCollection(from, CollectionUtil.newTreeSet());
    }

    public static <T> NonNullHashSet<T> cloneNonNullHashSet(@Nullable NonNullHashSet<T> from) {
        return cloneCollection(from, CollectionUtil.newNonNullHashSet());
    }

    public static <T> HashSet<T> cloneHashSet(@Nullable HashSet<T> from) {
        return cloneCollection(from, new HashSet<>());
    }

    public static <T> LinkedHashSet<T> cloneLinkedHashSet(@Nullable LinkedHashSet<T> from) {
        return cloneCollection(from, new LinkedHashSet<>());
    }

    public static <T> TreeMap<String, T> cloneTreeMap(@Nullable TreeMap<String, T> from) {
        return cloneMap(from, CollectionUtil.newTreeMap());
    }

    public static <K, V> HashMap<K, V> cloneHashMap(@Nullable HashMap<K, V> from) {
        return cloneMap(from, new HashMap<>());
    }

    public static <K, V> NonNullHashMap<K, V> cloneNonNullHashMap(
            @Nullable NonNullHashMap<K, V> from) {
        return cloneMap(from, CollectionUtil.newNonNullHashMap());
    }

    public static <K, V> LinkedHashMap<K, V> cloneLinkedHashMap(
            @Nullable LinkedHashMap<K, V> from) {
        return cloneMap(from, new LinkedHashMap<>());
    }

    public static <T> Stack<T> cloneStack(@Nullable Stack<T> from) {
        if (from == null) {
            return null;
        }

        Stack<T> to = new Stack<>();

        for (T item : from) {
            to.push(cloneIfPossible(item));
        }

        return to;
    }

    public static <T> Optional<T> cloneOptional(Optional<T> from) {
        if (from == null) {
            return null;
        }

        if (from.isPresent()) {
            return Optional.of(cloneIfPossible(from.get()));
        } else {
            return Optional.empty();
        }
    }

    private static <T, U extends Collection<T>> U cloneCollection(Collection<T> from, U to) {
        if (from == null) {
            return null;
        }

        for (T item : from) {
            to.add(cloneIfPossible(item));
        }

        return to;
    }

    private static <K, V, T extends Map<K, V>> T cloneMap(Map<K, V> from, T to) {
        if (from == null) {
            return null;
        }

        for (Map.Entry<K, V> entry : from.entrySet()) {
            K key = cloneIfPossible(entry.getKey());
            V value = cloneIfPossible(entry.getValue());
            to.put(key, value);
        }

        return to;
    }

    private static <T> T cloneIfPossible(@Nullable T item) {
        if (Thread.interrupted()) {
            throw new SfgeInterruptedException();
        }

        if (item == null) {
            return null;
        }

        if (item instanceof DeepCloneable) {
            return (T) clone((DeepCloneable) item);
        } else if (item instanceof Collectible<?>) {
            return (T) cloneCollectible((Collectible) item);
        } else if (item instanceof Immutable) {
            return (T) cloneImmutable((Immutable) item);
        } else if (item instanceof ApexValue) {
            return (T) cloneApexValue((ApexValue<?>) item);
        } else if (item instanceof Pair) {
            Object left = ((Pair<?, ?>) item).getLeft();
            Object right = ((Pair<?, ?>) item).getRight();
            return (T) Pair.of(cloneIfPossible(left), cloneIfPossible(right));
        } else if (item instanceof Triple) {
            Object left = ((Triple<?, ?, ?>) item).getLeft();
            Object middle = ((Triple<?, ?, ?>) item).getMiddle();
            Object right = ((Triple) item).getRight();
            return (T)
                    Triple.of(
                            cloneIfPossible(left), cloneIfPossible(middle), cloneIfPossible(right));
        } else if (item instanceof ArrayList) {
            return (T) cloneArrayList((List) item);
        } else if (item instanceof HashMap) {
            return (T) cloneHashMap((HashMap) item);
        } else if (item instanceof LinkedHashMap) {
            return (T) cloneLinkedHashMap((LinkedHashMap) item);
        } else if (item instanceof Stack) {
            return (T) cloneStack((Stack) item);
        } else if (item instanceof TreeMap) {
            return (T) cloneTreeMap((TreeMap) item);
        } else if (item instanceof BaseSFVertex) {
            return item;
		} else if (item instanceof EngineDirective) {
			return item;
        } else if (item instanceof LinkedList) {
            return (T) cloneLinkedList((LinkedList) item);
        } else if (item instanceof Enum) {
            return item;
        } else if (item instanceof Boolean) {
            return item;
        } else if (item instanceof String) {
            return item;
        } else if (item instanceof Long) {
            return item;
        } else if (item instanceof Optional) {
            return (T) cloneOptional((Optional<?>) item);
        } else {
            throw new UnexpectedException(item.getClass().getSimpleName());
        }
    }

    private CloneUtil() {}
}

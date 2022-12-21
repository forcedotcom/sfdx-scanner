package com.salesforce.graph.ops;

import com.google.common.base.Objects;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utilities that help with addressing common Typeable needs */
public final class TypeableUtil {
    public static final int NOT_A_MATCH = -1;

    private static final Logger LOGGER = LogManager.getLogger(TypeableUtil.class);

    private static final String LIST_PATTERN_STR = "list<\\s*([^\\s]*)\\s*>";
    private static final Pattern LIST_PATTERN =
            Pattern.compile(LIST_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    private static final String SET_PATTERN_STR = "set<\\s*([^\\s]*)\\s*>";
    private static final Pattern SET_PATTERN =
            Pattern.compile(SET_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    private TypeableUtil() {}

    /**
     * Provides type hierarchy of a given type. Any type in Apex is a subtype of Object. Any data
     * object in Apex is a subtype of SObject.
     *
     * @return OrderedTreeSet of the provided type's hierarchy. The higher the index, the farther
     *     away is the type in hierarchy.
     */
    public static OrderedTreeSet getTypeHierarchy(String definingType) {
        OrderedTreeSet typeHierarchy = new OrderedTreeSet();

        // If definingType is a list with a subtype, get type hierarchy for subtype.

        // We need to special-handle List since Apex seems to have a different notion of
        // type hierarchy for Lists compared to Maps and Sets.
        // While Lists can automatically cast a List<Account> object to a method that takes
        // List<SObject>,
        // Maps and Sets don't have this capability.

        // Also, this logic can't be housed only in ApexListValue since we sometimes have to
        // determine type hierarchy for
        // ValueDeclarationExpressionVertex (seen so far)
        typeHierarchy.addAll(getListTypeHierarchy(definingType));

        final String canonicalNameOfDefiningType =
                ApexStandardLibraryUtil.getCanonicalName(definingType);

        // Closest match in the type hierarchy is the defining type itself.
        typeHierarchy.add(canonicalNameOfDefiningType);

        // TODO: This hierarchy doesn't handle Class hierarchy, which is handled separately in
        //  AbstractClassScope. How can these be integrated when we are not sure what the subtype
        // is?

        // If we are handling a standard object or a custom object, SObject goes to the type
        // hierarchy
        if (isDataObject(canonicalNameOfDefiningType)) {
            typeHierarchy.add(
                    ApexStandardLibraryUtil.getCanonicalName(
                            ApexStandardLibraryUtil.Type.S_OBJECT));
        }

        // All apex objects are of type Object
        typeHierarchy.add(
                ApexStandardLibraryUtil.getCanonicalName(ApexStandardLibraryUtil.Type.OBJECT));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Type hierarchy identified for " + definingType + ": " + typeHierarchy);
        }
        return typeHierarchy;
    }

    /**
     * Gets hierarchy of types that could match based on the subtype. For example, List<Account>
     * would return back: List<Account>, List<SObject>, List<Object>
     *
     * @param definingType could be a list type or not.
     * @return empty optional for non-list definingType, and hierarchy of list types, otherwise
     */
    private static OrderedTreeSet getListTypeHierarchy(String definingType) {
        final OrderedTreeSet listTypeHierarchy = new OrderedTreeSet();
        Optional<String> subType = getListSubType(definingType);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Identified subtype of " + definingType + " as " + subType);
        }
        if (subType.isPresent()) {
            final OrderedTreeSet typeHierarchy = getTypeHierarchy(subType.get());
            for (String type : typeHierarchy.getAll()) {
                listTypeHierarchy.add(ApexStandardLibraryUtil.getListDeclaration(type));
            }
        }

        return listTypeHierarchy;
    }

    /**
     * Find subtype of list. For example subtype is "Account" in {@code List<Account>}
     *
     * @param definingType any type that may or may not be a list
     * @return Optional.empty() if no subtype could be identified, else Optional of the subType
     *     string
     */
    public static Optional<String> getListSubType(String definingType) {
        return getSubType(LIST_PATTERN, definingType, 1);
    }

    public static Optional<String> getSetSubType(String definingType) {
        return getSubType(SET_PATTERN, definingType, 1);
    }

    private static Optional<String> getSubType(
            Pattern subTypePattern, String definingType, int groupCountExpected) {
        final Matcher subTypeMatcher = subTypePattern.matcher(definingType);

        if (subTypeMatcher.find()) {
            if (subTypeMatcher.groupCount() != groupCountExpected) {
                throw new UnexpectedException(
                        "Expected to find only one Type in declaration: " + definingType);
            }
            return Optional.of(subTypeMatcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * Gathers type information from the first child vertex of a given vertex
     *
     * @return Optional with Typeable value, if found, otherwise, an empty Optional
     */
    public static Optional<Typeable> getSubTypeFromChildVertex(BaseSFVertex vertex) {
        Optional<Typeable> subType = Optional.empty();
        final List<BaseSFVertex> children = vertex.getChildren();
        if (!children.isEmpty()) {
            // Get type of first child, if available
            final BaseSFVertex firstChild = children.get(0);
            if (firstChild instanceof Typeable) {
                subType = Optional.of((Typeable) firstChild);
            }
        }
        return subType;
    }

    /**
     * Identifies if a given type is a DataObject or not. Includes plain SObject, registered
     * Standard Objects, and Custom Objects/settings that end with __c.
     */
    public static boolean isDataObject(String canonicalNameOfDefiningType) {
        return ApexStandardLibraryUtil.isStandardObject(canonicalNameOfDefiningType)
                || isCustomObject(canonicalNameOfDefiningType)
                || MetadataInfoProvider.get().isCustomSetting(canonicalNameOfDefiningType)
                || ApexStandardLibraryUtil.Type.S_OBJECT.equalsIgnoreCase(
                        canonicalNameOfDefiningType);
    }

    /**
     * @return true for custom objects but not for custom settings
     */
    public static boolean isCustomObject(String definingType) {
        return definingType
                        .toLowerCase(Locale.ROOT)
                        .endsWith(ASTConstants.TypeSuffix.SUFFIX_CUSTOM_OBJECT)
                && !isCustomSetting(definingType);
    }

    /**
     * @return true if definingType is found to be a custom setting
     */
    public static boolean isCustomSetting(String definingType) {
        return MetadataInfoProvider.get().isCustomSetting(definingType);
    }

    /**
     * @return true if the type's name ends with __mdt
     */
    public static boolean isMetadataObject(String definingType) {
        return definingType
                .toLowerCase(Locale.ROOT)
                .endsWith(ASTConstants.TypeSuffix.SUFFIX_METADATA_OBJECT);
    }

    /**
     * Ranks a parameter type against an invocableType.
     *
     * @param invocableType the type against which ranking is performed
     * @param parameterType the type which gets ranked
     * @return an integer that represents how close the type of a parameter vertex is compared to
     *     the invocableType. Lower the number, higher the match
     */
    public static int rankParameterMatch(Typeable invocableType, Typeable parameterType) {
        final OrderedTreeSet types = invocableType.getTypes();
        final String parameterTypeString = parameterType.getCanonicalType();
        final int rank = types.getIndex(parameterTypeString);

        if (rank == NOT_A_MATCH) {
            throw new UnexpectedException(
                    "Did not expect NOT_A_MATCH when ranking parameter match. parameterType = "
                            + parameterTypeString
                            + ", type = "
                            + types);
        }
        return rank;
    }

    /** Derive type information for a vertex that's not a typeable */
    public static Optional<String> deriveType(BaseSFVertex vertex) {
        final BaseSFVertex parentVertex = vertex.getParent();
        if (parentVertex instanceof Typeable) {
            return Optional.of(((Typeable) parentVertex).getCanonicalType());
        }
        return Optional.empty();
    }

    /**
     * Data structure that behaves like an ordered TreeSet with case insensitivity. This wraps an
     * ArrayList and adds the behaviors of uniqueness and case insensitivity.
     *
     * <p>TODO: After considering extending ArrayList, adding a wrapper seems like a better design
     * decision based on LSP's rule. If too many methods need to be added with boilerplate
     * indirection, consider moving to extending ArrayList.
     */
    public static final class OrderedTreeSet implements DeepCloneable<OrderedTreeSet> {
        private final ArrayList<String> internalList;

        public OrderedTreeSet() {
            this.internalList = new ArrayList<>();
        }

        OrderedTreeSet(OrderedTreeSet other) {
            this.internalList = CloneUtil.cloneArrayList(other.internalList);
        }

        public boolean contains(String value) {
            for (String item : internalList) {
                if (item.equalsIgnoreCase(value)) {
                    return true;
                }
            }
            return false;
        }

        public void add(String value) {
            // Do not add value if it already exists
            if (!contains(value)) {
                internalList.add(value);
            }
        }

        public String get(int i) {
            return internalList.get(i);
        }

        public int getIndex(String value) {
            for (int i = 0; i < internalList.size(); i++) {
                if (internalList.get(i).equalsIgnoreCase(value)) {
                    return i;
                }
            }
            return NOT_A_MATCH;
        }

        public ArrayList<String> getAll() {
            return internalList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OrderedTreeSet)) return false;
            OrderedTreeSet that = (OrderedTreeSet) o;
            return Objects.equal(internalList, that.internalList);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(internalList);
        }

        @Override
        public String toString() {
            return "OrderedTreeSet{" + "internalList=" + internalList + '}';
        }

        // TODO: Object should always be the last item. Having it earlier in the ranking would cause
        // bugs.
        public void addAll(ArrayList<String> values) {
            internalList.addAll(values);
        }

        public void addAll(OrderedTreeSet other) {
            addAll(other.getAll());
        }

        public boolean isEmpty() {
            return internalList.isEmpty();
        }

        @Override
        public OrderedTreeSet deepClone() {
            return new OrderedTreeSet(this);
        }
    }
}

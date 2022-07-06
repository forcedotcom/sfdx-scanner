package com.salesforce.graph.build;

import com.google.common.collect.ImmutableSet;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.JustInTimeGraphProvider;
import com.salesforce.graph.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Some items in Apex are case insensitive. For instance, the following Apex is valid
 *
 * <p>
 *
 * <pre>
 *     String aString = 'Hello';
 *     System.debug(astring);
 * </pre>
 *
 * <p>This class is responsible for identifying all vertex properties that can be referenced in a
 * case insensitive manner. A correspondinger property is added to the vertex with the {@link
 * #CASE_SAFE_SUFFIX} appended to the orginal property name. The value is a normalized version of
 * the original string that is provided by the {@link #toCaseSafeProperty(String)} method.
 */
public class CaseSafePropertyUtil {
    public static final String CASE_SAFE_SUFFIX = "_CaseSafe";

    // TODO: This naming seems inverted. These properties can be accessed in a "case-insensitive"
    // manner
    // The following properties are (with rare exception) assumed to be case-sensitive.
    private static final ImmutableSet<String> CASE_SENSITIVE_PROPERTIES =
            ImmutableSet.of(
                    Schema.DEFINING_TYPE,
                    Schema.FULL_METHOD_NAME,
                    Schema.INTERFACE_DEFINING_TYPES,
                    Schema.INTERFACE_NAMES,
                    Schema.METHOD_NAME,
                    Schema.NAME,
                    Schema.RETURN_TYPE,
                    Schema.SUPER_CLASS_NAME,
                    Schema.SUPER_INTERFACE_NAME);

    static void addCaseSafeProperty(
            GraphTraversal<Vertex, Vertex> traversal, String property, Object value) {
        // Get the case-sensitive variant of the desired property, if one exists.
        String caseSafeVariant = toCaseSafeProperty(property).orElse(null);

        // If there's no case-safe variant of the property, we don't need to do anything.
        if (caseSafeVariant != null) {
            // Only strings and lists of strings can be made case-safe.
            if (value instanceof String) {
                traversal.property(caseSafeVariant, toCaseSafeValue((String) value));
            } else if (value instanceof ArrayList) {
                traversal.property(caseSafeVariant, toCaseSafeValue((ArrayList) value));
            }
        }
    }

    private static boolean isCaseSensitiveProperty(String property) {
        return CASE_SENSITIVE_PROPERTIES.contains(property);
    }

    private static Optional<String> toCaseSafeProperty(String property) {
        if (isCaseSensitiveProperty(property)) {
            return Optional.of(property + CASE_SAFE_SUFFIX);
        } else {
            return Optional.empty();
        }
    }

    private static String toCaseSafeValue(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static Object[] toCaseSafeValue(ArrayList value) {
        if (value.isEmpty() || !(value.get(0) instanceof String)) {
            // An empty array or non-string array can't be made case-safe.
            return value.toArray();
        } else {
            ArrayList<String> stringList = new ArrayList<>();
            for (Object o : value) {
                stringList.add(((String) o).toLowerCase(Locale.ROOT));
            }
            return stringList.toArray();
        }
    }

    /**
     * Dynamically load the type, this will be a no-op if JustInTimeGraph isn't currently enabled.
     */
    private static void dynamicallyLoadType(String definingType) {
        JustInTimeGraphProvider.get().loadUserClass(definingType);
    }

    public static class H {
        public static GraphTraversal<Object, Object> has(
                String nodeType, String property, String value) {
            if (property.equalsIgnoreCase(Schema.DEFINING_TYPE)) {
                dynamicallyLoadType(value);
            }
            String caseSafeVariant = toCaseSafeProperty(property).orElse(null);
            if (caseSafeVariant != null) {
                return __.has(nodeType, caseSafeVariant, toCaseSafeValue(value));
            } else {
                return __.has(nodeType, property, value);
            }
        }

        public static GraphTraversal<Object, Object> has(
                List<String> nodeTypes, String property, String value) {
            if (property.equalsIgnoreCase(Schema.DEFINING_TYPE)) {
                dynamicallyLoadType(value);
            }
            // Iterate through the provided types, looking for case-sensitive and case-insensitive
            // calls to the desired property.
            String sensitiveProp = null;
            List<String> sensitiveTypes = new ArrayList<>();
            String insensitiveProp = null;
            List<String> insensitiveTypes = new ArrayList<>();
            for (String nodeType : nodeTypes) {
                // Try to get a case-safe variant of the property.
                String caseSafeVariant = toCaseSafeProperty(property).orElse(null);
                if (caseSafeVariant != null) {
                    sensitiveProp = caseSafeVariant;
                    sensitiveTypes.add(nodeType);
                } else {
                    insensitiveProp = property;
                    insensitiveTypes.add(nodeType);
                }
            }

            if (insensitiveProp == null) {
                // If they're all case-sensitive, we can use a single `has` with the case-safe
                // value.
                return __.hasLabel(P.within(sensitiveTypes))
                        .has(sensitiveProp, toCaseSafeValue(value));
            } else if (sensitiveProp == null) {
                // If they're all case-insensitive, we can use a single `has` with the base value.
                return __.hasLabel(P.within(insensitiveTypes)).has(insensitiveProp, value);
            } else {
                // If there's a mix, then we'll need to do a OR.
                return __.or(
                        __.hasLabel(P.within(sensitiveTypes))
                                .has(sensitiveProp, toCaseSafeValue(value)),
                        __.hasLabel(P.within(insensitiveTypes)).has(insensitiveProp, value));
            }
        }

        public static GraphTraversal<Object, Object> hasWithin(
                String nodeType, String property, String... values) {
            return hasWithin(nodeType, property, Arrays.asList(values));
        }

        public static GraphTraversal<Object, Object> hasWithin(
                String nodeType, String property, Collection<String> values) {
            return hasWithin(nodeType, property, new ArrayList<>(values));
        }

        public static GraphTraversal<Object, Object> hasWithin(
                String nodeType, String property, ArrayList<String> values) {
            if (property.equalsIgnoreCase(Schema.DEFINING_TYPE)) {
                for (String value : values) {
                    dynamicallyLoadType(value);
                }
            }
            String caseSafeVariant = toCaseSafeProperty(property).orElse(null);
            if (caseSafeVariant != null) {
                return __.has(nodeType, caseSafeVariant, P.within(toCaseSafeValue(values)));
            } else {
                return __.has(nodeType, property, P.within(values));
            }
        }

        public static GraphTraversal<Object, Object> hasEndingWith(
                String nodeType, String property, String value) {
            if (property.equalsIgnoreCase(Schema.DEFINING_TYPE)) {
                throw new UnexpectedException(property);
            }
            String caseSafeVariant = toCaseSafeProperty(property).orElse(null);
            if (caseSafeVariant != null) {
                return __.has(nodeType, caseSafeVariant, TextP.endingWith(toCaseSafeValue(value)));
            } else {
                return __.has(nodeType, property, TextP.endingWith(value));
            }
        }

        public static GraphTraversal<Object, Object> hasArrayContaining(
                String nodeType, String property, String value) {
            if (property.equalsIgnoreCase(Schema.DEFINING_TYPE)) {
                throw new UnexpectedException(property);
            }
            String caseSafeVariant = toCaseSafeProperty(property).orElse(null);
            if (caseSafeVariant != null) {
                return __.hasLabel(nodeType)
                        .has(caseSafeVariant, __.unfold().is(toCaseSafeValue(value)));
            } else {
                return __.hasLabel(nodeType).has(property, __.unfold().is(value));
            }
        }
    }
}

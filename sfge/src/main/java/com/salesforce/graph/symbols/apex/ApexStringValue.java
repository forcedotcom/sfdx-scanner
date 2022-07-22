package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.SystemNames.METHOD_EQUALS;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.StringUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Represents an Apex string type See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_string.htm
 */
// TODO: Some methods are returning empty when they should return indeterminant instances of the
// expected type
public final class ApexStringValue extends ApexSimpleValue<ApexStringValue, String>
        implements DeepCloneable<ApexStringValue>, Typeable {
    static final String TYPE = TypeInfos.STRING.getApexName();
    static final String METHOD_ABBREVIATE = "abbreviate";
    static final String METHOD_CONTAINS = "contains";
    static final String METHOD_CONTAINS_IGNORE_CASE = "containsIgnoreCase";
    static final String METHOD_COUNT_MATCHES = "countMatches";
    static final String METHOD_DELETE_WHITESPACE = "deleteWhitespace";
    static final String METHOD_ENDS_WITH = "endsWith";
    static final String METHOD_ENDS_WITH_IGNORE_CASE = "endsWithIgnoreCase";
    static final String METHOD_EQUALS_IGNORE_CASE = "equalsIgnoreCase";
    static final String METHOD_ESCAPE_CSV = "escapeCsv";
    static final String METHOD_ESCAPE_ECMA_SCRIPT = "escapeEcmaScript";
    static final String METHOD_ESCAPE_HTML_3 = "escapeHtml3";
    static final String METHOD_ESCAPE_HTML_4 = "escapeHtml4";
    static final String METHOD_ESCAPE_JAVA = "escapeJava";
    static final String METHOD_ESCAPE_SINGLE_QUOTES = "escapeSingleQuotes";
    static final String METHOD_GET_S_OBJECT_TYPE = "getSObjectType";
    static final String METHOD_HASH_CODE = "hashCode";
    static final String METHOD_INDEX_OF = "indexOf";
    static final String METHOD_INDEX_OF_IGNORE_CASE = "indexOfIgnoreCase";
    static final String METHOD_LEFT = "left";
    static final String METHOD_LENGTH = "length";
    static final String METHOD_NORMALIZE_SPACE = "normalizeSpace";
    static final String METHOD_REMOVE = "remove";
    static final String METHOD_REMOVE_END = "removeEnd";
    static final String METHOD_REMOVE_END_IGNORE_CASE = "removeEndIgnoreCase";
    static final String METHOD_REMOVE_START = "removeStart";
    static final String METHOD_REMOVE_START_IGNORE_CASE = "removeStartIgnoreCase";
    static final String METHOD_REPLACE = "replace";
    static final String METHOD_REPLACE_ALL = "replaceAll";
    static final String METHOD_REPLACE_FIRST = "replaceFirst";
    static final String METHOD_RIGHT = "right";
    static final String METHOD_SPLIT = "split";
    static final String METHOD_STARTS_WITH = "startsWith";
    static final String METHOD_STARTS_WITH_IGNORE_CASE = "startsWithIgnoreCase";
    static final String METHOD_SUB_STRING = "subString";
    static final String METHOD_SUB_STRING_AFTER = "subStringAfter";
    static final String METHOD_SUB_STRING_BEFORE = "subStringBefore";
    static final String METHOD_SUB_STRING_BETWEEN = "subStringBetween";
    static final String METHOD_TO_LOWER_CASE = "toLowerCase";
    static final String METHOD_TO_UPPER_CASE = "toUpperCase";
    static final String METHOD_TRIM = "trim";
    static final String METHOD_UNESCAPE_CSV = "unescapeCsv";
    static final String METHOD_UNESCAPE_ECMA_SCRIPT = "unescapeEcmaScript";
    static final String METHOD_UNESCAPE_HTML_3 = "unescapeHtml3";
    static final String METHOD_UNESCAPE_HTML_4 = "unescapeHtml4";
    static final String METHOD_UNESCAPE_JAVA = "unescapeJava";

    ApexStringValue(String value, ApexValueBuilder builder) {
        super(TYPE, (value != null ? value.intern() : null), builder);
    }

    private ApexStringValue(ApexStringValue other) {
        super(other);
    }

    @Override
    public ApexStringValue deepClone() {
        return new ApexStringValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean matchesParameterType(Typeable parameterVertex) {
        return getCanonicalType().equalsIgnoreCase(parameterVertex.getCanonicalType())
                ||
                // Strings are also ids
                ApexIdValue.TYPE.equalsIgnoreCase(parameterVertex.getCanonicalType());
    }

    /**
     * String value can match a method that takes Id as parameter - priority is given to a method
     * with String parameter type over Id parameter type. At runtime, Apex checks if the string
     * value is in a valid Id format. We make an assumption here that the Apex developer has taken
     * the precaution. TODO: validate this assumption.
     */
    @Override
    public TypeableUtil.OrderedTreeSet getTypes() {
        final TypeableUtil.OrderedTreeSet typeHierarchy = new TypeableUtil.OrderedTreeSet();
        typeHierarchy.add(ApexStringValue.TYPE);
        typeHierarchy.add(ApexIdValue.TYPE);
        typeHierarchy.add(TypeInfos.OBJECT.getApexName());
        return typeHierarchy;
    }

    // Methods that return indeterminant values if this object is itself indeterminant or a value
    // isn't present.
    // We need to be extra careful because will attempt to execute methods that would normally not
    // be executed. For
    // instance if (o == null && o.length() > 10) will execute both side even if o is null
    // TODO: Short circuit the && condition like it would at runtime
    private static final TreeMap<String, Function<ApexValueBuilder, IndeterminantValueProvider<?>>>
            INDETERMINANT_VALUE_PROVIDERS =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_ABBREVIATE,
                                    IndeterminantValueProvider.StringValueProvider::get),
                            Pair.of(
                                    METHOD_GET_S_OBJECT_TYPE,
                                    IndeterminantValueProvider.StringValueProvider::get),
                            Pair.of(
                                    METHOD_HASH_CODE,
                                    IndeterminantValueProvider.IntegerValueProvider::get),
                            Pair.of(
                                    METHOD_LENGTH,
                                    IndeterminantValueProvider.IntegerValueProvider::get),
                            Pair.of(
                                    METHOD_SPLIT,
                                    IndeterminantValueProvider.ListValueProvider::getStringList),
                            Pair.of(
                                    METHOD_SUB_STRING,
                                    IndeterminantValueProvider.StringValueProvider::get));

    private static final TreeMap<String, Function<String, String>>
            METHOD_NAME_TO_FUNCTION_STRING_UTILS_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_DELETE_WHITESPACE, StringUtil::stripAllSpaces),
                            Pair.of(METHOD_ESCAPE_CSV, StringEscapeUtils::escapeCsv),
                            Pair.of(METHOD_ESCAPE_ECMA_SCRIPT, StringEscapeUtils::escapeEcmaScript),
                            Pair.of(METHOD_ESCAPE_HTML_3, StringEscapeUtils::escapeHtml3),
                            Pair.of(METHOD_ESCAPE_HTML_4, StringEscapeUtils::escapeHtml4),
                            Pair.of(METHOD_ESCAPE_JAVA, StringEscapeUtils::escapeJava),
                            Pair.of(
                                    METHOD_ESCAPE_SINGLE_QUOTES,
                                    (stringValue) -> stringValue.replace("'", "\\'")),
                            Pair.of(METHOD_NORMALIZE_SPACE, StringUtils::normalizeSpace),
                            Pair.of(METHOD_TO_LOWER_CASE, StringUtils::toRootLowerCase),
                            Pair.of(METHOD_TO_UPPER_CASE, StringUtils::toRootUpperCase),
                            Pair.of(METHOD_TRIM, StringUtils::trim),
                            Pair.of(METHOD_UNESCAPE_CSV, StringEscapeUtils::unescapeCsv),
                            Pair.of(
                                    METHOD_UNESCAPE_ECMA_SCRIPT,
                                    StringEscapeUtils::unescapeEcmaScript),
                            Pair.of(METHOD_UNESCAPE_HTML_3, StringEscapeUtils::unescapeHtml3),
                            Pair.of(METHOD_UNESCAPE_HTML_4, StringEscapeUtils::unescapeHtml4),
                            Pair.of(METHOD_UNESCAPE_JAVA, StringEscapeUtils::unescapeJava));

    private static final TreeMap<String, BiFunction<String, String, Integer>>
            METHOD_NAME_TO_BI_INT_UTILS_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_COUNT_MATCHES, StringUtils::countMatches),
                            Pair.of(METHOD_INDEX_OF, StringUtils::indexOf),
                            Pair.of(METHOD_INDEX_OF_IGNORE_CASE, StringUtils::indexOfIgnoreCase));

    private static final TreeMap<String, BiFunction<String, Integer, String>>
            METHOD_NAME_TO_BI_STRING_INTEGER_STRING_UTILS_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_LEFT, StringUtils::left),
                            Pair.of(METHOD_RIGHT, StringUtils::right));

    private static final TreeMap<String, BiFunction<String, String, String>>
            METHOD_NAME_TO_BI_STRING_STRING_STRING_UTILS_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_REMOVE, StringUtils::remove),
                            Pair.of(METHOD_REMOVE_END, StringUtils::removeEnd),
                            Pair.of(
                                    METHOD_REMOVE_END_IGNORE_CASE,
                                    StringUtils::removeEndIgnoreCase),
                            Pair.of(METHOD_REMOVE_START, StringUtils::removeStart),
                            Pair.of(
                                    METHOD_REMOVE_START_IGNORE_CASE,
                                    StringUtils::removeStartIgnoreCase),
                            Pair.of(METHOD_SUB_STRING_AFTER, StringUtils::substringAfter),
                            Pair.of(METHOD_SUB_STRING_BEFORE, StringUtils::substringBefore),
                            Pair.of(METHOD_SUB_STRING_BETWEEN, StringUtils::substringBetween));

    private static final TreeMap<String, BiFunction<String, String, Boolean>>
            METHOD_NAME_TO_BI_BOOLEAN_UTILS_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_CONTAINS, StringUtils::contains),
                            Pair.of(METHOD_CONTAINS_IGNORE_CASE, StringUtils::containsIgnoreCase),
                            Pair.of(METHOD_ENDS_WITH, StringUtils::endsWith),
                            Pair.of(METHOD_ENDS_WITH_IGNORE_CASE, StringUtils::endsWithIgnoreCase),
                            Pair.of(METHOD_EQUALS, StringUtils::equals),
                            Pair.of(METHOD_EQUALS_IGNORE_CASE, StringUtils::equalsIgnoreCase),
                            Pair.of(METHOD_STARTS_WITH, StringUtils::startsWith),
                            Pair.of(METHOD_STARTS_WITH_IGNORE_CASE, StringUtils::startsWith));

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @param v the third function argument
         * @return the function result
         */
        R apply(T t, U u, V v);
    }

    private static final TreeMap<String, TriFunction<String, String, String, String>>
            METHOD_NAME_TO_TRI_STRING_UTILS_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_REPLACE, StringUtils::replace),
                            Pair.of(METHOD_REPLACE_ALL, StringUtils::replaceAll),
                            Pair.of(METHOD_REPLACE_FIRST, StringUtils::replaceFirst));

    static {
        for (String key : METHOD_NAME_TO_FUNCTION_STRING_UTILS_METHOD.keySet()) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    key, IndeterminantValueProvider.StringValueProvider::get);
        }
        for (String key : METHOD_NAME_TO_BI_INT_UTILS_METHOD.keySet()) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    key, IndeterminantValueProvider.IntegerValueProvider::get);
        }
        for (String key : METHOD_NAME_TO_BI_STRING_INTEGER_STRING_UTILS_METHOD.keySet()) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    key, IndeterminantValueProvider.StringValueProvider::get);
        }
        for (String key : METHOD_NAME_TO_BI_STRING_STRING_STRING_UTILS_METHOD.keySet()) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    key, IndeterminantValueProvider.StringValueProvider::get);
        }
        for (String key : METHOD_NAME_TO_BI_BOOLEAN_UTILS_METHOD.keySet()) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    key, IndeterminantValueProvider.BooleanValueProvider::get);
        }
        for (String key : METHOD_NAME_TO_TRI_STRING_UTILS_METHOD.keySet()) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    key, IndeterminantValueProvider.StringValueProvider::get);
        }
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        final String methodName = vertex.getMethodName();

        if ((isValueNotPresent() || isIndeterminant())
                && INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        }

        final String value = getValue().orElse(null);
        List<ChainedVertex> parameters = vertex.getParameters();
        if (METHOD_ABBREVIATE.equalsIgnoreCase(methodName)) {
            validateParameterSizes(vertex, 1, 2);
            final Integer maxWidth =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            if (maxWidth != null) {
                if (parameters.size() == 2) {
                    final Integer offset =
                            convertParameterToInteger(parameters.get(1), symbols).orElse(null);
                    if (offset != null) {
                        // The order of parameters is reversed from the Apex method
                        return Optional.of(
                                builder.buildString(
                                        StringUtils.abbreviate(value, offset, maxWidth)));
                    }
                } else {
                    return Optional.of(
                            builder.buildString(StringUtils.abbreviate(value, maxWidth)));
                }
            }
        } else if (METHOD_NAME_TO_FUNCTION_STRING_UTILS_METHOD.containsKey(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(
                    builder.buildString(
                            METHOD_NAME_TO_FUNCTION_STRING_UTILS_METHOD
                                    .get(methodName)
                                    .apply(value)));
        } else if (METHOD_NAME_TO_BI_STRING_INTEGER_STRING_UTILS_METHOD.containsKey(methodName)) {
            validateParameterSize(vertex, 1);
            final Integer parameter =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_BI_STRING_INTEGER_STRING_UTILS_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (METHOD_NAME_TO_BI_STRING_STRING_STRING_UTILS_METHOD.containsKey(methodName)) {
            validateParameterSize(vertex, 1);
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_BI_STRING_STRING_STRING_UTILS_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (METHOD_NAME_TO_BI_BOOLEAN_UTILS_METHOD.containsKey(methodName)) {
            validateParameterSize(vertex, 1);
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildBoolean(
                                METHOD_NAME_TO_BI_BOOLEAN_UTILS_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (METHOD_NAME_TO_BI_INT_UTILS_METHOD.containsKey(methodName)) {
            validateParameterSize(vertex, 1);
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildInteger(
                                METHOD_NAME_TO_BI_INT_UTILS_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (METHOD_NAME_TO_TRI_STRING_UTILS_METHOD.containsKey(methodName)) {
            validateParameterSize(vertex, 2);
            final String parameter1 =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            final String parameter2 =
                    convertParameterToString(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_TRI_STRING_UTILS_METHOD
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (METHOD_HASH_CODE.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildInteger(value.hashCode()));
        } else if (METHOD_LENGTH.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 0);
            return Optional.of(builder.buildInteger(value.length()));
        } else if (METHOD_SPLIT.equalsIgnoreCase(methodName)) {
            validateParameterSizes(vertex, 1, 2);
            final String regEx = convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (regEx != null) {
                final ApexListValue apexListValue = builder.deepClone().buildList();
                if (parameters.size() == 1) {
                    for (String result : StringUtils.split(value, regEx)) {
                        apexListValue.add(builder.deepClone().buildString(result));
                    }
                    return Optional.of(apexListValue);
                } else {
                    final Integer limit =
                            convertParameterToInteger(parameters.get(0), symbols).orElse(null);
                    if (limit != null) {
                        for (String result : StringUtils.split(value, regEx, limit)) {
                            apexListValue.add(builder.deepClone().buildString(result));
                        }
                        return Optional.of(apexListValue);
                    }
                }
            }
        } else if (METHOD_SUB_STRING.equalsIgnoreCase(methodName)) {
            validateParameterSize(vertex, 2);
            final Integer from = convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            final Integer to = convertParameterToInteger(parameters.get(1), symbols).orElse(null);
            if (from != null & to != null) {
                return Optional.of(builder.buildString(value.substring(from, to)));
            }
        } else {
            throw new UnimplementedMethodException(this, vertex);
        }

        // Return an indeterminant value if the method specific code did not return. This is
        // typically because one or
        // more of the parameters could not be resolved.
        return Optional.ofNullable(
                INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
    }

    @Override
    public Optional<Typeable> getTypeVertex() {
        Optional<Typeable> result = super.getTypeVertex();
        if (!result.isPresent()) {
            result = Optional.of(this);
        }
        return result;
    }

    @Override
    public Optional<String> getValue() {
        final String result;
        if (valueVertex instanceof LiteralExpressionVertex.SFString) {
            result = ((LiteralExpressionVertex.SFString) valueVertex).getLiteral();
        } else if (valueVertex instanceof VariableExpressionVertex) {
            ChainedVertex resolved = getValue(valueVertex).orElse(null);
            if (resolved instanceof LiteralExpressionVertex.SFString) {
                result = ((LiteralExpressionVertex.SFString) resolved).getLiteral();
            } else {
                return Optional.empty();
            }
        } else {
            result = value;
        }
        return Optional.ofNullable(result);
    }

    /**
     * IMPORTANT: Apex strings are case sensitive. #equals needs to account for this. This method
     * was hand edited to use #getValue instead of value directly.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexStringValue that = (ApexStringValue) o;
        return Objects.equals(getValue().orElse(null), that.getValue().orElse(null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue().orElse(null));
    }

    @Override
    public String toString() {
        return "ApexStringValue{" + " value=" + getValue() + "} " + super.toString();
    }
}

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
import java.util.*;
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
    static final String METHOD_CAPITALIZE = "capitalize";
    static final String METHOD_CENTER = "center";
    static final String METHOD_CHAR_AT = "charAt";
    static final String METHOD_CODE_POINT_AT = "codePointAt";
    static final String METHOD_CODE_POINT_BEFORE = "codePointBefore";
    static final String METHOD_CODE_POINT_COUNT = "codePointCount";
    static final String METHOD_COMPARE_TO = "compareTo";
    static final String METHOD_CONTAINS = "contains";
    static final String METHOD_CONTAINS_ANY = "containsAny";
    static final String METHOD_CONTAINS_IGNORE_CASE = "containsIgnoreCase";
    static final String METHOD_CONTAINS_NONE = "containsNone";
    static final String METHOD_CONTAINS_ONLY = "containsOnly";
    static final String METHOD_CONTAINS_WHITESPACE = "containsWhitespace";
    static final String METHOD_COUNT_MATCHES = "countMatches";
    static final String METHOD_DELETE_WHITESPACE = "deleteWhitespace";
    static final String METHOD_DIFFERENCE = "difference";
    static final String METHOD_ENDS_WITH = "endsWith";
    static final String METHOD_ENDS_WITH_IGNORE_CASE = "endsWithIgnoreCase";
    static final String METHOD_EQUALS_IGNORE_CASE = "equalsIgnoreCase";
    static final String METHOD_ESCAPE_CSV = "escapeCsv";
    static final String METHOD_ESCAPE_ECMA_SCRIPT = "escapeEcmaScript";
    static final String METHOD_ESCAPE_HTML_3 = "escapeHtml3";
    static final String METHOD_ESCAPE_HTML_4 = "escapeHtml4";
    static final String METHOD_ESCAPE_JAVA = "escapeJava";
    static final String METHOD_ESCAPE_SINGLE_QUOTES = "escapeSingleQuotes";
    static final String METHOD_ESCAPE_UNICODE = "escapeUnicode";
    static final String METHOD_ESCAPE_XML = "escapeXml";
    static final String METHOD_GET_CHARS = "getChars";
    static final String METHOD_GET_LEVENSHTEIN_DISTANCE = "getLevenshteinDistance";
    static final String METHOD_GET_S_OBJECT_TYPE = "getSObjectType";
    static final String METHOD_HASH_CODE = "hashCode";
    static final String METHOD_INDEX_OF = "indexOf";
    static final String METHOD_INDEX_OF_ANY = "indexOfAny";
    static final String METHOD_INDEX_OF_ANY_BUT = "indexOfAnyBut";
    static final String METHOD_INDEX_OF_CHAR = "indexOfChar";
    static final String METHOD_INDEX_OF_DIFFERENCE = "indexOfDifference";
    static final String METHOD_INDEX_OF_IGNORE_CASE = "indexOfIgnoreCase";
    static final String METHOD_IS_ALL_LOWER_CASE = "isAllLowerCase";
    static final String METHOD_IS_ALL_UPPER_CASE = "isAllUpperCase";
    static final String METHOD_IS_ALPHA = "isAlpha";
    static final String METHOD_IS_ALPHA_SPACE = "isAlphaSpace";
    static final String METHOD_IS_ALPHANUMERIC = "isAlphanumeric";
    static final String METHOD_IS_ALPHANUMERIC_SPACE = "isAlphanumericSpace";
    static final String METHOD_IS_ASCII_PRINTABLE = "isAsciiPrintable";
    static final String METHOD_IS_BLANK = "isBlank";
    static final String METHOD_IS_EMPTY = "isEmpty";
    static final String METHOD_IS_NOT_BLANK = "isNotBlank";
    static final String METHOD_IS_NOT_EMPTY = "isNotEmpty";
    static final String METHOD_IS_NUMERIC = "isNumeric";
    static final String METHOD_IS_NUMERIC_SPACE = "isNumericSpace";
    static final String METHOD_IS_WHITESPACE = "isWhitespace";
    static final String METHOD_LAST_INDEX_OF = "lastIndexOf";
    static final String METHOD_LAST_INDEX_OF_CHAR = "lastIndexOfChar";
    static final String METHOD_LAST_INDEX_OF_IGNORE_CASE = "lastIndexOfIgnoreCase";
    static final String METHOD_LEFT = "left";
    static final String METHOD_LEFT_PAD = "leftPad";
    static final String METHOD_LENGTH = "length";
    static final String METHOD_MID = "mid";
    static final String METHOD_NORMALIZE_SPACE = "normalizeSpace";
    static final String METHOD_OFFSET_BY_CODE_POINTS = "offsetByCodePoints";
    static final String METHOD_REMOVE = "remove";
    static final String METHOD_REMOVE_END = "removeEnd";
    static final String METHOD_REMOVE_END_IGNORE_CASE = "removeEndIgnoreCase";
    static final String METHOD_REMOVE_START = "removeStart";
    static final String METHOD_REMOVE_START_IGNORE_CASE = "removeStartIgnoreCase";
    static final String METHOD_REPEAT = "repeat";
    static final String METHOD_REPLACE = "replace";
    static final String METHOD_REPLACE_ALL = "replaceAll";
    static final String METHOD_REPLACE_FIRST = "replaceFirst";
    static final String METHOD_REVERSE = "reverse";
    static final String METHOD_RIGHT = "right";
    static final String METHOD_RIGHT_PAD = "rightPad";
    static final String METHOD_SPLIT = "split";
    static final String METHOD_SPLIT_BY_CHARACTER_TYPE = "splitByCharacterType";
    static final String METHOD_SPLIT_BY_CHARACTER_TYPE_CAMEL_CASE = "splitByCharacterTypeCamelCase";
    static final String METHOD_STARTS_WITH = "startsWith";
    static final String METHOD_STARTS_WITH_IGNORE_CASE = "startsWithIgnoreCase";
    static final String METHOD_STRIP_HTML_TAGS = "stripHtmlTags";
    static final String METHOD_SUB_STRING = "substring";
    static final String METHOD_SUB_STRING_AFTER = "substringAfter";
    static final String METHOD_SUB_STRING_AFTER_LAST = "substringAfterLast";
    static final String METHOD_SUB_STRING_BEFORE = "substringBefore";
    static final String METHOD_SUB_STRING_BEFORE_LAST = "substringBeforeLast";
    static final String METHOD_SUB_STRING_BETWEEN = "substringBetween";
    static final String METHOD_SWAP_CASE = "swapCase";
    static final String METHOD_TO_LOWER_CASE = "toLowerCase";
    static final String METHOD_TO_UPPER_CASE = "toUpperCase";
    static final String METHOD_TRIM = "trim";
    static final String METHOD_UNCAPITALIZE = "uncapitalize";
    static final String METHOD_UNESCAPE_CSV = "unescapeCsv";
    static final String METHOD_UNESCAPE_ECMA_SCRIPT = "unescapeEcmaScript";
    static final String METHOD_UNESCAPE_HTML_3 = "unescapeHtml3";
    static final String METHOD_UNESCAPE_HTML_4 = "unescapeHtml4";
    static final String METHOD_UNESCAPE_JAVA = "unescapeJava";
    static final String METHOD_UNESCAPE_UNICODE = "unescapeUnicode";
    static final String METHOD_UNESCAPE_XML = "unescapeXml";

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
    private static final Map<String, Function<ApexValueBuilder, IndeterminantValueProvider<?>>>
            INDETERMINANT_VALUE_PROVIDERS =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_GET_S_OBJECT_TYPE,
                                    IndeterminantValueProvider.StringValueProvider::get),
                            Pair.of(
                                    METHOD_SPLIT,
                                    IndeterminantValueProvider.ListValueProvider::getStringList),
                            Pair.of(
                                    METHOD_STRIP_HTML_TAGS,
                                    IndeterminantValueProvider.StringValueProvider::get));

    private static final Map<String, Function<String, String>>
            METHOD_NAME_TO_FUNCTION_STRING_STRING_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_CAPITALIZE, StringUtils::capitalize),
                            Pair.of(METHOD_DELETE_WHITESPACE, StringUtil::stripAllSpaces),
                            Pair.of(METHOD_ESCAPE_CSV, StringEscapeUtils::escapeCsv),
                            Pair.of(METHOD_ESCAPE_ECMA_SCRIPT, StringEscapeUtils::escapeEcmaScript),
                            Pair.of(METHOD_ESCAPE_HTML_3, StringEscapeUtils::escapeHtml3),
                            Pair.of(METHOD_ESCAPE_HTML_4, StringEscapeUtils::escapeHtml4),
                            Pair.of(METHOD_ESCAPE_JAVA, StringEscapeUtils::escapeJava),
                            Pair.of(METHOD_ESCAPE_SINGLE_QUOTES, s -> s.replace("'", "\\'")),
                            Pair.of(METHOD_ESCAPE_UNICODE, StringEscapeUtils::escapeJava),
                            Pair.of(METHOD_ESCAPE_XML, StringEscapeUtils::escapeXml10),
                            Pair.of(METHOD_NORMALIZE_SPACE, StringUtils::normalizeSpace),
                            Pair.of(METHOD_REVERSE, StringUtils::reverse),
                            Pair.of(METHOD_SWAP_CASE, StringUtils::swapCase),
                            Pair.of(METHOD_TO_LOWER_CASE, StringUtils::toRootLowerCase),
                            Pair.of(METHOD_TO_UPPER_CASE, StringUtils::toRootUpperCase),
                            Pair.of(METHOD_TRIM, StringUtils::trim),
                            Pair.of(METHOD_UNCAPITALIZE, StringUtils::uncapitalize),
                            Pair.of(METHOD_UNESCAPE_CSV, StringEscapeUtils::unescapeCsv),
                            Pair.of(
                                    METHOD_UNESCAPE_ECMA_SCRIPT,
                                    StringEscapeUtils::unescapeEcmaScript),
                            Pair.of(METHOD_UNESCAPE_HTML_3, StringEscapeUtils::unescapeHtml3),
                            Pair.of(METHOD_UNESCAPE_HTML_4, StringEscapeUtils::unescapeHtml4),
                            Pair.of(METHOD_UNESCAPE_JAVA, StringEscapeUtils::unescapeJava),
                            Pair.of(METHOD_UNESCAPE_UNICODE, StringEscapeUtils::unescapeJava),
                            Pair.of(METHOD_UNESCAPE_XML, StringEscapeUtils::unescapeXml));

    private static final Map<String, Function<String, Boolean>>
            METHOD_NAME_TO_FUNCTION_STRING_BOOLEAN_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_CONTAINS_WHITESPACE, StringUtils::containsWhitespace),
                            Pair.of(METHOD_IS_ALL_LOWER_CASE, StringUtils::isAllLowerCase),
                            Pair.of(METHOD_IS_ALL_UPPER_CASE, StringUtils::isAllUpperCase),
                            Pair.of(METHOD_IS_ALPHA, StringUtils::isAlpha),
                            Pair.of(METHOD_IS_ALPHA_SPACE, StringUtils::isAlphaSpace),
                            Pair.of(METHOD_IS_ALPHANUMERIC, StringUtils::isAlphanumeric),
                            Pair.of(METHOD_IS_ALPHANUMERIC_SPACE, StringUtils::isAlphanumericSpace),
                            Pair.of(METHOD_IS_ASCII_PRINTABLE, StringUtils::isAsciiPrintable),
                            Pair.of(METHOD_IS_BLANK, StringUtils::isBlank),
                            Pair.of(METHOD_IS_EMPTY, StringUtils::isEmpty),
                            Pair.of(METHOD_IS_NOT_BLANK, StringUtils::isNotBlank),
                            Pair.of(METHOD_IS_NOT_EMPTY, StringUtils::isNotEmpty),
                            Pair.of(METHOD_IS_NUMERIC, StringUtils::isNumeric),
                            Pair.of(METHOD_IS_NUMERIC_SPACE, StringUtils::isNumericSpace),
                            Pair.of(METHOD_IS_WHITESPACE, StringUtils::isWhitespace));

    private static final Map<String, Function<String, Integer>>
            METHOD_NAME_TO_FUNCTION_STRING_INTEGER_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_HASH_CODE, String::length),
                            Pair.of(METHOD_LENGTH, String::length));

    private static final Map<String, Function<String, List<Integer>>>
            METHOD_NAME_TO_FUNCTION_STRING_INTEGER_LIST_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_GET_CHARS,
                                    s -> charArrayToIntegerList(s.toCharArray())));

    private static final Map<String, Function<String, List<String>>>
            METHOD_NAME_TO_FUNCTION_STRING_STRING_LIST_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_SPLIT_BY_CHARACTER_TYPE,
                                    s -> Arrays.asList(StringUtils.splitByCharacterType(s))),
                            Pair.of(
                                    METHOD_SPLIT_BY_CHARACTER_TYPE_CAMEL_CASE,
                                    s ->
                                            Arrays.asList(
                                                    StringUtils.splitByCharacterTypeCamelCase(s))));

    private static final Map<String, BiFunction<String, String, Integer>>
            METHOD_NAME_TO_BIFUNCTION_STRING_STRING_INTEGER_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_COMPARE_TO, StringUtils::compare),
                            Pair.of(METHOD_COUNT_MATCHES, StringUtils::countMatches),
                            Pair.of(
                                    METHOD_GET_LEVENSHTEIN_DISTANCE,
                                    StringUtils::getLevenshteinDistance),
                            Pair.of(METHOD_INDEX_OF, StringUtils::indexOf),
                            Pair.of(METHOD_INDEX_OF_ANY, StringUtils::indexOfAny),
                            Pair.of(METHOD_INDEX_OF_ANY_BUT, StringUtils::indexOfAnyBut),
                            Pair.of(METHOD_INDEX_OF_DIFFERENCE, StringUtils::indexOfDifference),
                            Pair.of(METHOD_INDEX_OF_IGNORE_CASE, StringUtils::indexOfIgnoreCase),
                            Pair.of(METHOD_LAST_INDEX_OF, StringUtils::lastIndexOf),
                            Pair.of(
                                    METHOD_LAST_INDEX_OF_IGNORE_CASE,
                                    StringUtils::lastIndexOfIgnoreCase));

    private static final Map<String, BiFunction<String, Integer, Integer>>
            METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_INTEGER_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_CHAR_AT,
                                    (s, i) -> Character.getNumericValue(s.charAt(i))),
                            Pair.of(METHOD_CODE_POINT_AT, String::codePointAt),
                            Pair.of(METHOD_CODE_POINT_BEFORE, String::codePointBefore),
                            Pair.of(METHOD_INDEX_OF_CHAR, String::indexOf),
                            Pair.of(METHOD_LAST_INDEX_OF_CHAR, StringUtils::lastIndexOf));

    private static final Map<String, BiFunction<String, Integer, String>>
            METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_STRING_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_ABBREVIATE, StringUtils::abbreviate),
                            Pair.of(METHOD_CENTER, StringUtils::center),
                            Pair.of(METHOD_LEFT, StringUtils::left),
                            Pair.of(METHOD_LEFT_PAD, StringUtils::leftPad),
                            Pair.of(METHOD_RIGHT, StringUtils::right),
                            Pair.of(METHOD_RIGHT_PAD, StringUtils::rightPad),
                            Pair.of(METHOD_REPEAT, StringUtils::repeat),
                            Pair.of(METHOD_SUB_STRING, StringUtils::substring));

    private static final Map<String, BiFunction<String, String, String>>
            METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_DIFFERENCE, StringUtils::difference),
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
                            Pair.of(METHOD_SUB_STRING_AFTER_LAST, StringUtils::substringAfterLast),
                            Pair.of(METHOD_SUB_STRING_BEFORE, StringUtils::substringBefore),
                            Pair.of(
                                    METHOD_SUB_STRING_BEFORE_LAST,
                                    StringUtils::substringBeforeLast),
                            Pair.of(METHOD_SUB_STRING_BETWEEN, StringUtils::substringBetween));

    private static final Map<String, BiFunction<String, String, List<String>>>
            METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_LIST_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_SPLIT,
                                    (s, regExp) -> Arrays.asList(StringUtils.split(s, regExp))));

    private static final Map<String, BiFunction<String, String, Boolean>>
            METHOD_NAME_TO_BIFUNCTION_STRING_STRING_BOOLEAN_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_CONTAINS, StringUtils::contains),
                            Pair.of(METHOD_CONTAINS_ANY, StringUtils::containsAny),
                            Pair.of(METHOD_CONTAINS_IGNORE_CASE, StringUtils::containsIgnoreCase),
                            Pair.of(METHOD_CONTAINS_NONE, StringUtils::containsNone),
                            Pair.of(METHOD_CONTAINS_ONLY, StringUtils::containsOnly),
                            Pair.of(METHOD_ENDS_WITH, StringUtils::endsWith),
                            Pair.of(METHOD_ENDS_WITH_IGNORE_CASE, StringUtils::endsWithIgnoreCase),
                            Pair.of(METHOD_EQUALS, StringUtils::equals),
                            Pair.of(METHOD_EQUALS_IGNORE_CASE, StringUtils::equalsIgnoreCase),
                            Pair.of(METHOD_STARTS_WITH, StringUtils::startsWith),
                            Pair.of(METHOD_STARTS_WITH_IGNORE_CASE, StringUtils::startsWith));

    private static final Map<String, TriFunction<String, Integer, Integer, Integer>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_INTEGER_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_CODE_POINT_COUNT, String::codePointCount),
                            Pair.of(METHOD_INDEX_OF_CHAR, StringUtils::indexOf),
                            Pair.of(METHOD_LAST_INDEX_OF_CHAR, StringUtils::lastIndexOf),
                            Pair.of(METHOD_OFFSET_BY_CODE_POINTS, String::offsetByCodePoints));

    private static final Map<String, TriFunction<String, String, Integer, Integer>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_INTEGER_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_INDEX_OF, String::indexOf),
                            Pair.of(METHOD_INDEX_OF_IGNORE_CASE, StringUtils::indexOfIgnoreCase),
                            Pair.of(METHOD_LAST_INDEX_OF, StringUtils::lastIndexOf),
                            Pair.of(
                                    METHOD_LAST_INDEX_OF_IGNORE_CASE,
                                    StringUtils::lastIndexOfIgnoreCase));

    private static final Map<String, TriFunction<String, Integer, Integer, String>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_STRING_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_ABBREVIATE,
                                    (s, maxWidth, offset) ->
                                            StringUtils.abbreviate(
                                                    s, offset, maxWidth)), // notice order swap
                            Pair.of(METHOD_MID, StringUtils::mid),
                            Pair.of(METHOD_SUB_STRING, StringUtils::substring));

    private static final Map<String, TriFunction<String, Integer, String, String>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_STRING_STRING =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_LEFT_PAD, StringUtils::leftPad),
                            Pair.of(METHOD_RIGHT_PAD, StringUtils::rightPad));

    private static final Map<String, TriFunction<String, String, Integer, String>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING =
                    CollectionUtil.newTreeMapOf(Pair.of(METHOD_REPEAT, StringUtils::repeat));

    private static final Map<String, TriFunction<String, String, String, String>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_STRING_STRING_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(METHOD_REPLACE, StringUtils::replace),
                            Pair.of(METHOD_REPLACE_ALL, StringUtils::replaceAll),
                            Pair.of(METHOD_REPLACE_FIRST, StringUtils::replaceFirst));

    private static final Map<String, TriFunction<String, String, Integer, List<String>>>
            METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING_LIST_METHOD =
                    CollectionUtil.newTreeMapOf(
                            Pair.of(
                                    METHOD_SPLIT,
                                    (s, regExp, limit) ->
                                            Arrays.asList(StringUtils.split(s, regExp, limit))));

    static {
        Set<String> booleanReturnMethods = new HashSet<>();
        booleanReturnMethods.addAll(METHOD_NAME_TO_FUNCTION_STRING_BOOLEAN_METHOD.keySet());
        booleanReturnMethods.addAll(
                METHOD_NAME_TO_BIFUNCTION_STRING_STRING_BOOLEAN_METHOD.keySet());
        for (String methodName : booleanReturnMethods) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    methodName, IndeterminantValueProvider.BooleanValueProvider::get);
        }

        Set<String> stringReturnMethods = new HashSet<>();
        stringReturnMethods.addAll(METHOD_NAME_TO_FUNCTION_STRING_STRING_METHOD.keySet());
        stringReturnMethods.addAll(METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_STRING_METHOD.keySet());
        stringReturnMethods.addAll(METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_METHOD.keySet());
        stringReturnMethods.addAll(
                METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_STRING_STRING_METHOD.keySet());
        stringReturnMethods.addAll(
                METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_STRING_STRING.keySet());
        stringReturnMethods.addAll(
                METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING.keySet());
        stringReturnMethods.addAll(
                METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_STRING_METHOD.keySet());
        for (String methodName : stringReturnMethods) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    methodName, IndeterminantValueProvider.StringValueProvider::get);
        }

        Set<String> integerReturnMethods = new HashSet<>();
        integerReturnMethods.addAll(METHOD_NAME_TO_FUNCTION_STRING_INTEGER_METHOD.keySet());
        integerReturnMethods.addAll(
                METHOD_NAME_TO_BIFUNCTION_STRING_STRING_INTEGER_METHOD.keySet());
        integerReturnMethods.addAll(
                METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_INTEGER_METHOD.keySet());
        integerReturnMethods.addAll(
                METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_INTEGER_METHOD.keySet());
        integerReturnMethods.addAll(
                METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_INTEGER_METHOD.keySet());
        for (String methodName : integerReturnMethods) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    methodName, IndeterminantValueProvider.IntegerValueProvider::get);
        }

        Set<String> stringListReturnMethods = new HashSet<>();
        stringListReturnMethods.addAll(METHOD_NAME_TO_FUNCTION_STRING_STRING_LIST_METHOD.keySet());
        stringListReturnMethods.addAll(
                METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_LIST_METHOD.keySet());
        for (String methodName : stringListReturnMethods) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    methodName, IndeterminantValueProvider.ListValueProvider::getStringList);
        }

        Set<String> integerListReturnMethods = new HashSet<>();
        integerListReturnMethods.addAll(
                METHOD_NAME_TO_FUNCTION_STRING_INTEGER_LIST_METHOD.keySet());
        for (String methodName : integerListReturnMethods) {
            INDETERMINANT_VALUE_PROVIDERS.put(
                    methodName, IndeterminantValueProvider.ListValueProvider::getIntegerList);
        }
    }

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

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        final String methodName = vertex.getMethodName();

        if ((isValueNotPresent() || isIndeterminant())
                && INDETERMINANT_VALUE_PROVIDERS.containsKey(methodName)) {
            return Optional.ofNullable(
                    INDETERMINANT_VALUE_PROVIDERS.get(methodName).apply(builder).get());
        }

        int numParams = vertex.getParameters().size();

        final String value = getValue().orElse(null);
        List<ChainedVertex> parameters = vertex.getParameters();

        if (numParams == 0
                && METHOD_NAME_TO_FUNCTION_STRING_INTEGER_METHOD.containsKey(methodName)) {
            return Optional.of(
                    builder.buildInteger(
                            METHOD_NAME_TO_FUNCTION_STRING_INTEGER_METHOD
                                    .get(methodName)
                                    .apply(value)));
        } else if (numParams == 0
                && METHOD_NAME_TO_FUNCTION_STRING_BOOLEAN_METHOD.containsKey(methodName)) {
            return Optional.of(
                    builder.buildBoolean(
                            METHOD_NAME_TO_FUNCTION_STRING_BOOLEAN_METHOD
                                    .get(methodName)
                                    .apply(value)));
        } else if (numParams == 0
                && METHOD_NAME_TO_FUNCTION_STRING_STRING_METHOD.containsKey(methodName)) {
            return Optional.of(
                    builder.buildString(
                            METHOD_NAME_TO_FUNCTION_STRING_STRING_METHOD
                                    .get(methodName)
                                    .apply(value)));
        } else if (numParams == 0
                && METHOD_NAME_TO_FUNCTION_STRING_INTEGER_LIST_METHOD.containsKey(methodName)) {
            ApexListValue apexListValue = builder.deepClone().buildList();
            List<Integer> outList =
                    METHOD_NAME_TO_FUNCTION_STRING_INTEGER_LIST_METHOD.get(methodName).apply(value);
            for (Integer result : outList) {
                apexListValue.add(builder.deepClone().buildInteger(result));
            }
            return Optional.of(apexListValue);
        } else if (numParams == 0
                && METHOD_NAME_TO_FUNCTION_STRING_STRING_LIST_METHOD.containsKey(methodName)) {
            ApexListValue apexListValue = builder.deepClone().buildList();
            List<String> outList =
                    METHOD_NAME_TO_FUNCTION_STRING_STRING_LIST_METHOD.get(methodName).apply(value);
            for (String result : outList) {
                apexListValue.add(builder.deepClone().buildString(result));
            }
            return Optional.of(apexListValue);
        } else if (numParams == 1
                && METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_STRING_METHOD.containsKey(methodName)) {
            final Integer parameter =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_STRING_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (numParams == 1
                && METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_METHOD.containsKey(methodName)) {
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (numParams == 1
                && METHOD_NAME_TO_BIFUNCTION_STRING_STRING_BOOLEAN_METHOD.containsKey(methodName)) {
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildBoolean(
                                METHOD_NAME_TO_BIFUNCTION_STRING_STRING_BOOLEAN_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (numParams == 1
                && METHOD_NAME_TO_BIFUNCTION_STRING_STRING_INTEGER_METHOD.containsKey(methodName)) {
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildInteger(
                                METHOD_NAME_TO_BIFUNCTION_STRING_STRING_INTEGER_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (numParams == 1
                && METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_INTEGER_METHOD.containsKey(
                        methodName)) {
            final Integer parameter =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                return Optional.of(
                        builder.buildInteger(
                                METHOD_NAME_TO_BIFUNCTION_STRING_INTEGER_INTEGER_METHOD
                                        .get(methodName)
                                        .apply(value, parameter)));
            }
        } else if (numParams == 1
                && METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_LIST_METHOD.containsKey(
                        methodName)) {
            final String parameter =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            if (parameter != null) {
                ApexListValue apexListValue = builder.deepClone().buildList();
                List<String> outList =
                        METHOD_NAME_TO_BIFUNCTION_STRING_STRING_STRING_LIST_METHOD
                                .get(methodName)
                                .apply(value, parameter);
                for (String result : outList) {
                    apexListValue.add(builder.deepClone().buildString(result));
                }
                return Optional.of(apexListValue);
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_INTEGER_METHOD.containsKey(
                        methodName)) {
            final Integer parameter1 =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            final Integer parameter2 =
                    convertParameterToInteger(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildInteger(
                                METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_INTEGER_METHOD
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_INTEGER_METHOD.containsKey(
                        methodName)) {
            final String parameter1 =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            final Integer parameter2 =
                    convertParameterToInteger(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildInteger(
                                METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_INTEGER_METHOD
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_STRING_STRING_METHOD.containsKey(
                        methodName)) {
            final String parameter1 =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            final String parameter2 =
                    convertParameterToString(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_STRING_STRING_METHOD
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_STRING_STRING.containsKey(
                        methodName)) {
            final Integer parameter1 =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            final String parameter2 =
                    convertParameterToString(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_STRING_STRING
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING.containsKey(
                        methodName)) {
            final String parameter1 =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            final Integer parameter2 =
                    convertParameterToInteger(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_STRING_METHOD.containsKey(
                        methodName)) {
            final Integer parameter1 =
                    convertParameterToInteger(parameters.get(0), symbols).orElse(null);
            final Integer parameter2 =
                    convertParameterToInteger(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                return Optional.of(
                        builder.buildString(
                                METHOD_NAME_TO_TRIFUNCTION_STRING_INTEGER_INTEGER_STRING_METHOD
                                        .get(methodName)
                                        .apply(value, parameter1, parameter2)));
            }
        } else if (numParams == 2
                && METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING_LIST_METHOD.containsKey(
                        methodName)) {
            final String parameter1 =
                    convertParameterToString(parameters.get(0), symbols).orElse(null);
            final Integer parameter2 =
                    convertParameterToInteger(parameters.get(1), symbols).orElse(null);
            if (parameter1 != null && parameter2 != null) {
                ApexListValue apexListValue = builder.deepClone().buildList();
                List<String> outList =
                        METHOD_NAME_TO_TRIFUNCTION_STRING_STRING_INTEGER_STRING_LIST_METHOD
                                .get(methodName)
                                .apply(value, parameter1, parameter2);
                for (String result : outList) {
                    apexListValue.add(builder.deepClone().buildString(result));
                }
                return Optional.of(apexListValue);
            }
        } else if (METHOD_STRIP_HTML_TAGS.equalsIgnoreCase(methodName)) {
            // no local implementation for this, so let it return indeterminant value for now
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

    private static List<Integer> charArrayToIntegerList(char[] charArr) {
        List<Integer> integerList = new ArrayList<>();
        for (int i = 0; i < charArr.length; i++) {
            integerList.add(Character.getNumericValue(charArr[i]));
        }
        return integerList;
    }
}

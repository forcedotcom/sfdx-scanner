package com.salesforce.graph.ops;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.apex.SoqlQueryInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility that helps with parsing queries as well as extracting summary information from parsed
 * queries.
 */
public final class SoqlParserUtil {
    private static final Logger LOGGER = LogManager.getLogger(SoqlParserUtil.class);
    public static final String UNKNOWN = "Unknown";
    private static final String INNER_QUERY = "INNER_QUERY";

    private static final char STARTING_BRACKET = '(';
    private static final char ENDING_BRACKET = ')';

    private static final String FIELD_PATTERN_STR =
            "SELECT\\s+(.*)FROM"; // TODO: handle os-specific newlines
    private static final Pattern FIELD_PATTERN =
            Pattern.compile(FIELD_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String OBJECT_PATTERN_STR = "FROM\\b\\s+([^\\s]*)\\b";
    private static final Pattern OBJECT_PATTERN =
            Pattern.compile(OBJECT_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String WHERE_PATTERN_STR =
            "WHERE\\s+\\b([^\\s!=><]*)\\b"; // First field after WHERE
    private static final Pattern WHERE_PATTERN =
            Pattern.compile(WHERE_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String EXTENDED_WHERE_PATTERN_STR =
            "(?:AND|OR)\\s+\\b([^\\s!=><]*)\\b"; // Field after AND/OR
    private static final Pattern EXTENDED_WHERE_PATTERN =
            Pattern.compile(EXTENDED_WHERE_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String WITH_SECURITY_ENFORCED_PATTERN_STR = "WITH\\s+SECURITY_ENFORCED";
    private static final Pattern WITH_SECURITY_ENFORCED_PATTERN =
            Pattern.compile(WITH_SECURITY_ENFORCED_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String WITH_USER_MODE_PATTERN_STR = "WITH\\s+USER_MODE";
    private static final Pattern WITH_USER_MODE_PATTERN =
            Pattern.compile(WITH_USER_MODE_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String COUNT_PATTERN_STR = "COUNT\\(\\s*\\)";
    private static final Pattern COUNT_PATTERN =
            Pattern.compile(COUNT_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String ALL_FIELDS_PATTERN_STR =
            "FIELDS\\(\\s*\\b(ALL|STANDARD|CUSTOM)\\b\\s*\\)";
    private static final Pattern ALL_FIELDS_PATTERN =
            Pattern.compile(ALL_FIELDS_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String GROUP_BY_PATTERN_STR = "GROUP\\s+BY\\s+\\b([^\\s]*)\\b";
    private static final Pattern GROUP_BY_PATTERN =
            Pattern.compile(GROUP_BY_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String ORDER_BY_PATTERN_STR =
            "ORDER\\s+BY\\s+\\b([^\\s]*)\\b"; // Works only on a single field at this point
    private static final Pattern ORDER_BY_PATTERN =
            Pattern.compile(ORDER_BY_PATTERN_STR, Pattern.CASE_INSENSITIVE);
    private static final String LIMIT_1_PATTERN_STR = "LIMIT\\s+1\\b";
    private static final Pattern LIMIT_1_PATTERN =
            Pattern.compile(LIMIT_1_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    private static final String RECORD_TYPE_OBJECT = "RecordType";
    private static final String SOBJECT_TYPE_FIELD = "SObjectType";
    private static final String SOBJECT_TYPE_VALUE_PATTERN_STR = "SObjectType\\s*=\\s*'(\\S+)'";
    private static final Pattern SOBJECT_TYPE_VALUE_PATTERN =
            Pattern.compile(SOBJECT_TYPE_VALUE_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    public static final TreeSet<String> ALWAYS_ACCESSIBLE_FIELDS =
            CollectionUtil.newTreeSetOf("Id");

    /** Detects object name of a complex query represented by a Set of SoqlQueryInfo */
    public static String getObjectName(HashSet<SoqlQueryInfo> queryInfos) {
        if (!queryInfos.isEmpty()) {
            final Optional<SoqlQueryInfo> queryInfoOptional = getOuterMostQueryInfo(queryInfos);
            if (queryInfoOptional.isPresent()) {
                final SoqlQueryInfo queryInfo = queryInfoOptional.get();
                // if we know that the only field invoked is count(), we should return Integer
                if (isCountQuery(queryInfo)) {
                    return TypeInfos.INTEGER.getApexName();
                }
                return queryInfo.getObjectName();
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("No outermost query identified in QueryInfo: " + queryInfos);
                }
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("No queryInfo items in list: " + queryInfos);
            }
        }
        return UNKNOWN;
    }

    /**
     * @return true if the Soql query looks up only COUNT()
     */
    public static boolean isCountQuery(SoqlQueryInfo queryInfo) {
        return queryInfo.isCount() && queryInfo.getFields().isEmpty() && !queryInfo.isAllFields();
    }

    /**
     * @return true if the outermost query returns a single SObject
     */
    public static boolean isSingleSObject(HashSet<SoqlQueryInfo> queryInfos) {
        final Optional<SoqlQueryInfo> outerMostQueryInfo = getOuterMostQueryInfo(queryInfos);
        if (outerMostQueryInfo.isPresent()) {
            return isSingleSObject(outerMostQueryInfo.get());
        }
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("No outermost query identified in QueryInfo: " + queryInfos);
        }

        // Since we are not sure, return false
        return false;
    }

    /**
     * @return true if the query has a Limit 1 clause
     */
    public static boolean isSingleSObject(SoqlQueryInfo queryInfo) {
        return queryInfo.isLimit1();
    }

    /**
     * Parses a complex query
     *
     * @param uncleanQuery with additional whitespaces and newlines
     * @return a Set of SoqlQueryInfo where each SoqlQueryInfo represents a simple portion of the
     *     Soql query.
     */
    public static HashSet<SoqlQueryInfo> parseQuery(String uncleanQuery) {
        final HashSet<SoqlQueryInfo> queryInfos = new HashSet<>();
        String outerQuery = cleanupQuery(uncleanQuery);

        queryInfos.addAll(getInnerQueries(outerQuery, true, false, false));
        return regroupByObject(queryInfos);
    }

    static HashSet<SoqlQueryInfo> regroupByObject(HashSet<SoqlQueryInfo> queryInfos) {
        return ObjectFieldUtil.regroupByObject(queryInfos);
    }

    private static Optional<SoqlQueryInfo> getOuterMostQueryInfo(
            HashSet<SoqlQueryInfo> queryInfos) {
        for (SoqlQueryInfo queryInfo : queryInfos) {
            if (queryInfo.isOutermost()) {
                return Optional.of(queryInfo);
            }
        }
        return Optional.empty();
    }

    private static HashSet<SoqlQueryInfo> getInnerQueries(
            String query, boolean isOutermost, boolean isSecurityEnforced, boolean isUserMode) {
        final HashSet<SoqlQueryInfo> queryInfos = new HashSet<>();
        String updatedQuery = query;

        // TODO: Handle contents within brackets when they are not a query. For example, we need to
        // handle a WHERE clause with "CONDITION1 AND (CONDITION2 OR CONDITION3)"
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) == STARTING_BRACKET) {
                final int indexOfStartingBracket = i;
                final Optional<String> innerQuery =
                        parseInnerQuery(query.substring(indexOfStartingBracket));
                if (innerQuery.isPresent()) {
                    final String innerQueryStr = innerQuery.get();

                    // Modify outer query to replace inner query portion
                    updatedQuery = updatedQuery.replace(innerQueryStr, INNER_QUERY);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Updated query: " + updatedQuery);
                    }

                    // recursively parse inner query to detect next level inner queries
                    queryInfos.addAll(
                            getInnerQueries(
                                    innerQueryStr,
                                    false,
                                    isSecurityEnforced(updatedQuery),
                                    isUserMode(updatedQuery)));
                }
            }
        }
        final SoqlQueryInfo soqlQueryInfo =
                getSoqlQueryInfo(updatedQuery, isOutermost, isSecurityEnforced, isUserMode);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Adding soqlQueryInfo: " + soqlQueryInfo);
        }
        // TODO: some inner queries get added twice. Put a control check to skip adding if it has
        // been handled already
        queryInfos.add(soqlQueryInfo);

        return queryInfos;
    }

    private static SoqlQueryInfo getSoqlQueryInfo(
            String query,
            boolean isOutermost,
            boolean outerIsSecurityEnforced,
            boolean outerIsUserMode) {
        final TreeSet<String> fields = parseFields(query);
        final String objectName = getObjectName(query, fields);

        // WITH SECURITY_ENFORCED clause at outer level is applicable to inner level as well,
        // there's insufficient documentation to state if the other way is true.
        // To be more restrictive, we won't count WITH SECURITY_ENFORCED clause for an inner query
        // as a protection to outer query.
        // WITH USER_MODE clause is only available at outer level, and is applicable to inner levels
        // as well.
        return new SoqlQueryInfo(
                query,
                objectName,
                fields,
                hasAllFields(query),
                hasCountField(query),
                isLimit1(query),
                // Use securityEnforced value of outer query if inner query doesn't have the clause
                outerIsSecurityEnforced || isSecurityEnforced(query),
                isOutermost,
                outerIsUserMode || isUserMode(query));
    }

    private static String getObjectName(String query, TreeSet<String> fields) {
        final String objectNameInQuery = parseObjectName(query);

        // Handle special case where RecordType is queried
        if (RECORD_TYPE_OBJECT.equalsIgnoreCase(objectNameInQuery)) {
            if (fields.contains(SOBJECT_TYPE_FIELD)) {
                // Get value of SObjectType field
                // For now, the check is very specific for a structure in WHERE clause like:
                // SObjectType = 'Account'
                // Based on what we encounter, expand to handle other structures as well.
                // TODO: make the check specific to WHERE clause. This makes an assumption that a
                // value equals wouldn't
                // occur in a part of the query that's not the WHERE clause.
                final Matcher matcher = SOBJECT_TYPE_VALUE_PATTERN.matcher(query);
                if (matcher.find()) {
                    // Remove SObjectType from field list so that this isn't treated like other
                    // fields
                    fields.remove(SOBJECT_TYPE_FIELD);
                    // Return the value check against SObjectType. In the above example, we'll
                    // return "Account".
                    return matcher.group(1);
                }
            }
        }

        return objectNameInQuery;
    }

    private static Optional<String> parseInnerQuery(String query) {
        int indexOfMatchingEndingBracket = -1; // we'll find this value now
        int matchTracker = 0; // to maintain the level of matching
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) == STARTING_BRACKET) {
                matchTracker++;
            } else if (query.charAt(i) == ENDING_BRACKET) {
                matchTracker--;
                if (matchTracker == 0) {
                    // we've found the matching end bracket
                    indexOfMatchingEndingBracket = i;
                    break;
                }
            }
        }

        if (matchTracker != 0) {
            throw new UnexpectedException("Query does not have balanced parantheses: " + query);
        }

        final String contentsInBrackets = query.substring(1, indexOfMatchingEndingBracket);

        // Make sure we have a subquery and not a count() or all_fields(standard)
        if (FIELD_PATTERN.matcher(contentsInBrackets).find()) {
            return Optional.of(contentsInBrackets);
        }

        return Optional.empty();
    }

    private static String cleanupQuery(String uncleanQuery) {
        // clean query to remove newlines and tab characters
        return uncleanQuery.replaceAll("[\\\n\\\t]", " ");
    }

    private static String parseObjectName(String query) {
        try {
            // TODO: support comma separated list of multiple objects with aliases
            final Matcher objectMatcher = OBJECT_PATTERN.matcher(query);
            if (!objectMatcher.find()) {
                throw new UnexpectedException("Query should always have a FROM clause: " + query);
            }

            final MatchResult matchResult = objectMatcher.toMatchResult();
            if (matchResult.groupCount() != 1) {
                throw new UnexpectedException("Query can contain only one FROM clause:" + query);
            }

            final String objectName = matchResult.group(1);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object name on query: " + objectName);
            }

            if (objectName.contains(",")) {
                throw new TodoException(
                        "Querying multiple objects is not supported yet: " + objectName);
            }

            return objectName;
        } catch (IllegalStateException ex) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unable to determine object name.", ex);
            }
            return UNKNOWN;
        }
    }

    private static String[] extractFields(String query) {
        try {
            final Matcher fieldMatcher = FIELD_PATTERN.matcher(query);

            if (!fieldMatcher.find()) {
                throw new UnexpectedException("Query should have fields: " + query);
            }

            final MatchResult matchResult = fieldMatcher.toMatchResult();
            if (matchResult.groupCount() != 1) {
                throw new UnexpectedException(
                        "Field matcher should have one entry with full line and one entry for the group matched");
            }

            final String matchedFields = matchResult.group(1).trim();
            // TODO: if we get a *, clean it before adding to list
            final String[] fields = matchedFields.split("\\s*,\\s*");
            return fields;
        } catch (IllegalStateException ex) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unable to extract fields.", ex);
            }
            return new String[] {};
        }
    }

    private static TreeSet<String> parseFields(String query) {
        final TreeSet<String> fields = CollectionUtil.newTreeSet();
        for (String field : extractFields(query)) {
            final Optional<String> fieldOptional = processField(field);
            if (fieldOptional.isPresent()) {
                fields.add(fieldOptional.get());
            }
        }
        fields.addAll(determineFieldsInWhereClause(query));
        fields.addAll(determineFieldsInGroupBy(query));
        fields.addAll(determineFieldsInOrderBy(query));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Fields on query: " + fields);
        }
        return fields;
    }

    /** check if we have a count() */
    private static boolean isCountField(String field) {
        final Matcher countMatcher = COUNT_PATTERN.matcher(field);
        return countMatcher.find();
    }

    /** check if we have a fields() */
    private static boolean isAllFieldsField(String field) {
        final Matcher allFieldsMatcher = ALL_FIELDS_PATTERN.matcher(field);
        return allFieldsMatcher.find();
        // TODO: allFieldsMatch.group(1) will have information on ALL, STANDARD, CUSTOM if we want
        // to process further
    }

    private static boolean hasAllFields(String query) {
        // TODO: This wouldn't work for inner/outer query situation when only one of them has
        // ALL_FIELDS()
        return isAllFieldsField(query);
    }

    private static boolean hasCountField(String query) {
        // TODO: This wouldn't work for inner/outer query situation when only one of them has
        // COUNT()
        return isCountField(query);
    }

    private static boolean isInnerQuery(String field) {
        return field.contains(INNER_QUERY);
    }

    private static Optional<String> processField(String field) {
        // skip (INNER_QUERY), count() and all_field()
        if (isCountField(field) || isAllFieldsField(field) || isInnerQuery(field)) {
            return Optional.empty();
        }
        return Optional.of(field);
    }

    private static List<String> determineFieldsInWhereClause(String query) {
        final List<String> fields = new ArrayList<>();
        // check if we have a Where clause
        final Matcher whereClauseMatcher = WHERE_PATTERN.matcher(query);
        if (whereClauseMatcher.find()) {
            if (whereClauseMatcher.groupCount() > 0) {
                final String whereClauseField = whereClauseMatcher.group(1);
                if (!isInnerQuery(whereClauseField)) {
                    fields.add(whereClauseField);
                }
            }

            // Check if there are AND/OR portions and capture their fields
            final Matcher extendedWhereClauseMatcher = EXTENDED_WHERE_PATTERN.matcher(query);

            // Using a WHILE loop to look at all the matches
            while (extendedWhereClauseMatcher.find()) {
                if (extendedWhereClauseMatcher.groupCount() > 0) {
                    final String extendedWhereClauseField = extendedWhereClauseMatcher.group(1);
                    if (!isInnerQuery(extendedWhereClauseField)) {
                        fields.add(extendedWhereClauseField);
                    }
                }
            }
        }

        return fields;
    }

    private static List<String> determineFieldsInGroupBy(String query) {
        List<String> fields = new ArrayList<>();

        // check if we have a Group BY
        final Matcher groupByMatcher = GROUP_BY_PATTERN.matcher(query);
        if (groupByMatcher.find()) {
            // TODO: Handle ROLL UP fields

            if (groupByMatcher.groupCount() > 0) {
                final String groupByField = groupByMatcher.group(1);
                // TODO: will we ever need to track groupBy fields separate from the queried fields?
                fields.add(groupByField);
            } else {
                throw new UnexpectedException("No field found on Group By clause: " + query);
            }
        }
        return fields;
    }

    private static boolean isLimit1(String query) {
        return LIMIT_1_PATTERN.matcher(query).find();
    }

    private static boolean isSecurityEnforced(String query) {
        // TODO: evaluate where the phrase occurs
        return WITH_SECURITY_ENFORCED_PATTERN.matcher(query).find();
    }

    private static boolean isUserMode(String query) {
        return WITH_USER_MODE_PATTERN.matcher(query).find();
    }

    private static List<String> determineFieldsInOrderBy(String query) {
        final List<String> fields = new ArrayList<>();

        // check if we have an ORDER BY
        final Matcher orderByMatcher = ORDER_BY_PATTERN.matcher(query);
        if (orderByMatcher.find()) {

            if (orderByMatcher.groupCount() > 0) {
                final String orderByField = orderByMatcher.group(1);
                fields.add(orderByField);
            } else {
                throw new UnexpectedException("No field found on Order By clause: " + query);
            }
        }
        return fields;
    }

    private SoqlParserUtil() {}
}

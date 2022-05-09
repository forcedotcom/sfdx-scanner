package com.salesforce.graph.ops;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.google.common.collect.ImmutableList;
import com.salesforce.apex.StandardLibraryLoader;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.JSONDeserializeFactory;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.VariableExpressionApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexBooleanValueFactory;
import com.salesforce.graph.symbols.apex.ApexCustomValueFactory;
import com.salesforce.graph.symbols.apex.ApexEnumValueFactory;
import com.salesforce.graph.symbols.apex.ApexFieldDescribeMapValueFactory;
import com.salesforce.graph.symbols.apex.ApexFieldSetDescribeMapValueFactory;
import com.salesforce.graph.symbols.apex.ApexIdValueFactory;
import com.salesforce.graph.symbols.apex.ApexSimpleValueFactory;
import com.salesforce.graph.symbols.apex.ApexSoqlValueFactory;
import com.salesforce.graph.symbols.apex.ApexStringValueFactory;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.SObjectAccessDecisionFactory;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResultFactory;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResultFactory;
import com.salesforce.graph.symbols.apex.schema.SObjectFieldFactory;
import com.salesforce.graph.symbols.apex.schema.SObjectTypeFactory;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Utilities for handling Standard Apex types. Apex Standard types are imported into the graph using
 * Apex class stubs. These stubs are kept in the src/main/resources/StandardApexLibrary folder.
 */
public final class ApexStandardLibraryUtil {
    // TODO: Alphabetize
    /** Functional interfaces that convert method calls to standard types */
    private static final List<MethodCallApexValueBuilder> METHOD_CALL_BUILDER_FUNCTIONS =
            ImmutableList.of(
                    DescribeSObjectResultFactory.METHOD_CALL_BUILDER_FUNCTION,
                    DescribeFieldResultFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexFieldDescribeMapValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexFieldSetDescribeMapValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexStringValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexSoqlValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexCustomValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexBooleanValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    SObjectAccessDecisionFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexIdValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexSimpleValueFactory.METHOD_CALL_BUILDER_FUNCTION,
                    JSONDeserializeFactory.METHOD_CALL_BUILDER_FUNCTION,
                    ApexEnumValueFactory.METHOD_CALL_BUILDER_FUNCTION);

    /** Functional interfaces that convert variable expressions to standard types */
    private static final List<VariableExpressionApexValueBuilder>
            VARIABLE_EXPRESSION_BUILDER_FUNCTIONS =
                    ImmutableList.of(
                            ApexEnumValueFactory.VARIABLE_EXPRESSION_BUILDER_FUNCTION,
                            ApexStringValueFactory.VARIABLE_EXPRESSION_BUILDER_FUNCTION,
                            DescribeSObjectResultFactory.VARIABLE_EXPRESSION_BUILDER_FUNCTION,
                            DescribeFieldResultFactory.VARIABLE_EXPRESSION_BUILDER_FUNCTION,
                            SObjectFieldFactory.VARIABLE_EXPRESSION_BUILDER_FUNCTION,
                            SObjectTypeFactory.VARIABLE_EXPRESSION_BUILDER_FUNCTION);

    private static final TreeSet<String> STANDARD_OBJECT_NAMES;
    private static final TreeSet<String> S_OBJECTS_REQUIRING_FLS;
    private static final TreeSet<String> S_OBJECTS_REQUIRING_CRUD;
    private static final TreeSet<String> SYSTEM_READ_ONLY_OBJECT_NAMES;

    private static final String LIST_FORMAT = "List<%s>";
    private static final String MAP_FORMAT = "Map<%s,%s>";
    private static final String SET_FORMAT = "Set<%s>";

    /**
     * Variable names that can be referenced without being declared, i.e. MyObject__c.SObjectType
     */
    public static final class VariableNames {
        public static final String FIELDS = "fields";
        public static final String FIELD_SETS = "fieldSets";
        public static final String SCHEMA = "Schema";
        public static final String S_OBJECT_TYPE = "SObjectType";
    }

    /** Standard Apex types */
    public static final class Type {
        public static final String SCHEMA_DESCRIBE_FIELD_RESULT = "Schema.DescribeFieldResult";
        public static final String SCHEMA_DESCRIBE_S_OBJECT_RESULT = "Schema.DescribeSObjectResult";
        public static final String SCHEMA_FIELD_SET = "Schema.FieldSet";
        public static final String SCHEMA_FIELD_SET_MEMBER = "Schema.FieldSetMember";
        public static final String SCHEMA_RECORD_TYPE_INFO = "Schema.RecordTypeInfo";
        public static final String SCHEMA_S_OBJECT_FIELD = "Schema.SObjectField";
        public static final String SCHEMA_S_OBJECT_TYPE = "Schema.SObjectType";
        public static final String SYSTEM_ACCESS_TYPE = "System.AccessType";
        public static final String SYSTEM_SCHEMA = "System.Schema";
        public static final String STRING = TypeInfos.STRING.getApexName();
        public static final String SYSTEM_USER_INFO = "System.UserInfo";
        public static final String SYSTEM_S_OBJECT_ACCESS_DECISION = "System.SObjectAccessDecision";
        public static final String S_OBJECT = "SObject";
        public static final String OBJECT = "Object";
    }

    static {
        // Includes both read/write SObjects and readonly System objects
        STANDARD_OBJECT_NAMES = SObjectListReader.getStandardObjects();

        // SObjects that need FLS check
        S_OBJECTS_REQUIRING_FLS = SObjectListReader.getSObjectsRequiringFlsCheck();

        // SObjects that need CRUD check
        S_OBJECTS_REQUIRING_CRUD = SObjectListReader.getSObjectsRequiringCrudCheck();

        // System readonly objects.
        // Derived by filtering out objects that require CRUD/FLS from the master standard object
        // list.
        SYSTEM_READ_ONLY_OBJECT_NAMES =
                CollectionUtil.newTreeSetOf(
                        STANDARD_OBJECT_NAMES.stream()
                                .filter(
                                        objName ->
                                                !S_OBJECTS_REQUIRING_FLS.contains(objName)
                                                        && !S_OBJECTS_REQUIRING_CRUD.contains(
                                                                objName))
                                .collect(Collectors.toList()));
    }

    /** @return {@link ApexValue} if this vertex references a standard type */
    public static Optional<ApexValue<?>> getStandardType(VariableExpressionVertex.Unknown vertex) {
        for (VariableExpressionApexValueBuilder function : VARIABLE_EXPRESSION_BUILDER_FUNCTIONS) {
            Optional<ApexValue<?>> result = function.apply(vertex);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /** @return {@link ApexValue} if this vertex generates a standard type */
    public static Optional<ApexValue<?>> getStandardType(
            GraphTraversalSource g, MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        for (MethodCallApexValueBuilder function : METHOD_CALL_BUILDER_FUNCTIONS) {
            Optional<ApexValue<?>> result = function.apply(g, vertex, symbols);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Some objects can be referred to by multiple names. This will convert the defining type to the
     * canonical name if it exists, if not the original value is returned. i.e. {@link
     * ApexStandardLibraryUtil.VariableNames#S_OBJECT_TYPE} is converted to {@link
     * ApexStandardLibraryUtil.Type#SCHEMA_S_OBJECT_TYPE}. This method will also convert values to
     * the correct case, i.e "SCHEMA.SOBJECTTYPE" is converted to "Schema.SObjectType". This method
     * works for all root vertex types. See {@link
     * com.salesforce.apex.jorje.ASTConstants.NodeType#ROOT_VERTICES}
     */
    public static String getCanonicalName(String definingType) {
        return StandardLibraryLoader.getCanonicalName(definingType);
    }

    /**
     * Converts declarations such as "String[]" to "List&lt;String&gt;"
     *
     * @return converted value if defining type is an array, else empty
     */
    public static Optional<String> convertArrayToList(String definingTypeParam) {
        final String definingType = StringUtil.stripAllSpaces(definingTypeParam);
        if (definingType.endsWith("[]")) {
            final String containedType = definingType.substring(0, definingType.length() - 2);
            final String listType =
                    ApexStandardLibraryUtil.getListDeclaration(getCanonicalName(containedType));
            return Optional.of(listType);
        } else {
            return Optional.empty();
        }
    }

    public static boolean isStandardObject(String definingType) {
        return STANDARD_OBJECT_NAMES.contains(definingType);
    }

    public static boolean isSystemReadOnlyObject(String definingType) {
        return SYSTEM_READ_ONLY_OBJECT_NAMES.contains(definingType);
    }

    public static String getListDeclaration(String subType) {
        return String.format(LIST_FORMAT, subType);
    }

    public static String getSetDeclaration(String subType) {
        return String.format(SET_FORMAT, subType);
    }

    public static String getMapDeclaration(String keyType, String valueType) {
        return String.format(MAP_FORMAT, keyType, valueType);
    }

    private ApexStandardLibraryUtil() {}

    private static final class SObjectListReader {
        // Path relative to src/main/resources
        private static final String DIRECTORY = "sObjects";
        private static final String S_OBJECT_CRUD_LIST_FILENAME =
                Paths.get(DIRECTORY, "sObjectCrudList.txt").toString();
        private static final String S_OBJECT_FLS_LIST_FILENAME =
                Paths.get(DIRECTORY, "sObjectFlsList.txt").toString();
        private static final String S_OBJECT_LIST_FILENAME =
                Paths.get(DIRECTORY, "sObjectList.txt").toString();

        private SObjectListReader() {}

        /**
         * Reads the list of standard objects and returns a TreeSet of standard object names. The
         * object names were taken from Salesforce public documentation at:
         * https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_list.htm
         */
        static TreeSet<String> getStandardObjects() {
            return getFileAsTreeSet(S_OBJECT_LIST_FILENAME);
        }

        /** @return subset of {@link #getStandardObjects()} with objects that require FLS check */
        static TreeSet<String> getSObjectsRequiringFlsCheck() {
            return getFileAsTreeSet(S_OBJECT_FLS_LIST_FILENAME);
        }

        /** @return subset of {@link #getStandardObjects()} with objects that require CRUD check */
        static TreeSet<String> getSObjectsRequiringCrudCheck() {
            return getFileAsTreeSet(S_OBJECT_CRUD_LIST_FILENAME);
        }

        private static TreeSet<String> getFileAsTreeSet(String listFilename) {
            final List<String> stringList = getFileContents(listFilename);
            final TreeSet<String> sObjects = CollectionUtil.newTreeSetOf(stringList);
            // Remove entry for empty lines
            sObjects.remove("");

            return sObjects;
        }

        private static List<String> getFileContents(String filename) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try (InputStream inputStream = classLoader.getResourceAsStream(filename)) {
                return new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new UnexpectedException(
                        "Developer error: Could not read resource file " + filename, e);
            }
        }
    }
}

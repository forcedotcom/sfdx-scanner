package com.salesforce.rules;

import com.salesforce.graph.EnumUtil;
import java.util.Optional;
import java.util.TreeMap;

public class DmlUtil {

    // we don't ever want this class to be initialized
    private DmlUtil() {}

    public enum DatabaseOperation {
        CONVERT_LEAD("Database.convertLead", true),
        COUNT_QUERY("Database.countQuery", true),
        COUNT_QUERY_WITH_BINDS("Database.countQueryWithBinds", true),
        DELETE("Database.delete", true),
        DELETE_ASYNC("Database.deleteAsync", true),
        DELETE_IMMEDIATE("Database.deleteImmediate", true),
        EMPTY_RECYCLE_BIN("Database.emptyRecycleBin", true),
        EXECUTE_BATCH("Database.executeBatch", true),
        GET_ASYNC_DELETE_RESULT("Database.getAsyncDeleteResult", true),
        GET_ASYNC_LOCATOR("Database.getAsyncLocator", true),
        GET_ASYNC_SAVE_RESULT("Database.getAsyncSaveResult", true),
        GET_DELETED("Database.getDeleted", true),
        GET_QUERY_LOCATOR("Database.getQueryLocator", true),
        GET_QUERY_LOCATOR_WITH_BINDS("Database.getQueryLocatorWithBinds", true),
        GET_UPDATED("Database.getUpdated", true),
        INSERT("Database.insert", true),
        INSERT_ASYNC("Database.insertAsync", true),
        INSERT_IMMEDIATE("Database.insertImmediate", true),
        MERGE("Database.merge", true),
        QUERY("Database.query", true),
        QUERY_WITH_BINDS("Database.queryWithBinds", true),
        ROLLBACK("Database.rollback", true),
        SET_SAVEPOINT("Database.setSavepoint", true),
        UNDELETE("Database.undelete", true),
        UPDATE("Database.update", true),
        UPDATE_ASYNC("Database.updateAsync", true),
        UPDATE_IMMEDIATE("Database.updateImmediate", true),
        UPSERT("Database.upsert", true);

        // Create a map of database operation method -> DatabaseOperation for fromString method
        private static final TreeMap<String, DatabaseOperation> FROM_DATABASE_METHOD =
                EnumUtil.getEnumTreeMap(
                        DatabaseOperation.class, DatabaseOperation::getDatabaseOperationMethod);
        private final String databaseOperationMethod;
        private final boolean loopIsViolation;

        /**
         * @param databaseOperationMethod the method name of the Database method. For example,
         *     <code>Database.insertAsync</code>.
         * @param loopIsViolation whether this operation in a loop is a violation of {@link
         *     DmlInLoopRule}
         */
        DatabaseOperation(String databaseOperationMethod, boolean loopIsViolation) {

            this.databaseOperationMethod = databaseOperationMethod;
            this.loopIsViolation = loopIsViolation;
        }

        public String getDatabaseOperationMethod() {
            return this.databaseOperationMethod;
        }

        /**
         * @return true if this operation should be a violation in {@link DmlInLoopRule}, false
         *     otherwise.
         */
        public boolean isLoopIsViolation() {
            return loopIsViolation;
        }

        /**
         * Convert a Databse.[method] name to a DatabaseOperation
         *
         * @param method the method name to convert. For example, <code>Database.update</code>
         * @return the corresponding DatabaseOperation, if found
         */
        public static Optional<DatabaseOperation> fromString(String method) {
            return Optional.ofNullable(FROM_DATABASE_METHOD.get(method));
        }
    }
}

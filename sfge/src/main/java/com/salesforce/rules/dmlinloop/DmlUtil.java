package com.salesforce.rules.dmlinloop;

import com.salesforce.graph.EnumUtil;
import com.salesforce.rules.DmlInLoopRule;
import java.util.Optional;
import java.util.TreeMap;

public final class DmlUtil {

    // we don't ever want this class to be initialized
    private DmlUtil() {}

    /**
     * An enumeration of all of the possible Database.method() methods in the Database class in
     * Apex.
     */
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

        /** map of database operation method strings -> DatabaseOperation for fromString method */
        private static final TreeMap<String, DatabaseOperation> METHOD_NAME_TO_DATABASE_OPERATION =
                EnumUtil.getEnumTreeMap(
                        DatabaseOperation.class, DatabaseOperation::getDatabaseOperationMethod);
        /** stores the string representation of this method */
        private final String databaseOperationMethod;
        /** stores if this method should be a violation when in a loop */
        private final boolean violationInLoop;

        /**
         * @param databaseOperationMethod the method name of the Database method. For example,
         *     <code>Database.insertAsync</code>.
         * @param violationInLoop whether this operation in a loop is a violation of {@link
         *     DmlInLoopRule}
         */
        DatabaseOperation(String databaseOperationMethod, boolean violationInLoop) {
            this.databaseOperationMethod = databaseOperationMethod;
            this.violationInLoop = violationInLoop;
        }

        /**
         * get the string representation of the associated method. For example, Databse.query()
         * returns Database.query.
         */
        public String getDatabaseOperationMethod() {
            return this.databaseOperationMethod;
        }

        /**
         * @return true if this operation should be a violation in {@link DmlInLoopRule}, false
         *     otherwise.
         */
        public boolean isViolationInLoop() {
            return violationInLoop;
        }

        /**
         * Convert a Databse.method() name to a DatabaseOperation
         *
         * @param method the name of the method to convert. For example, <code>Database.update
         *     </code>
         * @return the corresponding DatabaseOperation, if found
         */
        public static Optional<DatabaseOperation> fromString(String method) {
            return Optional.ofNullable(METHOD_NAME_TO_DATABASE_OPERATION.get(method));
        }
    }
}

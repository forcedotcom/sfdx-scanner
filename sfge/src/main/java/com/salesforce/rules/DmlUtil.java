package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;

public class DmlUtil {

    // we don't ever want this class to be initialized
    private DmlUtil() {}

    public enum DmlOperation {
        DELETE(ASTConstants.NodeType.DML_DELETE_STATEMENT, "Database.delete"),
        INSERT(ASTConstants.NodeType.DML_INSERT_STATEMENT, "Database.insert"),
        MERGE(ASTConstants.NodeType.DML_MERGE_STATEMENT, "Database.merge"),
        READ(ASTConstants.NodeType.SOQL_EXPRESSION, "Database.query"),
        UNDELETE(ASTConstants.NodeType.DML_UNDELETE_STATEMENT, "Database.undelete"),
        UPDATE(ASTConstants.NodeType.DML_UPDATE_STATEMENT, "Database.update"),
        UPSERT(ASTConstants.NodeType.DML_UPSERT_STATEMENT, "Database.upsert"),
        ;


        /**
         * Statement type indicated in the AST. This is applicable only for direct DML statements
         * such as: READ: [SELECT Id, Name from Account] INSERT: insert account; UPDATE: update
         * account; DELETE: delete account;
         */
        private final String dmlStatementType;

        /**
         * DML operation invoked on Database type. Examples: READ: Database.query('SELECT Id, Name
         * from Account'); INSERT: Database.insert(accounts); UPDATE: Database.update(accounts);
         * DELETE: Database.delete(accounts);
         */
        private final String databaseOperationMethod;

        DmlOperation(String dmlStatementType, String databaseOperationMethod) {
            this.dmlStatementType = dmlStatementType;
            this.databaseOperationMethod = databaseOperationMethod;
        }

        public String getDmlStatementType() {
            return dmlStatementType;
        }

        public String getDatabaseOperationMethod() {
            return databaseOperationMethod;
        }

    }
}

package com.salesforce.rules.fls.apex.operations;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ops.SoqlParserUtil;

/**
 * FLS Violation when SFGE understands that a DML operation is happening, but it is unable to
 * determine more information about the object and the fields involved.
 */
public class UnresolvedCrudFlsViolationInfo extends FlsViolationInfo {
    public UnresolvedCrudFlsViolationInfo(FlsConstants.FlsValidationType validationType) {
        super(validationType, SoqlParserUtil.UNKNOWN, CollectionUtil.newTreeSet(), false);
    }

    public String getMessageTemplate() {
        return UserFacingMessages.UNRESOLVED_CRUD_FLS_TEMPLATE;
    }
}

package com.salesforce.rules.fls.apex.operations;

import com.salesforce.config.UserFacingMessages;
import java.util.TreeSet;

/**
 * Represents a warning about stripInaccessible check on Read operation, which lets the developer
 * know that SFGE doesn't have sufficient information and asks them to apply their best judgement.
 * For example:
 *
 * <p>{@code List<Account> unclean = [SELECT Id, Name FROM Account]; SObjectAccessDecision sd =
 * Security.stripInaccessible(AccessType.READABLE, unclean); List<Account> clean = sd.getRecords();
 * }
 *
 * <p>Now we don't have a way to confirm that unclean was indeed discarded and clean was only
 * displayed to users.
 */
public final class FlsStripInaccessibleWarningInfo extends FlsViolationInfo {
    public FlsStripInaccessibleWarningInfo(
            FlsConstants.FlsValidationType validationType,
            String objectName,
            TreeSet<String> fields,
            boolean isAllFields) {
        super(validationType, objectName, fields, isAllFields);
    }

    public String getMessageTemplate() {
        return UserFacingMessages.CrudFlsTemplates.STRIP_INACCESSIBLE_READ_WARNING_TEMPLATE;
    }
}

package com.salesforce.rules;

import com.salesforce.config.UserFacingMessages;

/** Internal representation of an occurrence info. */
public class OccurrenceInfo {
    final String label;
    final String definingType;
    final int lineNum;

    public OccurrenceInfo(String label, String definingType, int lineNum) {
        this.label = label;
        this.definingType = definingType;
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        return String.format(
                UserFacingMessages.OccurrenceInfoTemplates.OCCURRENCE_TEMPLATE,
                label,
                definingType,
                lineNum);
    }
}

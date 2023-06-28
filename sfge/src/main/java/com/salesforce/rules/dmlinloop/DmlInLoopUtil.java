package com.salesforce.rules.dmlinloop;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.OccurrenceInfo;

public class DmlInLoopUtil {

    /**
     * Retrieve the DML Rule violation message
     *
     * @param loopVertex the vertex that created the loop containing sinkName
     * @return
     */
    public static String getMessage(SFVertex loopVertex) {
        return String.format(
                UserFacingMessages.DmlInLoopRuleTemplates.MESSAGE_TEMPLATE,
                new OccurrenceInfo(
                        loopVertex.getLabel(),
                        loopVertex.getDefiningType(),
                        loopVertex.getBeginLine()));
    }

    /**
     * Retrieve the DML Rule violation message
     *
     * @param occurenceInfo the OccurenceInfo of the sink (DML statement)
     * @return
     */
    public static String getMessage(OccurrenceInfo occurenceInfo) {
        return String.format(
                UserFacingMessages.DmlInLoopRuleTemplates.MESSAGE_TEMPLATE, occurenceInfo);
    }
}

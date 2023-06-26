package com.salesforce.rules.dmlinloop;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.OccurrenceInfo;

public class DmlInLoopUtil {

    /**
     * Retrieve the DML Rule violation message
     * @param sinkName the name/label of the DML method/statement
     * @param loopVertex the vertex that created the loop containing sinkName
     * @return
     */
    public static String getMessage(String sinkName, SFVertex loopVertex) {
        return String.format(
            UserFacingMessages.DmlInLoopRuleTemplates.MESSAGE_TEMPLATE,
            sinkName,
            new OccurrenceInfo(
                loopVertex.getLabel(),
                loopVertex.getDefiningType(),
                loopVertex.getBeginLine()
            ));
    }

    /**
     * Retrieve the DML Rule violation message
     * @param sinkName the name/label of the DML method/statement
     * @param occurenceInfo the OccurenceInfo of the sink (DML statement)
     * @return
     */
    public static String getMessage( String sinkName, OccurrenceInfo occurenceInfo) {
        return String.format(
            UserFacingMessages.DmlInLoopRuleTemplates.MESSAGE_TEMPLATE,
            sinkName,
            occurenceInfo);
    }
}

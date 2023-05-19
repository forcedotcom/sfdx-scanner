package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Represents a vertex that can perform DML through Apex. */
public abstract class DmlStatementVertex extends BaseSFVertex {
    protected static final Logger LOGGER = LogManager.getLogger(DmlStatementVertex.class);

    private static final String ACCESS_LEVEL_REFERENCE = "AccessLevel";

    private enum AccessLevel {
        USER_MODE,
        SYSTEM_MODE
    }

    private final AccessLevel accessLevel;

    DmlStatementVertex(Map<Object, Object> properties) {
        super(properties);
        accessLevel = calculateAccessLevel(this);
    }

    private static AccessLevel calculateAccessLevel(DmlStatementVertex vertex) {
        // Default to System mode to begin with.
        AccessLevel accessLevel = AccessLevel.SYSTEM_MODE;

        // If AccessLevel is included in the syntax, it's usually the last child. It shows up in the
        // form of a VariableExpression with a ReferenceExpression child.

        // spotless: off
        // Example:
        //<DmlUpdateStatement BeginColumn="9" BeginLine="6" DefiningType="MyClass" DefiningType_CaseSafe="myclass" EndLine="6" EndScopes="[BlockStatement]" FirstChild="false" LastChild="true" childIdx="2">
        //  <VariableExpression BeginColumn="24" BeginLine="6" DefiningType="MyClass" DefiningType_CaseSafe="myclass" EndLine="6" FirstChild="true" LastChild="false" Name="a" Name_CaseSafe="a" childIdx="0">
        //    <EmptyReferenceExpression BeginColumn="24" BeginLine="6" DefiningType="MyClass" DefiningType_CaseSafe="myclass" EndLine="6" FirstChild="true" LastChild="true" childIdx="0"/>
        //  </VariableExpression>
        //  <VariableExpression BeginColumn="30" BeginLine="-1" DefiningType="MyClass" DefiningType_CaseSafe="myclass" EndLine="-1" FirstChild="false" LastChild="true" Name="USER_MODE" Name_CaseSafe="user_mode" childIdx="1">
        //    <ReferenceExpression BeginColumn="19" BeginLine="6" DefiningType="MyClass" DefiningType_CaseSafe="myclass" EndLine="6" FirstChild="true" LastChild="true" Name="AccessLevel" Name_CaseSafe="accesslevel" Names="[AccessLevel]" ReferenceType="LOAD" childIdx="0"/>
        //  </VariableExpression>
        //</DmlUpdateStatement>

        // spotless: on

        final List<VariableExpressionVertex> children =
                vertex.getChildren(ASTConstants.NodeType.VARIABLE_EXPRESSION);
        if (children.size() > 0) {
            final VariableExpressionVertex lastChild = children.get(children.size() - 1);
            ReferenceExpressionVertex referenceExpression =
                    lastChild.getOnlyChildOrNull(ASTConstants.NodeType.REFERENCE_EXPRESSION);
            if (referenceExpression != null) {
                if (ACCESS_LEVEL_REFERENCE.equalsIgnoreCase(referenceExpression.getName())) {
                    // lastChild's name holds AccessLevel value
                    final String accessLevelValueString = lastChild.getName();
                    final AccessLevel accessLevelValue =
                            AccessLevel.valueOf(accessLevelValueString);
                    if (accessLevelValue != null) {
                        accessLevel = accessLevelValue;
                    } else {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(
                                    "AccessLevel is unknown. accessLevelValueString="
                                            + accessLevelValueString);
                        }
                    }
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "Unknown ReferenceExpression name. referenceExpression="
                                        + referenceExpression);
                    }
                }
            }
        }
        return accessLevel;
    }

    /**
     * @return true if the DML statement has System access level.
     */
    public boolean isSystemMode() {
        return accessLevel == AccessLevel.SYSTEM_MODE;
    }
}

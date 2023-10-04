package com.salesforce.apex.jorje;

import apex.jorje.data.Location;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementors of this interface are an intermediate representation between Jorje classes and
 * values that are inserted into the graph. Most implementations derive from {@link AstNodeWrapper},
 * others such as {@link EngineDirectiveNode} aren't associated with an {@link
 * apex.jorje.semantic.ast.AstNode}
 */
public interface JorjeNode {
    /** Dispatch to a {@link JorjeNodeVisitor} */
    void accept(JorjeNodeVisitor visitor);

    /*
     * All methods below represent the graph hierarchy and properties that are inserted by
     * {@link com.salesforce.graph.build.AbstractApexVertexBuilder}
     */

    List<JorjeNode> getChildren();

    String getLabel();

    String getDefiningType();

    String getName();

    Map<String, Object> getProperties();

    Optional<JorjeNode> getParent();

    Location getLocation();

    void addChild(JorjeNode child);

    void setMetaInformation(PositionInformation positionInformation);

    void computeChildIndices(int startOffset, boolean expectingSubsequentChildren);

    void setChildIndex(int childIndex);

    int getChildIndex();

    void setFirstChild(boolean firstChild);

    void setLastChild(boolean lastChild);

    Integer getBeginLine();

    Integer getEndLine();

    Integer getBeginColumn();
}

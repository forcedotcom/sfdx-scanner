package com.salesforce.apex.jorje;

import apex.jorje.data.Location;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.Nullable;

/**
 * Common implementation for {@link JorjeNode} subclasses that may or may not correspond to an
 * {@link AstNodeWrapper}
 */
abstract class AbstractJorjeNode implements JorjeNode {
    private final JorjeNode parent;
    private final List<JorjeNode> children;

    // Positional information
    private Integer beginLine;
    private Integer endLine;
    private Integer beginColumn;

    // Hierarchy information
    private Integer childIndex;
    private Boolean firstChild;
    private Boolean lastChild;

    protected AbstractJorjeNode(@Nullable JorjeNode parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    /** Overridden by subclasses to add their specific properties. */
    protected abstract void fillProperties(Map<String, Object> properties);

    @Override
    public final List<JorjeNode> getChildren() {
        return children;
    }

    @Override
    public void setChildIndex(int childIndex) {
        this.childIndex = childIndex;
    }

    @Override
    public int getChildIndex() {
        return this.childIndex;
    }

    @Override
    public void setFirstChild(boolean firstChild) {
        this.firstChild = firstChild;
    }

    @Override
    public void setLastChild(boolean lastChild) {
        this.lastChild = lastChild;
    }

    @Override
    public void addChild(JorjeNode child) {
        this.children.add(child);
    }

    @Override
    public Integer getBeginLine() {
        return beginLine;
    }

    @Override
    public Integer getEndLine() {
        return endLine;
    }

    @Override
    public Integer getBeginColumn() {
        return beginColumn;
    }

    @Override
    public final Map<String, Object> getProperties() {
        // Use a tree map for deterministic order, helps with debugging
        final Map<String, Object> properties = new TreeMap<>();

        // Let the subclass fill the properties first, this allows us to detect conflicts
        fillProperties(properties);

        putProperty(properties, Schema.DEFINING_TYPE, getDefiningType());
        putProperty(properties, Schema.BEGIN_LINE, getBeginLine());
        putProperty(properties, Schema.END_LINE, getEndLine());
        putProperty(properties, Schema.BEGIN_COLUMN, getBeginColumn());
        if (childIndex != null) {
            putProperty(properties, Schema.CHILD_INDEX, childIndex);
            putProperty(properties, Schema.FIRST_CHILD, firstChild);
            putProperty(properties, Schema.LAST_CHILD, lastChild);
        }

        return Collections.unmodifiableMap(properties);
    }

    @Override
    public final Optional<JorjeNode> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Invoked after the entire tree is built. Used to augment the nodes with extra information that
     * may not be present during tree creation.
     */
    @Override
    public final void setMetaInformation(PositionInformation positionInformation) {
        final Location location = getLocation();
        beginLine = positionInformation.getLineNumber(location.getStartIndex());
        endLine = positionInformation.getLineNumber(location.getEndIndex());
        beginColumn = location.getColumn();

        computeChildIndices(0, false);
    }

    /**
     * Computes (or recomputes) the indices associated with each of this node's children.
     *
     * @param startOffset Offset applied to the indices. Useful if synthetic vertices are inserted
     *     at the head of the list.
     * @param expectingSubsequentChildren If true, the final child's {@link #lastChild} property is
     *     set to false.
     */
    @Override
    public final void computeChildIndices(int startOffset, boolean expectingSubsequentChildren) {
        for (int i = 0; i < children.size(); i++) {
            final JorjeNode child = children.get(i);
            child.setChildIndex(i + startOffset);
            child.setFirstChild(i + startOffset == 0);
            child.setLastChild(!expectingSubsequentChildren && i == (children.size() - 1));
        }
    }

    private void putProperty(Map<String, Object> properties, String key, Object value) {
        final Object previous = properties.put(key, value);
        if (previous != null) {
            throw new UnexpectedException(
                    "Subclass added conflicting value. key="
                            + key
                            + ", previous="
                            + previous
                            + ", value="
                            + value);
        }
    }
}

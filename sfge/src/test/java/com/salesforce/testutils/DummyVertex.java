package com.salesforce.testutils;

import com.salesforce.graph.vertex.SFVertex;

/** Dummy vertex to use when we don't want to build a graph. */
public class DummyVertex implements SFVertex {
    private final String label;

    public DummyVertex(String label) {
        this.label = label;
    }

    @Override
    public Long getId() {
        return 123456L;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getDefiningType() {
        return "dummy";
    }

    @Override
    public Integer getChildIndex() {
        return 0;
    }

    @Override
    public Integer getBeginLine() {
        return 0;
    }

    @Override
    public Integer getEndLine() {
        return 0;
    }

    @Override
    public Integer getBeginColumn() {
        return 0;
    }

    @Override
    public String getFileName() {
        return "dummy";
    }
}

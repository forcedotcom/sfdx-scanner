package com.salesforce.graph.vertex;

public interface SFVertex {
    Long getId();

    String getLabel();

    String getDefiningType();

    Integer getChildIndex();

    Integer getBeginLine();

    Integer getEndLine();

    Integer getBeginColumn();

    String getFileName();
}

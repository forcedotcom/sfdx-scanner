package com.salesforce.graph;

import com.salesforce.apex.ApexEnum;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Provides information on Metadata tha has been loaded into the graph. */
public interface MetadataInfo {
    /**
     * Analyze the graph for MetadataInfo. Must only be called once.
     *
     * @throws com.salesforce.exception.UnexpectedException if called more than once
     */
    void initialize(GraphTraversalSource g);

    /**
     * Currently uses heuristics to identify the types of metadata in the graph. This will
     * eventually be replaced by parsing the metadata files and importing it into the graph.
     *
     * @return true if {@code name} refers to a piece of Metadata that has been identified as a
     *     CustomSetting
     */
    boolean isCustomSetting(String name);

    /** @return an {@link ApexEnum} if {@code name} refers to an Enum */
    Optional<ApexEnum> getEnum(String name);
}

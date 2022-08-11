package com.salesforce.rules.ops;

import com.salesforce.graph.ApexPath;
import com.salesforce.rules.Violation;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Observer that listens to activities happening in SFGE while analyzing source code.
 */
public interface ProgressListener {
    /**
     * Invoked when meta information about source code is collected
     * @param metaInfoType type of information
     * @param itemsCollected items that were collected
     */
    void collectedMetaInfo(String metaInfoType, TreeSet<String> itemsCollected);

    /**
     * Invoked when a file is compiled by Jorje.
     */
    void compiledAnotherFile();

    /**
     * Invoked when compilation is completed for all files.
     */
    void finishedFileCompilation();

    /**
     * Invoked when SFGE starts building the graph with information
     * found during compilation.
     */
    void startedBuildingGraph();

    /**
     * Invoked when SFGE completes building graph.
     */
    void completedBuildingGraph();

    /**
     * Invoked when entry points to analysis are identified.
     * @param pathEntryPointsCount number of entry points identified.
     */
    void pathEntryPointsIdentified(int pathEntryPointsCount);

    /**
     * Invoked when all the paths originating from an entry point are analyzed.
     * @param paths number of paths that originated from an entry point.
     * @param violations number of violations detected while walking the identified paths.
     */
    void finishedAnalyzingEntryPoint(List<ApexPath> paths, Set<Violation> violations);

    /**
     * Invoked when analysis is finished.
     */
    void completedAnalysis();
}

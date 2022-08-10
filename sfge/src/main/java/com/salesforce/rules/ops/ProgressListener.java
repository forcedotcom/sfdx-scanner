package com.salesforce.rules.ops;

import com.salesforce.graph.ApexPath;
import com.salesforce.rules.Violation;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public interface ProgressListener {
    void collectedMetaInfo(String metaInfoType, TreeSet<String> itemsCollected);

    void compiledAnotherFile();

    void finishedFileCompilation();

    void startedBuildingGraph();

    void completedBuildingGraph();

    void pathEntryPointsIdentified(int pathEntryPointsCount);

    void finishedAnalyzingEntryPoint(List<ApexPath> paths, Set<Violation> violations);

    void completedAnalysis();
}

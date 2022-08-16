package com.salesforce.rules.ops;

import com.salesforce.graph.ApexPath;
import com.salesforce.rules.Violation;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Test implementation of {@link ProgressListener} to use as a non-singleton with tests */
public class TestProgressListenerImpl implements ProgressListener {
    @Override
    public void collectedMetaInfo(String metaInfoType, TreeSet<String> itemsCollected) {
        // do nothing
    }

    @Override
    public void compiledAnotherFile() {
        // do nothing
    }

    @Override
    public void finishedFileCompilation() {
        // do nothing
    }

    @Override
    public void startedBuildingGraph() {
        // do nothing
    }

    @Override
    public void completedBuildingGraph() {
        // do nothing
    }

    @Override
    public void pathEntryPointsIdentified(int pathEntryPointsCount) {
        // do nothing
    }

    @Override
    public void finishedAnalyzingEntryPoint(List<ApexPath> paths, Set<Violation> violations) {
        // do nothing
    }

    @Override
    public void completedAnalysis() {
        // do nothing
    }
}

package com.salesforce.rules.ops;

import com.google.common.base.Joiner;
import com.salesforce.config.SfgeConfigProvider;
import com.salesforce.graph.ApexPath;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import com.salesforce.rules.Violation;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Publishes information to CLI on the progress of analysis. */
public class ProgressListenerImpl implements ProgressListener {

    private int filesCompiled = 0;
    private int pathsDetected = 0;
    private int violationsDetected = 0;
    private int entryPointsAnalyzed = 0;

    private final int progressIncrements;

    static ProgressListener getInstance() {
        return ProgressListenerImpl.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final ProgressListenerImpl INSTANCE = new ProgressListenerImpl();
    }

    private ProgressListenerImpl() {
        progressIncrements = SfgeConfigProvider.get().getProgressIncrementsOnVerbose();
    }

    @Override
    public void collectedMetaInfo(String metaInfoType, TreeSet<String> itemsCollected) {
        final String items =
                (itemsCollected.isEmpty()) ? "none found" : Joiner.on(',').join(itemsCollected);
        CliMessager.postMessage(
                "Meta information collected",
                EventKey.INFO_META_INFO_COLLECTED,
                metaInfoType,
                items);
    }

    @Override
    public void compiledAnotherFile() {
        filesCompiled++;
    }

    @Override
    public void finishedFileCompilation() {
        CliMessager.postMessage(
                "Finished compiling files",
                EventKey.INFO_COMPLETED_FILE_COMPILATION,
                String.valueOf(filesCompiled));
    }

    @Override
    public void startedBuildingGraph() {
        CliMessager.postMessage("Started building graph", EventKey.INFO_STARTED_BUILDING_GRAPH);
    }

    @Override
    public void completedBuildingGraph() {
        CliMessager.postMessage("Finished building graph", EventKey.INFO_COMPLETED_BUILDING_GRAPH);
    }

    @Override
    public void pathEntryPointsIdentified(int pathEntryPointsCount) {
        CliMessager.postMessage(
                "Path entry points identified",
                EventKey.INFO_PATH_ENTRY_POINTS_IDENTIFIED,
                String.valueOf(pathEntryPointsCount));
    }

    @Override
    public void finishedAnalyzingEntryPoint(List<ApexPath> paths, Set<Violation> violations) {
        pathsDetected += paths.size();
        violationsDetected += violations.size();
        entryPointsAnalyzed++;

        if (pathsDetected % progressIncrements == 0) {
            CliMessager.postMessage(
                    "Count of violations in paths, entry points",
                    EventKey.INFO_PATH_ANALYSIS_PROGRESS,
                    String.valueOf(violationsDetected),
                    String.valueOf(pathsDetected),
                    String.valueOf(entryPointsAnalyzed));
        }
    }

    @Override
    public void completedAnalysis() {
        CliMessager.postMessage(
                "Completed analysis stats",
                EventKey.INFO_COMPLETED_PATH_ANALYSIS,
                String.valueOf(pathsDetected),
                String.valueOf(entryPointsAnalyzed),
                String.valueOf(violationsDetected));
    }
}

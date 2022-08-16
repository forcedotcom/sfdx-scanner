package com.salesforce.rules.ops;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.salesforce.config.SfgeConfigProvider;
import com.salesforce.graph.ApexPath;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import com.salesforce.rules.Violation;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Publishes realtime information to CLI on the progress of analysis. */
public class ProgressListenerImpl implements ProgressListener {

    @VisibleForTesting static final String NONE_FOUND = "none found";

    private int filesCompiled = 0;
    private int pathsDetected = 0;
    private int lastPathCountReported = 0;
    private int violationsDetected = 0;
    private int entryPointsAnalyzed = 0;
    private int totalEntryPoints = 0;

    private final int progressIncrements;

    static ProgressListener getInstance() {
        return ProgressListenerImpl.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final ProgressListenerImpl INSTANCE = new ProgressListenerImpl();
    }

    @VisibleForTesting
    ProgressListenerImpl() {
        progressIncrements = SfgeConfigProvider.get().getProgressIncrements();
    }

    @Override
    public void collectedMetaInfo(String metaInfoType, TreeSet<String> itemsCollected) {
        final String items = stringify(itemsCollected);
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
        totalEntryPoints = pathEntryPointsCount;
        CliMessager.postMessage(
                "Path entry points identified",
                EventKey.INFO_PATH_ENTRY_POINTS_IDENTIFIED,
                String.valueOf(totalEntryPoints));
    }

    @Override
    public void finishedAnalyzingEntryPoint(List<ApexPath> paths, Set<Violation> violations) {
        pathsDetected += paths.size();
        violationsDetected += violations.size();
        entryPointsAnalyzed++;

        // Make a post only if we have more paths detected than the progress increments
        // since the last time we posted.
        if (pathsDetected - lastPathCountReported >= progressIncrements) {
            CliMessager.postMessage(
                    "Count of violations in paths, entry points",
                    EventKey.INFO_PATH_ANALYSIS_PROGRESS,
                    String.valueOf(violationsDetected),
                    String.valueOf(pathsDetected),
                    String.valueOf(entryPointsAnalyzed),
                    String.valueOf(totalEntryPoints));

            lastPathCountReported = pathsDetected;
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

    @VisibleForTesting
    String stringify(Collection<String> items) {
        return (items.isEmpty()) ? NONE_FOUND : Joiner.on(',').join(items);
    }

    @VisibleForTesting
    void reset() {
        filesCompiled = 0;
        pathsDetected = 0;
        lastPathCountReported = 0;
        violationsDetected = 0;
        entryPointsAnalyzed = 0;
    }

    @VisibleForTesting
    int getFilesCompiled() {
        return filesCompiled;
    }

    @VisibleForTesting
    int getPathsDetected() {
        return pathsDetected;
    }

    @VisibleForTesting
    int getLastPathCountReported() {
        return lastPathCountReported;
    }

    @VisibleForTesting
    int getViolationsDetected() {
        return violationsDetected;
    }

    @VisibleForTesting
    int getEntryPointsAnalyzed() {
        return entryPointsAnalyzed;
    }
}

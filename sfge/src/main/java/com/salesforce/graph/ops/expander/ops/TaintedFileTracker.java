package com.salesforce.graph.ops.expander.ops;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Tracks apex class files that were encountered while expanding paths. */
public class TaintedFileTracker implements PathExpansionObserver {
    final HashSet<String> taintedFiles;

    public TaintedFileTracker() {
        taintedFiles = new HashSet<>();
    }

    @Override
    public void onPathVisit(ApexPath path) {
        Optional<MethodVertex> methodOpt = path.getMethodVertex();
        if (methodOpt.isPresent()) {
            taintedFiles.add(methodOpt.get().getFileName());
        }
    }

    public Set<String> getTaintedFiles() {
        return Collections.unmodifiableSet(taintedFiles);
    }
}

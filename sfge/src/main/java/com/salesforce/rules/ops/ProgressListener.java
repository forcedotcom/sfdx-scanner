package com.salesforce.rules.ops;

import com.salesforce.graph.ApexPath;
import java.util.List;

public interface ProgressListener {
    void initializingPathCreation();

    void identifiedPaths(List<ApexPath> paths);

    void pickedNewPathForAnalysis(ApexPath path);
}

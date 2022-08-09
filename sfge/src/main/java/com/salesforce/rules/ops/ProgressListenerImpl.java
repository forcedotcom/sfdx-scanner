package com.salesforce.rules.ops;

import com.salesforce.graph.ApexPath;
import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import java.util.List;

public class ProgressListenerImpl implements ProgressListener {

    @Override
    public void initializingPathCreation() {
        CliMessager.postMessage("", EventKey.INFO_BEGIN_PATH_CREATION);
    }

    @Override
    public void identifiedPaths(List<ApexPath> paths) {
        CliMessager.postMessage("", EventKey.INFO_END_PATH_CREATION, String.valueOf(paths.size()));
    }

    @Override
    public void pickedNewPathForAnalysis(ApexPath path) {}
}

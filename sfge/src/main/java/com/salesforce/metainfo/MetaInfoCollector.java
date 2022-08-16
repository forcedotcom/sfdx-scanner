package com.salesforce.metainfo;

import com.salesforce.exception.SfgeRuntimeException;
import java.util.List;
import java.util.TreeSet;

/** Collects meta info from non-apex files in the same project */
public interface MetaInfoCollector {
    /**
     * Processes non-apex project files contained in the specified folders, including
     * subdirectories, gathering relevant information.
     */
    void loadProjectFiles(List<String> sourceFolders) throws MetaInfoLoadException;

    /**
     * @return meta info collected from processing the project files. For example,
     *     ApexControllerInfoCollector returns the name of any Apex class referenced by a
     *     VisualForce file (e.g., a VF controller/extension).
     */
    TreeSet<String> getMetaInfoCollected();

    String getMetaInfoTypeName();

    /** Thrown when project files cannot be properly loaded/processed. */
    final class MetaInfoLoadException extends SfgeRuntimeException {
        MetaInfoLoadException(String msg) {
            super(msg);
        }

        MetaInfoLoadException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}

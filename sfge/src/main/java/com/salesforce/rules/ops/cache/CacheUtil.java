package com.salesforce.rules.ops.cache;

public final class CacheUtil {
    private CacheUtil() {}

    /*public void getFilesInPath(ApexPath path) {
        final Result result = new Result();
        final MethodVertex entryPoint = path.getEntryPoint();

        final String entryFile = entryPoint.getFileName();
        final String entryMethod = entryPoint.getName();
        final ImmutableList<BaseSFVertex> transversedVertices = path.getTraversedVertices().asList();
        for (BaseSFVertex vertex: transversedVertices) {
            result.addFileToEntryPoint(vertex.getFileName(), entryFile, entryMethod);
        }

    }*/

    /**
     * Violation -> Set<Files> File -> Violation (source file and method) File -> Set<source
     * file/method>
     *
     * <p>Source file/method -> Set<violation>
     */

    /**
     * Additional output to include cache information Verbose parameter to execute Graph Engine
     * that'll trigger additional output
     */
}

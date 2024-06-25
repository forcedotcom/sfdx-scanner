package com.salesforce.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.salesforce.config.SfgeConfig;
import com.salesforce.config.SfgeConfigProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Creates cache data that can be used to execute Delta runs. */
public final class CacheCreator {
    private static final Logger LOGGER = LogManager.getLogger(CacheCreator.class);

    private Dependencies dependencies;

    public CacheCreator() {
        this.dependencies = new Dependencies();
    }

    CacheCreator(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Creates cache data that can be used for future delta runs
     *
     * @param result from the current execution
     */
    public void create(Result result) {
        if (SfgeConfigProvider.get().isCachingDisabled()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                    "Skipping to cache information since it has been disabled.");
            }
            return;
        }
        createFilesToEntriesCache(result.getFilesToEntriesMap());
    }

    private void createFilesToEntriesCache(FilesToEntriesMap filesToEntriesMap) {
        final String jsonData = filesToEntriesMap.toJsonString();
        final String filesToEntriesDataFilename = getFilesToEntriesDataFilename();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "About to create cache for filesToEntriesMap at " + filesToEntriesDataFilename);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Content to be written in " + filesToEntriesDataFilename + ": " + jsonData);
        }

        try {
            dependencies.writeFile(filesToEntriesDataFilename, jsonData);
        } catch (IOException e) {
            // Putting a warning instead of stopping the process since this is not in the critical
            // path
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Exception occurred while writing filesToEntriesMap cache data.", e);
            }
        }
    }

    private String getFilesToEntriesDataFilename() {
        return SfgeConfigProvider.get().getFilesToEntriesCacheLocation();
    }

    static class Dependencies {

        /**
         * Write to file. The current assumption is to overwrite the file. TODO: This could be
         * problematic when the delta run wants to preserve the original content. The ideal approach
         * would be to overwrite entries of only the files that were changed.
         *
         * @param filename of the file to create/overwrite.
         * @param data that should be written to the file.
         * @throws IOException when an error is encountered.
         */
        void writeFile(String filename, String data) throws IOException {
            mkdirsIfNeeded(filename);

            final boolean append = false;
            FileWriter fileWriter = new FileWriter(filename, append);
            try {
                fileWriter.write(data);
            } finally {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            }
        }

        /**
         * Create new directories in the path, if necessary
         *
         * @param filename
         */
        private void mkdirsIfNeeded(String filename) {
            final Path filepath = Path.of(filename);
            final Path directory = filepath.getParent();

            final boolean isNewDirectory = directory.toFile().mkdirs();
            if (LOGGER.isDebugEnabled()) {
                if (isNewDirectory) {
                    LOGGER.debug("Created new cache directory: " + directory.getFileName());
                } else {
                    LOGGER.debug("Cache directory already exists: " + directory.getFileName());
                }
            }
        }
    }
}

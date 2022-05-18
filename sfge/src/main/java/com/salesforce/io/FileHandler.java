package com.salesforce.io;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileHandler implements IoHandler {
    private FileHandler() {}

    @Override
    public String readTargetFile(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        final List<String> allLines = Files.readAllLines(Paths.get(fileName));
        Joiner.on("\n").appendTo(sb, allLines);

        return sb.toString();
    }

    public static FileHandler getInstance() {
        return FileHandler.LazyLoader.INSTANCE;
    }

    private static final class LazyLoader {
        // Postpones initialization until first use.
        private static final FileHandler INSTANCE = new FileHandler();
    }
}

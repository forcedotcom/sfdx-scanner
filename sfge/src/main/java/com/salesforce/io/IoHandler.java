package com.salesforce.io;

import java.io.IOException;

/**
 * This interface exists so we can have multiple interchangeable implementations. E.g., one for
 * loading files in production, and one for connecting anonymous source code to aliases in testing.
 */
public interface IoHandler {
    String readTargetFile(String fileName) throws IOException;
}

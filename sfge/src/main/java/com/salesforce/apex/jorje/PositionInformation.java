package com.salesforce.apex.jorje;

import com.salesforce.exception.UnexpectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Converts character based indexes to line numbers. */
final class PositionInformation {
    private static final Logger LOGGER = LogManager.getLogger(PositionInformation.class);

    static final String END_LINE = "\n";

    /** The total length of the source code, including new lines */
    private final int totalLength;

    /**
     * Jorje provides location information using an index. Store a list of the starting index for
     * each line. The line number is calculated by doing a binary search of this list to find the
     * point at which the index would be inserted into the list.
     */
    private final List<Integer> firstCharacterIndex;

    PositionInformation(String sourceCode) {
        this.totalLength = sourceCode.length();
        this.firstCharacterIndex = new ArrayList<>();
        // Add a 0 to account for the first line
        firstCharacterIndex.add(0);
        int index = 0;
        for (String line : sourceCode.split(END_LINE)) {
            // Add 1 to compensate for the new line
            index += line.length() + 1;
            firstCharacterIndex.add(index);
        }
    }

    /** Finds the line number that contains the given character index */
    int getLineNumber(int index) {
        if (index < 0) {
            throw new UnexpectedException("Index must be positive");
        }

        if (index > totalLength) {
            throw new UnexpectedException("totalLength=" + totalLength + ", index=" + index);
        }

        // The result is >= 0 if there is an exact match or the value is greater than all items in
        // the list
        // The result is "(-(insertion point) - 1)" if there is not an exact match
        // Add 1 because the line numbers start at 1
        final int insertLocation = Collections.binarySearch(firstCharacterIndex, index);
        final int line;
        if (insertLocation > 0) {
            // This was an exact match, add one to account for the offset
            line = insertLocation + 1;
        } else {
            // Add 1 because the result subtracted 1
            line = -1 * (insertLocation + 1);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "index=" + index + ", insertLocation=" + insertLocation + ", line=" + line);
        }
        return line;
    }
}

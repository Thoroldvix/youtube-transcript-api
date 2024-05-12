package io.github.thoroldvix.internal;

import java.io.IOException;
import java.util.List;

/**
 * Used for reading lines from a file.
 */
@FunctionalInterface
interface FileLinesReader {
    /**
     * Reads lines from a file.
     *
     * @param filePath The path to the file
     * @return A list of lines
     * @throws IOException If the file could not be read
     */
    List<String> readLines(String filePath) throws IOException;
}

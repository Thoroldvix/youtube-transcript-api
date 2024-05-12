package io.github.thoroldvix.api;

/**
 * Represents a formatter for transcript content.
 */
@FunctionalInterface
public interface TranscriptFormatter {

    /**
     * Formats the transcript content.
     *
     * @param transcriptContent The {@link TranscriptContent} to format.
     * @return The formatted transcript content as a {@link String}.
     */
    String format(TranscriptContent transcriptContent);
}

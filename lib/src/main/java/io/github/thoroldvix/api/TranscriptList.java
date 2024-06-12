package io.github.thoroldvix.api;

import java.util.function.Consumer;

/**
 * Represents a list of all available transcripts for a YouTube video.
 * <p>
 * This interface provides methods to iterate over all available transcripts for a given YouTube video, and to find either generated or manual transcripts for a specific language.
 * Individual transcripts are represented by {@link Transcript} objects.
 * Instances of {@link TranscriptList} can be obtained through the {@link YoutubeTranscriptApi} class.
 * </p>
 */
public interface TranscriptList extends Iterable<Transcript> {

    /**
     * Searches for a transcript using the provided language codes.
     * Manually created transcripts are prioritized, and if none are found, generated transcripts are used.
     * If you only want generated or manually created transcripts, use {@link #findGeneratedTranscript(String...)} or {@link #findManualTranscript(String...)} instead.
     *
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return The found {@link Transcript}.
     * @throws TranscriptRetrievalException If no transcript could be found for the given language codes.
     */
    Transcript findTranscript(String... languageCodes) throws TranscriptRetrievalException;

    /**
     * Searches for an automatically generated transcript using the provided language codes.
     *
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return The found {@link Transcript}.
     * @throws TranscriptRetrievalException If no transcript could be found for the given language codes.
     */
    Transcript findGeneratedTranscript(String... languageCodes) throws TranscriptRetrievalException;

    /**
     * Searches for a manually created transcript using the provided language codes.
     *
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return The found {@link Transcript}.
     * @throws TranscriptRetrievalException If no transcript could be found for the given language codes.
     */
    Transcript findManualTranscript(String... languageCodes) throws TranscriptRetrievalException;

    /**
     * Retrieves the ID of the video to which transcript was retrieved.
     *
     * @return The video ID.
     */
    String getVideoId();

    @Override
    default void forEach(Consumer<? super Transcript> action) {
        Iterable.super.forEach(action);
    }
}

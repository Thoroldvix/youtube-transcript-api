package io.github.thoroldvix.api;

import io.github.thoroldvix.internal.TranscriptApiFactory;

/**
 * This is the main interface for the YouTube Transcript API.
 * <p>
 * It provides functionality for retrieving all available transcripts or retrieving actual transcript content from YouTube.
 * </p>
 * <p>
 * To instantiate this API, you should use {@link TranscriptApiFactory}.
 * </p>
 */
public interface YoutubeTranscriptApi {

    /**
     * Retrieves a list of available transcripts for a given video using cookies from a specified file path.
     * <p>
     * Used when you want to list transcripts for a video that is age-restricted.
     * It tries to bypass the age-restriction by using the provided authentication cookies.
     * </p>
     * <p>
     * <b>Note:</b> For more information on how to obtain the authentication cookies,
     * see the <a href="https://github.com/thoroldvix/youtube-transcript-api/#Cookies">GitHub page</a>.
     * </p>
     *
     * @param videoId     The ID of the video
     * @param cookiesPath The file path to the text file containing the authentication cookies
     * @return {@link TranscriptList} A list of all available transcripts for the given video
     * @throws TranscriptRetrievalException If the retrieval of the transcript list fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    TranscriptList listTranscriptsWithCookies(String videoId, String cookiesPath) throws TranscriptRetrievalException;

    /**
     * Retrieves a list of available transcripts for a given video.
     *
     * @param videoId The ID of the video
     * @return {@link TranscriptList} A list of all available transcripts for the given video
     * @throws TranscriptRetrievalException If the retrieval of the transcript list fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    TranscriptList listTranscripts(String videoId) throws TranscriptRetrievalException;

    /**
     * Retrieves transcript content for a given video using cookies from a specified file path.
     * <p>
     * Used when you want to retrieve transcript content for a video that is age-restricted.
     * It tries to bypass the age-restriction by using the provided authentication cookies.
     * </p>
     * <p>
     * <b>Note:</b> For more information on how to obtain the authentication cookies,
     * see the <a href="https://github.com/thoroldvix/youtube-transcript-api/#Cookies">GitHub page</a>.
     * </p>
     * <p>
     * This is a shortcut for calling:
     * </p>
     * <p>
     * {@code listTranscriptsWithCookies(videoId).findTranscript(languageCodes).fetch();}
     * </p>
     *
     * @param videoId       The ID of the video
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @param cookiesPath   The file path to the text file containing the authentication cookies
     * @return {@link TranscriptContent} The transcript content
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    TranscriptContent getTranscriptWithCookies(String videoId, String cookiesPath, String... languageCodes) throws TranscriptRetrievalException;

    /**
     * Retrieves transcript content for a single video.
     * <p>
     * This is a shortcut for calling:
     * </p>
     * <p>
     * {@code listTranscripts(videoId).findTranscript(languageCodes).fetch();}
     * </p>
     *
     * @param videoId       The ID of the video
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return {@link TranscriptContent} The transcript content
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     * @throws IllegalArgumentException     If the video ID is invalid
     */
    TranscriptContent getTranscript(String videoId, String... languageCodes) throws TranscriptRetrievalException;
}

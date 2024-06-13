package io.github.thoroldvix.api;

import io.github.thoroldvix.internal.TranscriptApiFactory;

import java.util.Map;

/**
 * Retrieves transcripts for all videos in a playlist, or all videos for a specific channel.
 * <p>
 * Playlists and channel videos are retrieved from the YouTube API, so you will need to have a valid api key to use this.
 * <p>
 * All methods take a {@link TranscriptRequest} object as a parameter, which contains API key, cookies file path (optional), and stop on error flag (optional, defaults to true).
 * If cookies are not provided, the API will not be able to access age restricted videos, see <a href="https://github.com/Thoroldvix/youtube-transcript-api#cookies">Cookies</a>.
 * <p>
 * {@link TranscriptRequest} also contains a flag to stop on error, or continue on error.
 * </p>
 * <p>
 * To get implementation for this interface see {@link TranscriptApiFactory}
 * </p>
 */
public interface PlaylistsTranscriptApi {

    /**
     * Retrieves transcript lists for all videos in the specified playlist.
     *
     * @param playlistId The ID of the playlist
     * @param request    {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, TranscriptRequest request) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript lists for all videos for the specified channel.
     *
     * @param channelName The name of the channel
     * @param request     {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForChannel(String channelName, TranscriptRequest request) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript content for all videos in the specified playlist.
     *
     * @param playlistId    The ID of the playlist
     * @param request       {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId,
                                                             TranscriptRequest request,
                                                             String... languageCodes) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript content for all videos for the specified channel.
     *
     * @param channelName   The name of the channel
     * @param request       {@link TranscriptRequest} request object containing API key, cookies file path, and stop on error flag
     * @param languageCodes A varargs list of language codes in descending priority.
     *                      <p>
     *                      For example:
     *                      </p>
     *                      If this is set to {@code ("de", "en")}, it will first attempt to fetch the German transcript ("de"), and then fetch the English
     *                      transcript ("en") if the former fails. If no language code is provided, it uses English as the default language.
     * @return A map of video IDs to {@link TranscriptContent} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript fails
     */
    Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException;
}

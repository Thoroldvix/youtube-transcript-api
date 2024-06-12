package io.github.thoroldvix.api;

import io.github.thoroldvix.internal.TranscriptApiFactory;

import java.util.Map;

/**
 * Retrieves transcripts for all videos in a playlist, or all videos for a specific channel.
 * <p>
 * Playlists and channel videos are retrieved from the YouTube API, so you will need to have a valid api key to use this.
 * </p>
 * <p>
 * To get implementation for this interface see {@link TranscriptApiFactory}
 * </p>
 */
public interface PlaylistsTranscriptApi {

    /**
     * Retrieves transcript lists for all videos in the specified playlist using provided API key and cookies file from a specified path.
     *
     * @param playlistId      The ID of the playlist
     * @param apiKey          API key for the YouTube V3 API (see <a href="https://developers.google.com/youtube/v3/getting-started">Getting started</a>)
     * @param continueOnError Whether to continue if transcript retrieval fails for a video. If true, all transcripts that could not be retrieved will be skipped,
     *                        otherwise an exception will be thrown.
     * @param cookiesPath     The file path to the text file containing the authentication cookies. Used in the case if some videos are age restricted see {<a href="https://github.com/Thoroldvix/youtube-transcript-api#cookies">Cookies</a>}
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, String apiKey, String cookiesPath, boolean continueOnError) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript lists for all videos in the specified playlist using provided API key.
     *
     * @param playlistId      The ID of the playlist
     * @param apiKey          API key for the YouTube V3 API (see <a href="https://developers.google.com/youtube/v3/getting-started">Getting started</a>)
     * @param continueOnError Whether to continue if transcript retrieval fails for a video. If true, all transcripts that could not be retrieved will be skipped,
     *                        otherwise an exception will be thrown.
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, String apiKey, boolean continueOnError) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript lists for all videos for the specified channel using provided API key and cookies file from a specified path.
     *
     * @param channelName     The name of the channel
     * @param apiKey          API key for the YouTube V3 API (see <a href="https://developers.google.com/youtube/v3/getting-started">Getting started</a>)
     * @param cookiesPath     The file path to the text file containing the authentication cookies. Used in the case if some videos are age restricted see {<a href="https://github.com/Thoroldvix/youtube-transcript-api#cookies">Cookies</a>}
     * @param continueOnError Whether to continue if transcript retrieval fails for a video. If true, all transcripts that could not be retrieved will be skipped,
     *                        otherwise an exception will be thrown.
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForChannel(String channelName, String apiKey, String cookiesPath, boolean continueOnError) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript lists for all videos for the specified channel using provided API key.
     *
     * @param channelName     The name of the channel
     * @param apiKey          API key for the YouTube V3 API (see <a href="https://developers.google.com/youtube/v3/getting-started">Getting started</a>)
     * @param continueOnError Whether to continue if transcript retrieval fails for a video. If true, all transcripts that could not be retrieved will be skipped,
     *                        otherwise an exception will be thrown.
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForChannel(String channelName, String apiKey, boolean continueOnError) throws TranscriptRetrievalException;
}

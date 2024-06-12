package io.github.thoroldvix.api;

import io.github.thoroldvix.internal.TranscriptApiFactory;

import java.util.Map;

/**
 * Retrieves transcripts for all videos in a playlist
 * <p>
 * Playlists are retrieved from the YouTube API, so you will need to have a valid api key to use this.
 * </p>
 * <p>
 * To get implementation for this interface see {@link TranscriptApiFactory}
 * </p>
 */
public interface YoutubePlaylistTranscripts {

    /**
     * Retrieves transcript lists for all videos in the playlist using the given cookies file from a specified path.
     *
     * @param continueOnError Whether to continue if transcript retrieval fails for a video. If true, all transcripts that could not be retrieved will be skipped,
     *                        otherwise an exception will be thrown.
     * @param cookiesPath     The file path to the text file containing the authentication cookies. Used in the case if some videos are age restricted see {<a href="https://github.com/Thoroldvix/youtube-transcript-api#cookies">Cookies</a>}
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForPlaylist(boolean continueOnError, String cookiesPath) throws TranscriptRetrievalException;


    /**
     * Retrieves transcript lists for all videos in the playlist.
     *
     * @param continueOnError Whether to continue if transcript retrieval fails for a video. If true, all transcripts that could not be retrieved will be skipped,
     *                        otherwise an exception will be thrown.
     * @return A map of video IDs to {@link TranscriptList} objects
     * @throws TranscriptRetrievalException If the retrieval of the transcript lists fails
     */
    Map<String, TranscriptList> listTranscriptsForPlaylist(boolean continueOnError) throws TranscriptRetrievalException;
}

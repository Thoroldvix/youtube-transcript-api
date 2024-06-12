package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.YoutubeClient;
import io.github.thoroldvix.api.YoutubePlaylistTranscripts;
import io.github.thoroldvix.api.YoutubeTranscriptApi;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsible for creating instances of {@link YoutubeTranscriptApi} and {@link YoutubePlaylistTranscripts}.
 */
public final class TranscriptApiFactory {

    private TranscriptApiFactory() {
    }

    /**
     * Creates a new instance of {@link YoutubeTranscriptApi} using the default YouTube client.
     *
     * @return A new instance of {@link YoutubeTranscriptApi}
     */
    public static YoutubeTranscriptApi createDefault() {
        return createWithClient(new DefaultYoutubeClient());
    }

    /**
     * Creates a new instance of {@link YoutubePlaylistTranscripts} using the specified {@link YoutubeClient}, playlist ID and API key for the YouTube API.
     *
     * @param client     The {@link YoutubeClient} to be used for YouTube interactions
     * @param playlistId The YouTube playlist ID
     * @param apiKey     The YouTube API key
     * @return A new instance of {@link YoutubePlaylistTranscripts}
     */
    public static YoutubePlaylistTranscripts createDefaultPlaylistTranscripts(YoutubeClient client, String playlistId, String apiKey) {
        return new DefaultYoutubePlaylistTranscripts(client, createDefault(), playlistId, apiKey);
    }

    /**
     * Creates a new instance of {@link YoutubePlaylistTranscripts} using the default YouTube client, playlist ID and API key for the YouTube API.
     *
     * @param playlistId The YouTube playlist ID
     * @param apiKey     The YouTube API key
     * @return A new instance of {@link YoutubePlaylistTranscripts}
     */
    public static YoutubePlaylistTranscripts createDefaultPlaylistTranscripts(String playlistId, String apiKey) {
        return createDefaultPlaylistTranscripts(new DefaultYoutubeClient(), playlistId, apiKey);
    }

    /**
     * Creates a new instance of {@link YoutubeTranscriptApi} using the specified {@link YoutubeClient}.
     *
     * @param client The {@link YoutubeClient} to be used for YouTube interactions
     * @return A new instance of {@link YoutubeTranscriptApi}
     */
    public static YoutubeTranscriptApi createWithClient(YoutubeClient client) {
        return new DefaultYoutubeTranscriptApi(client, filePath -> Files.readAllLines(Path.of(filePath)));
    }
}

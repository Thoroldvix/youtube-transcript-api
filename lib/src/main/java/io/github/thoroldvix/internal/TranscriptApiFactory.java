package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.PlaylistsTranscriptApi;
import io.github.thoroldvix.api.YoutubeClient;
import io.github.thoroldvix.api.YoutubeTranscriptApi;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsible for creating instances of {@link YoutubeTranscriptApi} and {@link PlaylistsTranscriptApi}.
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
     * Creates a new instance of {@link PlaylistsTranscriptApi} using the specified {@link YoutubeClient}.
     *
     * @param client The {@link YoutubeClient} to be used for YouTube interactions
     * @return A new instance of {@link PlaylistsTranscriptApi}
     */
    public static PlaylistsTranscriptApi createDefaultPlaylistsApi(YoutubeClient client) {
        return new DefaultPlaylistsTranscriptApi(client, createDefault());
    }

    /**
     * Creates a new instance of {@link PlaylistsTranscriptApi} using the default YouTube client.
     *
     * @return A new instance of {@link PlaylistsTranscriptApi}
     */
    public static PlaylistsTranscriptApi createDefaultPlaylistsApi() {
        return createDefaultPlaylistsApi(new DefaultYoutubeClient());
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

package io.github.thoroldvix.api;

/**
 * The YouTube API V3 endpoints. Used by the {@link YoutubeClient}.
 */
public enum YtApiV3Endpoint {
    PLAYLIST_ITEMS("playlistItems"),
    SEARCH("search"),
    CHANNELS("channels");
    private final static String YOUTUBE_API_V3_BASE_URL = "https://www.googleapis.com/youtube/v3/";

    private final String resource;
    private final String url;

    YtApiV3Endpoint(String resource) {
        this.url = YOUTUBE_API_V3_BASE_URL + resource;
        this.resource = resource;
    }

    public String url() {
        return url;
    }

    @Override
    public String toString() {
        return resource;
    }
}

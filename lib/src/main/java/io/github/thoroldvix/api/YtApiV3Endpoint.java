package io.github.thoroldvix.api;

/**
 * The YouTube API v3 endpoints. Used by the {@link YoutubeClient}.
 */
public enum YtApiV3Endpoint {

    PLAYLIST_ITEMS("playlistItems"),
    ;
    private final static String YOUTUBE_API_V3_BASE_URL = "https://www.googleapis.com/youtube/v3/";

    private final String url;

    YtApiV3Endpoint(String urlPath) {
        this.url = YOUTUBE_API_V3_BASE_URL + urlPath;
    }

    public String url() {
        return url;
    }
}

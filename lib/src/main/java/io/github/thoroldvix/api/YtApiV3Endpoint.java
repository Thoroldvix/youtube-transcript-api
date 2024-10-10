package io.github.thoroldvix.api;

import java.util.Map;


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

    private static String createParamsString(Map<String, String> params) {
        StringBuilder paramString = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue().replaceAll(" ", "%20");
            paramString.append(entry.getKey()).append("=").append(value).append("&");
        }

        paramString.deleteCharAt(paramString.length() - 1);
        return paramString.toString();
    }

    /**
     * @return The URL of the endpoint.
     */
    public String url() {
        return url;
    }

    /**
     * @param params A map of parameters to include in the request.
     *
     * @return The URL of the endpoint with the specified parameters.
     */
    public String url(Map<String, String> params) {
        return url + "?" + createParamsString(params);
    }

    @Override
    public String toString() {
        return resource;
    }
}

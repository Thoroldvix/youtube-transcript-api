package io.github.thoroldvix.api;


import java.util.Map;

/**
 * Responsible for sending GET requests to YouTube.
 */
@FunctionalInterface
public interface YoutubeClient {

    /**
     * Sends a GET request to the specified URL and returns the response body.
     *
     * @param url     The URL to which the GET request is made.
     * @param headers A map of additional headers to include in the request.
     * @return The body of the response as a {@link String}.
     * @throws TranscriptRetrievalException If the request to YouTube fails.
     */
    String get(String url, Map<String, String> headers) throws TranscriptRetrievalException;
}


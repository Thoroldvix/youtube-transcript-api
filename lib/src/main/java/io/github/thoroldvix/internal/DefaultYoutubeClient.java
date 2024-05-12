package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Default implementation of {@link YoutubeClient}.
 */
final class DefaultYoutubeClient implements YoutubeClient {

    private static final String YOUTUBE_REQUEST_FAILED = "Request to YouTube failed.";

    private final HttpClient httpClient;

    DefaultYoutubeClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    DefaultYoutubeClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(String url, Map<String, String> headers) throws TranscriptRetrievalException {
        String videoId = url.split("=")[1];
        String[] headersArray = createHeaders(headers);
        HttpRequest request = createRequest(url, headersArray);
        HttpResponse<String> response = send(videoId, request);
        if (response.statusCode() != 200) {
            throw new TranscriptRetrievalException(videoId, YOUTUBE_REQUEST_FAILED + " Status code: " + response.statusCode());
        }
        return response.body();
    }

    private HttpResponse<String> send(String videoId, HttpRequest request) throws TranscriptRetrievalException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TranscriptRetrievalException(videoId, YOUTUBE_REQUEST_FAILED, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(videoId, YOUTUBE_REQUEST_FAILED, e);
        }
    }

    private static HttpRequest createRequest(String url, String[] headersArray) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(headersArray)
                .build();
    }

    private String[] createHeaders(Map<String, String> headers) {
        String[] headersArray = new String[headers.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersArray[i++] = entry.getKey();
            headersArray[i++] = entry.getValue();
        }
        return headersArray;
    }
}

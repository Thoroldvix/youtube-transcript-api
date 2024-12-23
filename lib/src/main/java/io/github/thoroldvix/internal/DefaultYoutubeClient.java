package io.github.thoroldvix.internal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;
import io.github.thoroldvix.api.YtApiV3Endpoint;


/**
 * Default implementation of {@link YoutubeClient}.
 */
final class DefaultYoutubeClient implements YoutubeClient {

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
        String errorMessage = "Request to YouTube failed.";
        String[] headersArray = createHeaders(headers);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(headersArray)
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TranscriptRetrievalException(videoId, errorMessage, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(videoId, errorMessage, e);
        }

        if (response.statusCode() != 200) {
            throw new TranscriptRetrievalException(videoId, errorMessage + " Status code: " + response.statusCode());
        }
        return response.body();
    }

    @Override
    public String get(YtApiV3Endpoint endpoint, Map<String, String> params) throws TranscriptRetrievalException {
        String errorMessage = String.format("Request to YouTube '%s' endpoint failed.", endpoint);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.url(params)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TranscriptRetrievalException(errorMessage, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptRetrievalException(errorMessage, e);
        }

        if (response.statusCode() != 200) {
            throw new TranscriptRetrievalException(errorMessage + " Status code: " + response.statusCode());
        }

        return response.body();
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

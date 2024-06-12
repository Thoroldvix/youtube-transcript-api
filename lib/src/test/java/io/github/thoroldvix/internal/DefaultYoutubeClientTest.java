package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.PLAYLIST_ITEMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DefaultYoutubeClientTest {

    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final Map<String, String> HEADERS = Map.of("Accept-Language", "en-US");
    private static final Map<String, String> PARAMS = Map.of("key", "test", "part", "snippet");
    @Mock
    private HttpResponse<String> response;
    @Mock
    private HttpClient httpClient;
    @Captor
    private ArgumentCaptor<HttpRequest> requestCaptor;

    private YoutubeClient youtubeClient;

    @BeforeEach
    void setUp() {
        youtubeClient = new DefaultYoutubeClient(httpClient);
    }

    private void givenResponse(String expected) throws IOException, InterruptedException {
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(expected);
    }

    @Test
    void get() throws Exception {
        String expected = "<html></html>";
        givenResponse(expected);

        String actual = youtubeClient.get(VIDEO_URL, Map.of("Accept-Language", "en-US", "Cookie", "test"));

        HttpRequest request = requestCaptor.getValue();

        assertThat(actual).isEqualTo(expected);
        assertThat(request.uri()).isEqualTo(URI.create(VIDEO_URL));
        assertThat(request.headers().map().get("Cookie")).contains("test");
        assertThat(request.headers().map().get("Accept-Language")).contains("en-US");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 404})
    void getThrowsExceptionIfResponseIsNotOk(int statusCode) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(response);
        when(response.statusCode()).thenReturn(statusCode);

        assertThatThrownBy(() -> youtubeClient.get(VIDEO_URL, HEADERS))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getThrowsExceptionWhenIOExceptionOccurs() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new IOException());

        assertThatThrownBy(() -> youtubeClient.get(VIDEO_URL, HEADERS))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getThrowsExceptionWhenInterruptedExceptionOccurs() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> youtubeClient.get(VIDEO_URL, HEADERS))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getToApiEndpoint() throws Exception {
        String expected = "expected response";

        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(expected);

        String actual = youtubeClient.get(PLAYLIST_ITEMS, PARAMS);

        HttpRequest request = requestCaptor.getValue();

        assertThat(actual).isEqualTo(expected);
        assertThat(request.uri().toString()).contains(PLAYLIST_ITEMS.url() + "?");
        assertThat(request.uri().toString()).contains("key=test");
        assertThat(request.uri().toString()).contains("part=snippet");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 404})
    void getToApiEndpointThrowsExceptionIfResponseIsNotOk(int statusCode) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(response);
        when(response.statusCode()).thenReturn(statusCode);

        assertThatThrownBy(() -> youtubeClient.get(PLAYLIST_ITEMS, PARAMS))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getToApiEndpointThrowsExceptionWhenIOExceptionOccurs() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new IOException());

        assertThatThrownBy(() -> youtubeClient.get(PLAYLIST_ITEMS, PARAMS))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getToApiEndpointThrowsExceptionWhenInterruptedExceptionOccurs() throws Exception {
        when(httpClient.send(any(), any())).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> youtubeClient.get(PLAYLIST_ITEMS, PARAMS))
                .isInstanceOf(TranscriptRetrievalException.class);
    }
}
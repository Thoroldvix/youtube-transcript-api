package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.PLAYLIST_ITEMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YoutubePlaylistTranscriptsTest {
    private final String VIDEO_ID_1 = "8idr1WZ1A7Q";
    private final String VIDEO_ID_2 = "ZA4JkHKZM50";
    private YoutubeClient client;

    private YoutubeTranscriptApi api;

    private YoutubePlaylistTranscripts playlistApi;

    private static String PLAYLIST_SINGLE_PAGE;

    private static final String API_RESPONSES_DIR = "src/test/resources/api_v3_responses";

    @BeforeAll
    static void beforeAll() throws IOException {
        PLAYLIST_SINGLE_PAGE = Files.readString(Paths.get(API_RESPONSES_DIR, "playlist_single_page.json"));
    }

    @BeforeEach
    void setUp() {
        client = mock(YoutubeClient.class);
        api = mock(YoutubeTranscriptApi.class);
        playlistApi = new DefaultYoutubePlaylistTranscripts(
                client,
                api,
                "playlistId",
                "apiKey"
        );
    }

    @Test
    void listTranscriptsForPlaylistWithCookies() throws TranscriptRetrievalException {
        TranscriptList transcriptList1 = createTranscriptList(VIDEO_ID_1);
        TranscriptList transcriptList2 = createTranscriptList(VIDEO_ID_2);


        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(api.listTranscriptsWithCookies(VIDEO_ID_1, "cookiePath")).thenReturn(transcriptList1);
        when(api.listTranscriptsWithCookies(VIDEO_ID_2, "cookiePath")).thenReturn(transcriptList2);

        Map<String, TranscriptList> result = playlistApi.listTranscriptsForPlaylist(false, "cookiePath");

        assertThat(result.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(result.get(VIDEO_ID_1)).isEqualTo(transcriptList1);
        assertThat(result.get(VIDEO_ID_2)).isEqualTo(transcriptList2);
    }

    private static DefaultTranscriptList createTranscriptList(String videoId) {
        return new DefaultTranscriptList(videoId, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Test
    void listTranscriptsForPlaylistContinueOnError() throws TranscriptRetrievalException {
        TranscriptList transcriptList = createTranscriptList(VIDEO_ID_2);

        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(api.listTranscripts(VIDEO_ID_2)).thenReturn(transcriptList);
        when(api.listTranscripts(VIDEO_ID_1)).thenThrow(new TranscriptRetrievalException(VIDEO_ID_1, "Error"));

        assertThatThrownBy(() -> playlistApi.listTranscriptsForPlaylist(false))
                .isInstanceOf(TranscriptRetrievalException.class);

        assertThat(playlistApi.listTranscriptsForPlaylist(true)).containsKey(VIDEO_ID_2);
    }

    @Test
    void listTranscriptsForPlaylistGetsNextPage(@Captor ArgumentCaptor<Map<String, String>> paramsCaptor) throws TranscriptRetrievalException,
            IOException {
        String firstPageResponse = Files.readString(Paths.get(API_RESPONSES_DIR, "playlist_page_one.json"));
        String secondPageResponse = Files.readString(Paths.get(API_RESPONSES_DIR, "playlist_page_two.json"));

        when(client.get(eq(PLAYLIST_ITEMS), paramsCaptor.capture()))
                .thenReturn(firstPageResponse)
                .thenReturn(secondPageResponse);

        playlistApi.listTranscriptsForPlaylist(false);

        assertThat(paramsCaptor.getValue()).containsEntry("pageToken",
                "EAAajgFQVDpDQUVpRURJNE9VWTBRVFEyUkVZd1FUTXdSRElvQVVpN3BfbVAwY3FHQTFBQldrVWlRMmx" +
                "LVVZSR2NFbFZWVGxwVkRGa1ZWVlZVbEJoYlRGMlRURnJNbEZWVW5STlJrNXFWak" +
                "JHYzFKV2FHMU1WMXAzUldkM1NYRkxlVTl6ZDFsUkxVbFRkWGgzU1NJ");
    }

    @Test
    void listTranscriptsForPlaylistThrowsExceptionIfCannotParsePlaylistJson() throws TranscriptRetrievalException {
        when(client.get(eq(PLAYLIST_ITEMS), any())).thenReturn("error");

        assertThatThrownBy(() -> playlistApi.listTranscriptsForPlaylist(false))
                .isInstanceOf(TranscriptRetrievalException.class);
    }
}
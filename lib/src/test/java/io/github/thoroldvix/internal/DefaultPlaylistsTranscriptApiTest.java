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
import java.util.List;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPlaylistsTranscriptApiTest {
    private final String VIDEO_ID_1 = "8idr1WZ1A7Q";
    private final String VIDEO_ID_2 = "ZA4JkHKZM50";
    private static final String API_KEY = "apiKey";
    private static final String PLAYLIST_ID = "playlistId";
    private YoutubeClient client;

    private YoutubeTranscriptApi api;

    private PlaylistsTranscriptApi playlistApi;

    private static String PLAYLIST_SINGLE_PAGE;

    private static String CHANNEL_SEARCH_RESPONSE;
    private static final TranscriptRequest REQUEST = new TranscriptRequest(API_KEY, true);

    private static final String API_RESPONSES_DIR = "src/test/resources/api_v3_responses";
    private static final TranscriptRequest REQUEST_WITH_COOKIES = new TranscriptRequest(API_KEY, "cookiePath", true);
    private static String CHANNEL_RESPONSE;

    @BeforeAll
    static void beforeAll() throws IOException {
        PLAYLIST_SINGLE_PAGE = Files.readString(Paths.get(API_RESPONSES_DIR, "playlist_single_page.json"));
        CHANNEL_SEARCH_RESPONSE = Files.readString(Paths.get(API_RESPONSES_DIR, "channel_search_response.json"));
        CHANNEL_RESPONSE = Files.readString(Paths.get(API_RESPONSES_DIR, "channel_response.json"));
    }

    @BeforeEach
    void setUp() {
        client = mock(YoutubeClient.class);
        api = mock(YoutubeTranscriptApi.class);
        playlistApi = new DefaultPlaylistsTranscriptApi(
                client,
                api
        );
    }

    @Test
    void listTranscriptsForPlaylist() throws TranscriptRetrievalException {
        TranscriptList transcriptList1 = createTranscriptList(VIDEO_ID_1);
        TranscriptList transcriptList2 = createTranscriptList(VIDEO_ID_2);


        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(api.listTranscriptsWithCookies(VIDEO_ID_1, "cookiePath")).thenReturn(transcriptList1);
        when(api.listTranscriptsWithCookies(VIDEO_ID_2, "cookiePath")).thenReturn(transcriptList2);

        Map<String, TranscriptList> actual = playlistApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST_WITH_COOKIES);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(transcriptList1);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(transcriptList2);
    }

    private DefaultTranscriptList createTranscriptList(String videoId) {
        DefaultTranscript transcript = new DefaultTranscript(
                client,
                videoId,
                "apiUrl",
                "English",
                "en",
                false,
                Collections.emptyMap()
        );
        return new DefaultTranscriptList(videoId, Map.of("en", transcript), Collections.emptyMap(), Collections.emptyMap());
    }

    @Test
    void listTranscriptsForPlaylistContinueOnError() throws TranscriptRetrievalException {
        TranscriptList transcriptList = createTranscriptList(VIDEO_ID_2);


        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(api.listTranscripts(VIDEO_ID_2)).thenReturn(transcriptList);
        when(api.listTranscripts(VIDEO_ID_1)).thenThrow(new TranscriptRetrievalException(VIDEO_ID_1, "Error"));

        assertThatThrownBy(() -> playlistApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);

        assertThatNoException()
                .isThrownBy(() -> playlistApi.listTranscriptsForPlaylist(PLAYLIST_ID,
                        new TranscriptRequest("apiKey", false)));
    }

    @Test
    void listTranscriptsForPlaylistGetsNextPage(@Captor ArgumentCaptor<Map<String, String>> paramsCaptor) throws TranscriptRetrievalException,
            IOException {
        String firstPageResponse = Files.readString(Paths.get(API_RESPONSES_DIR, "playlist_page_one.json"));
        String secondPageResponse = Files.readString(Paths.get(API_RESPONSES_DIR, "playlist_page_two.json"));

        when(client.get(eq(PLAYLIST_ITEMS), paramsCaptor.capture()))
                .thenReturn(firstPageResponse)
                .thenReturn(secondPageResponse);

        playlistApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(paramsCaptor.getValue()).containsEntry("pageToken",
                "EAAajgFQVDpDQUVpRURJNE9VWTBRVFEyUkVZd1FUTXdSRElvQVVpN3BfbVAwY3FHQTFBQldrVWlRMmx" +
                "LVVZSR2NFbFZWVGxwVkRGa1ZWVlZVbEJoYlRGMlRURnJNbEZWVW5STlJrNXFWak" +
                "JHYzFKV2FHMU1WMXAzUldkM1NYRkxlVTl6ZDFsUkxVbFRkWGgzU1NJ");
    }

    @Test
    void listTranscriptsForPlaylistThrowsExceptionIfCannotParsePlaylistJson() throws TranscriptRetrievalException {
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn("error");

        assertThatThrownBy(() -> playlistApi.listTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForChannelWithCookies(@Captor ArgumentCaptor<Map<String, String>> paramsCaptor) throws Exception {
        TranscriptList transcriptList1 = createTranscriptList(VIDEO_ID_1);
        TranscriptList transcriptList2 = createTranscriptList(VIDEO_ID_2);

        when(client.get(eq(SEARCH), anyMap())).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(eq(CHANNELS), paramsCaptor.capture())).thenReturn(CHANNEL_RESPONSE);
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);

        when(api.listTranscriptsWithCookies(VIDEO_ID_1, "cookiePath")).thenReturn(transcriptList1);
        when(api.listTranscriptsWithCookies(VIDEO_ID_2, "cookiePath")).thenReturn(transcriptList2);

        Map<String, TranscriptList> actual = playlistApi.listTranscriptsForChannel("3Blue1Brown",
                REQUEST_WITH_COOKIES);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(transcriptList1);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(transcriptList2);
        assertThat(paramsCaptor.getValue()).containsEntry("id", "UCYO_jab_esuFRV4b17AJtAw");
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionWhenChannelNotFound() throws TranscriptRetrievalException, IOException {
        String searchNoMatchResponse = Files.readString(Paths.get(API_RESPONSES_DIR, "channel_search_no_match.json"));
        when(client.get(eq(SEARCH), anyMap())).thenReturn(searchNoMatchResponse);

        assertThatThrownBy(() -> playlistApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionIfCannotParseChannelSearchJson() throws Exception {
        when(client.get(eq(SEARCH), anyMap())).thenReturn("error");

        assertThatThrownBy(() -> playlistApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsForChannelThrowsExceptionIfCannotParseChannelJson() throws Exception {
        when(client.get(eq(SEARCH), anyMap())).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(eq(CHANNELS), anyMap())).thenReturn("error");

        assertThatThrownBy(() -> playlistApi.listTranscriptsForChannel("3Blue1Brown", REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptsForPlaylist() throws Exception {
        TranscriptList transcriptList1 = createTranscriptList(VIDEO_ID_1);
        TranscriptList transcriptList2 = createTranscriptList(VIDEO_ID_2);
        String transcriptContentXml = Files.readString(Paths.get("src/test/resources/", "transcript.xml"));

        DefaultTranscriptContent expected = new DefaultTranscriptContent(
                List.of(
                        new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0, 1.54),
                        new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                        new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239)
                )

        );

        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap())).thenReturn(transcriptContentXml);
        when(api.listTranscripts(VIDEO_ID_1)).thenReturn(transcriptList1);
        when(api.listTranscripts(VIDEO_ID_2)).thenReturn(transcriptList2);


        Map<String, TranscriptContent> actual = playlistApi.getTranscriptsForPlaylist(PLAYLIST_ID, REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(expected);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(expected);
    }

    @Test
    void getTranscriptsForPlaylistStopOnError() throws TranscriptRetrievalException {
        TranscriptList transcriptList1 = createTranscriptList(VIDEO_ID_1);
        TranscriptList transcriptList2 = createTranscriptList(VIDEO_ID_2);

        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap())).thenThrow(new TranscriptRetrievalException(VIDEO_ID_1, "Error"));
        when(api.listTranscripts(VIDEO_ID_1)).thenReturn(transcriptList1);
        when(api.listTranscripts(VIDEO_ID_2)).thenReturn(transcriptList2);

        assertThatThrownBy(() -> playlistApi.getTranscriptsForPlaylist(PLAYLIST_ID, REQUEST))
                .isInstanceOf(TranscriptRetrievalException.class);

        assertThatNoException()
                .isThrownBy(() -> playlistApi.getTranscriptsForPlaylist(PLAYLIST_ID,
                        new TranscriptRequest("apiKey", false)));
    }

    @Test
    void getTranscriptsForChannel() throws TranscriptRetrievalException, IOException {
        TranscriptList transcriptList1 = createTranscriptList(VIDEO_ID_1);
        TranscriptList transcriptList2 = createTranscriptList(VIDEO_ID_2);
        String transcriptContentXml = Files.readString(Paths.get("src/test/resources/", "transcript.xml"));

        DefaultTranscriptContent expected = new DefaultTranscriptContent(
                List.of(
                        new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0, 1.54),
                        new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                        new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239)
                )

        );

        when(client.get(eq(SEARCH), anyMap())).thenReturn(CHANNEL_SEARCH_RESPONSE);
        when(client.get(eq(CHANNELS), anyMap())).thenReturn(CHANNEL_RESPONSE);
        when(client.get(eq(PLAYLIST_ITEMS), anyMap())).thenReturn(PLAYLIST_SINGLE_PAGE);
        when(client.get(anyString(), anyMap())).thenReturn(transcriptContentXml);

        when(api.listTranscripts(VIDEO_ID_1)).thenReturn(transcriptList1);
        when(api.listTranscripts(VIDEO_ID_2)).thenReturn(transcriptList2);

        Map<String, TranscriptContent> actual = playlistApi.getTranscriptsForChannel("3Blue1Brown", REQUEST);

        assertThat(actual.keySet()).containsExactlyInAnyOrder(VIDEO_ID_1, VIDEO_ID_2);
        assertThat(actual.get(VIDEO_ID_1)).isEqualTo(expected);
        assertThat(actual.get(VIDEO_ID_2)).isEqualTo(expected);
    }
}
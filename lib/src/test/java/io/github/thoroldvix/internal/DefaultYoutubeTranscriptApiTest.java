package io.github.thoroldvix.internal;


import io.github.thoroldvix.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultYoutubeTranscriptApiTest {

    private static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    private static final String RESOURCE_PATH = "src/test/resources/";
    private static final String VIDEO_ID = "dQw4w9WgXcQ";

    private static String YOUTUBE_HTML;
    private static String TRANSCRIPT_XML;
    private static String CONSENT_PAGE_HTML;

    private YoutubeClient client;
    private FileLinesReader fileLinesReader;
    private YoutubeTranscriptApi youtubeTranscriptApi;

    @BeforeAll
    static void beforeAll() throws IOException {
        YOUTUBE_HTML = Files.readString(Path.of(RESOURCE_PATH, "pages/youtube.html.static"));
        TRANSCRIPT_XML = Files.readString(Path.of(RESOURCE_PATH, "transcript.xml"));
        CONSENT_PAGE_HTML = Files.readString(Path.of(RESOURCE_PATH, "pages/youtube_consent_page.html.static"));
    }

    @BeforeEach
    void setUp() {
        client = mock(YoutubeClient.class);
        fileLinesReader = Mockito.mock(FileLinesReader.class);
        youtubeTranscriptApi = new DefaultYoutubeTranscriptApi(client, fileLinesReader);
    }

    private void givenVideoPageHtml(String html) throws Exception {
        when(client.get(anyString(), anyMap())).thenReturn(html);
    }

    private void givenVideoPageHtmlFromFile(String fileName) throws Exception {
        String html = Files.readString(Path.of(RESOURCE_PATH, fileName));
        givenVideoPageHtml(html);
    }

    @Test
    void listTranscripts() throws Exception {
        when(client.get(YOUTUBE_WATCH_URL + VIDEO_ID, Map.of("Accept-Language", "en-US")))
                .thenReturn(YOUTUBE_HTML);

        TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts(VIDEO_ID);

        assertThat(transcriptList)
                .map(Transcript::getLanguageCode)
                .containsExactlyInAnyOrder("zh", "de", "hi", "ja", "ko", "es", "cs", "en");
    }

    @Test
    void listTranscriptsGivenVideoPageWithInvalidCaptionsJsonThrowsException() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_malformed_captions_json.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void listTranscriptsNoTranslations() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_no_translation.html.static");

        TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts(VIDEO_ID);

        for (Transcript transcript : transcriptList) {
            assertThat(transcript.getTranslationLanguages()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "short", "with spaces", "over11characterslong"})
    void listTranscriptsGivenInvalidVideoIdThrowsException(String invalidId) {
        assertThatThrownBy(() -> youtubeTranscriptApi.listTranscripts(invalidId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTranscript() throws Exception {
        when(client.get(anyString(), anyMap()))
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(TRANSCRIPT_XML);

        TranscriptContent expected = getTranscriptContent();

        TranscriptContent actual = youtubeTranscriptApi.getTranscript(VIDEO_ID);

        assertThat(actual).isEqualTo(expected);
    }

    private static DefaultTranscriptContent getTranscriptContent() {
        return new DefaultTranscriptContent(List.of(new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0.0, 1.54),
                new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239)));
    }

    @Test
    void getTranscriptCreatesConsentCookieIfNeededAndRetries() throws Exception {
        when(client.get(anyString(), anyMap()))
                .thenReturn(CONSENT_PAGE_HTML)
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(TRANSCRIPT_XML);

        youtubeTranscriptApi.getTranscript(VIDEO_ID);

        verify(client).get(anyString(), eq(Map.of("Accept-Language", "en-US",
                "Cookie", "CONSENT=YES+cb.20210328-17-p0.de+FX+119")));
    }

    @Test
    void getTranscriptThrowsExceptionWhenConsentCookieCreationFailed() throws Exception {
        givenVideoPageHtml(CONSENT_PAGE_HTML);

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenConsentCookieAgeInvalid() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_consent_page_invalid.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenVideoUnavailable() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_video_unavailable.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenYoutubeRequestLimitReached() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_too_many_requests.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionGivenInvalidTranscriptXML() throws Exception {
        when(client.get(anyString(), anyMap()))
                .thenReturn(YOUTUBE_HTML)
                .thenReturn("invalid xml");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenYoutubeRequestFailed() throws Exception {
        when(client.get(anyString(), anyMap())).thenThrow(TranscriptRetrievalException.class);

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenTranscriptsDisabled() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_transcripts_disabled.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);

        givenVideoPageHtmlFromFile("pages/youtube_transcripts_disabled2.html.static");

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenLanguageUnavailable() throws Exception {
        givenVideoPageHtml(YOUTUBE_HTML);

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID, "cz"))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptThrowsExceptionWhenNoTranscriptAvailable() throws Exception {
        givenVideoPageHtmlFromFile("pages/youtube_no_transcript_available.html.static");


        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscript(VIDEO_ID))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void getTranscriptWithCookies() throws Exception {
        List<String> lines = Files.readAllLines(Path.of(RESOURCE_PATH, "example_cookies.txt"));

        when(client.get(anyString(), anyMap()))
                .thenReturn(YOUTUBE_HTML)
                .thenReturn(TRANSCRIPT_XML);
        when(fileLinesReader.readLines(anyString())).thenReturn(lines);

        TranscriptContent expected = getTranscriptContent();

        TranscriptContent actual = youtubeTranscriptApi.getTranscriptWithCookies(VIDEO_ID, "cookiePath");

        assertThat(actual).isEqualTo(expected);
        verify(client).get(anyString(), eq(Map.of("Accept-Language", "en-US", "Cookie", "TEST_FIELD=\"TEST_VALUE\";$Path=\"/\";$Domain=\".example.com\"")));

    }

    @Test
    void getTranscriptWithCookiesWhenCannotReadCookiesFileThrowsException() throws Exception {
        when(fileLinesReader.readLines(anyString())).thenThrow(IOException.class);

        assertThatThrownBy(() -> youtubeTranscriptApi.getTranscriptWithCookies(VIDEO_ID, "cookiePath"))
                .isInstanceOf(TranscriptRetrievalException.class);
    }
}
package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.Transcript;
import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTranscriptTest {

    private YoutubeClient youtubeClient;
    private Transcript transcript;

    @BeforeEach
    void setUp() {
        youtubeClient = mock(YoutubeClient.class);
        transcript = new DefaultTranscript(
                youtubeClient,
                "dQw4w9WgXcQ",
                "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ",
                "English",
                "en",
                false,
                Map.of("af", "Afrikaans")
        );
    }

    @Test
    void fetchesTranscriptContent() throws Exception {
        String transcriptXml = Files.readString(Path.of("src/test/resources/transcript.xml"));
        when(youtubeClient.get(transcript.getApiUrl(), Map.of("Accept-Language", "en-US"))).thenReturn(transcriptXml);

        List<DefaultTranscriptContent.Fragment> expected = List.of(new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0.0, 1.54),
                new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239));

        TranscriptContent actual = transcript.fetch();

        assertThat(actual.getContent()).isEqualTo(expected);
    }

    @Test
    void translatesTranscript() throws Exception {
        Transcript translatedTranscript = transcript.translate("af");

        assertThat(translatedTranscript.getLanguageCode()).isEqualTo("af");
        assertThat(translatedTranscript.getApiUrl()).contains("&tlang=af");
    }

    @Test
    void translateTranscriptTranslationLanguageNotAvailable() {
        assertThatThrownBy(() -> transcript.translate("zz"))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void isTranslatableGivesCorrectResult() {
        Transcript notTranslatableTranscript = new DefaultTranscript(
                youtubeClient,
                "dQw4w9WgXcQ",
                "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ",
                "English",
                "en",
                false,
                Collections.emptyMap()
        );
        assertThat(transcript.isTranslatable()).isTrue();
        assertThat(notTranslatableTranscript.isTranslatable()).isFalse();
    }

    @Test
    void translateTranscriptThrowsExceptionWhenNotTranslatable() {
        Transcript transcript = new DefaultTranscript(
                youtubeClient,
                "dQw4w9WgXcQ",
                "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ",
                "English",
                "en",
                false,
                Collections.emptyMap()
        );

        assertThatThrownBy(() -> transcript.translate("af"))
                .isInstanceOf(TranscriptRetrievalException.class);
    }

    @Test
    void toStringFormattedCorrectly() {
        Transcript transcript = new DefaultTranscript(
                youtubeClient,
                "dQw4w9WgXcQ",
                "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ",
                "English",
                "en",
                false,
                Map.of("en", "English", "af", "Afrikaans")
        );

        String expected = """
                Transcript for video with id: dQw4w9WgXcQ.
                Language: English
                Language code: en
                API URL for retrieving content: https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ
                Available translation languages: [af, en]""";

        assertThat(transcript.toString()).isEqualTo(expected);
    }
}
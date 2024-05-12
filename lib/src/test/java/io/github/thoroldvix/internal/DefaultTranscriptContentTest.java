package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.internal.DefaultTranscriptContent.Fragment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTranscriptContentTest {

    private TranscriptContent transcriptContent;

    @BeforeEach
    void setUp() {
        List<Fragment> fragments = List.of(new DefaultTranscriptContent.Fragment("Hey, this is just a test", 0.0, 1.54),
                new DefaultTranscriptContent.Fragment("this is not the original transcript", 1.54, 4.16),
                new DefaultTranscriptContent.Fragment("test & test, like this \"test\" he's testing", 5.7, 3.239));
        transcriptContent = new DefaultTranscriptContent(fragments);
    }

    @Test
    void toStringFormattedCorrectly() {
        String expected = """
                content=[{text='Hey, this is just a test', start=0.0, dur=1.54},\
                 {text='this is not the original transcript', start=1.54, dur=4.16},\
                 {text='test & test, like this "test" he's testing', start=5.7, dur=3.239}]""";

        assertThat(transcriptContent.toString()).isEqualToNormalizingNewlines(expected);
    }
}

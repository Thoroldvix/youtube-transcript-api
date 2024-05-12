package io.github.thoroldvix;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptRetrievalExceptionTest {

    @Test
    void exceptionMessageBuiltCorrectly() {
        TranscriptRetrievalException exception = new TranscriptRetrievalException("dQw4w9WgXcQ", "Cause");

        String expected = "Could not retrieve transcript for the video: https://www.youtube.com/watch?v=dQw4w9WgXcQ.\nReason: Cause";

        String actual = exception.getMessage();

        assertThat(actual).isEqualTo(expected);
    }
}
package io.github.thoroldvix.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.thoroldvix.api.TranscriptContent.Fragment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responsible for creating {@link TranscriptFormatter} instances.
 * <p>
 * Available formatters are:
 * </p>
 * <ul>
 *     <li>{@link #jsonFormatter()}</li>
 *     <li>{@link #prettyJsonFormatter()}</li>
 *     <li>{@link #textFormatter()}</li>
 *     <li>{@link #webVTTFormatter()}</li>
 *     <li>{@link #srtFormatter()}</li>
 * </ul>
 */
public final class TranscriptFormatters {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TranscriptFormatters() {
    }

    /**
     * Creates a {@link TranscriptFormatter} that formats transcript content as JSON.
     *
     * @return A {@link TranscriptFormatter} for JSON format.
     */
    public static TranscriptFormatter jsonFormatter() {
        return transcriptContent -> formatAsJSON(transcriptContent, OBJECT_MAPPER.writer());
    }

    /**
     * Creates a {@link TranscriptFormatter} that formats transcript content as pretty-printed JSON.
     *
     * @return A {@link TranscriptFormatter} for pretty-printed JSON format.
     */
    public static TranscriptFormatter prettyJsonFormatter() {
        return transcriptContent -> formatAsJSON(transcriptContent, OBJECT_MAPPER.writerWithDefaultPrettyPrinter());
    }

    private static String formatAsJSON(TranscriptContent transcriptContent, ObjectWriter objectWriter) {
        try {
            return objectWriter.writeValueAsString(transcriptContent);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * Creates a {@link TranscriptFormatter} that formats transcript content as plain text without timestamps.
     *
     * @return A {@link TranscriptFormatter} for plain text format.
     */
    public static TranscriptFormatter textFormatter() {
        return transcriptContent -> transcriptContent.getContent().stream()
                .map(Fragment::getText)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Creates a {@link TranscriptFormatter} that formats transcript content as WebVTT format.
     * <p>
     * See <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebVTT_API">WebVTT specification</a> for more information.
     * </p>
     *
     * @return A {@link TranscriptFormatter} for WebVTT format.
     */
    public static TranscriptFormatter webVTTFormatter() {
        return transcriptContent -> "WEBVTT\n\n" + formatAsSubtitles(
                transcriptContent,
                fragment -> String.format("%s%n%s", fragmentToTimeStamp(fragment), fragment.getText())
        );
    }

    private static String formatAsSubtitles(TranscriptContent transcriptContent, Function<Fragment, String> formatter) {
        return transcriptContent.getContent().stream()
                .map(formatter)
                .collect(Collectors.joining("\n\n"));
    }

    private static String fragmentToTimeStamp(Fragment fragment) {
        return String.format("%s --> %s",
                secondsToTimeStamp(fragment.getStart()),
                secondsToTimeStamp(fragment.getStart() + fragment.getDur()));
    }

    private static String secondsToTimeStamp(double seconds) {
        int hour = (int) (seconds / 3600);
        int minute = (int) ((seconds % 3600) / 60);
        int second = (int) (seconds % 60);
        int millisecond = (int) ((seconds % 1) * 1000);
        return String.format("%02d:%02d:%02d.%03d", hour, minute, second, millisecond);
    }

    /**
     * Creates a {@link TranscriptFormatter} that formats transcript content as SRT (SubRip) subtitles.
     * <p>
     * See <a href="https://www.3playmedia.com/blog/create-srt-file/">SRT file format</a> for more information.
     * </p>
     *
     * @return A {@link TranscriptFormatter} for SRT format.
     */
    public static TranscriptFormatter srtFormatter() {
        return transcriptContent -> {
            AtomicInteger i = new AtomicInteger(1);
            return formatAsSubtitles(
                    transcriptContent,
                    fragment -> String.format("%d%n%s%n%s", i.getAndIncrement(), fragmentToTimeStamp(fragment), fragment.getText())
            );
        };
    }
}

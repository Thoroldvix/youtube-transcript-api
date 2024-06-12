package io.github.thoroldvix.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import io.github.thoroldvix.api.TranscriptContent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TranscriptContent}
 */
final class DefaultTranscriptContent implements TranscriptContent {

    private final List<DefaultTranscriptContent.Fragment> content;

    public DefaultTranscriptContent(List<DefaultTranscriptContent.Fragment> content) {
        this.content = content;
    }

    @Override
    public List<TranscriptContent.Fragment> getContent() {
        return Collections.unmodifiableList(content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultTranscriptContent that = (DefaultTranscriptContent) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public String toString() {
        return "content=[" + content.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")) + "]";
    }

    /**
     * Default implementation of {@link TranscriptContent.Fragment}
     */
    @JacksonXmlRootElement(localName = "transcript")
    static final class Fragment implements TranscriptContent.Fragment {
        @JacksonXmlText
        @JsonProperty("text")
        private String text;
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty("start")
        private double start;
        @JacksonXmlProperty(isAttribute = true)
        @JsonProperty("dur")
        private double dur;

        public Fragment(String text, double start, double dur) {
            this.text = text;
            this.start = start;
            this.dur = dur;
        }

        Fragment() {
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public double getStart() {
            return start;
        }

        @Override
        public double getDur() {
            return dur;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fragment fragment = (Fragment) o;
            return Double.compare(start, fragment.start) == 0 && Double.compare(dur, fragment.dur) == 0 && Objects.equals(text, fragment.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, start, dur);
        }

        @Override
        public String toString() {
            return "{" +
                   "text='" + text + '\'' +
                   ", start=" + start +
                   ", dur=" + dur +
                   '}';
        }
    }
}


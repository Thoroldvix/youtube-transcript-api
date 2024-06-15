package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.internal.DefaultTranscriptContent.Fragment;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Responsible for extracting transcript content from xml.
 */
final class TranscriptContentExtractor {

    private final String videoId;
    private static final XmlMapper XML_MAPPER = new XmlMapper();

    TranscriptContentExtractor(String videoId) {
        this.videoId = videoId;
    }

    private static List<Fragment> formatFragments(List<Fragment> fragments) {
        return fragments.stream()
                .filter(TranscriptContentExtractor::isValidTranscriptFragment)
                .map(TranscriptContentExtractor::removeHtmlTags)
                .map(TranscriptContentExtractor::unescapeXmlTags)
                .collect(Collectors.toList());
    }

    TranscriptContent extract(String xml) throws TranscriptRetrievalException {
        List<Fragment> fragments = parseFragments(xml);
        List<Fragment> content = formatFragments(fragments);

        return new DefaultTranscriptContent(content);
    }

    private static Fragment unescapeXmlTags(Fragment fragment) {
        String formattedValue = StringEscapeUtils.unescapeXml(fragment.getText());
        return new Fragment(formattedValue, fragment.getStart(), fragment.getDur());
    }

    private static Fragment removeHtmlTags(Fragment fragment) {
        Pattern pattern = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
        String text = pattern.matcher(fragment.getText()).replaceAll("");
        return new Fragment(text, fragment.getStart(), fragment.getDur());
    }

    private static boolean isValidTranscriptFragment(Fragment fragment) {
        return fragment.getText() != null && !fragment.getText().isBlank();
    }

    private List<Fragment> parseFragments(String xml) throws TranscriptRetrievalException {
        try {
            return XML_MAPPER.readValue(xml, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(videoId, "Failed to parse transcript content XML.", e);
        }
    }
}

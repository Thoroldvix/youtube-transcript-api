package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.Transcript;
import io.github.thoroldvix.api.TranscriptList;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Extracts transcript list from video page HTML.
 */
final class TranscriptListExtractor {

    private static final String TOO_MANY_REQUESTS = "YouTube is receiving too many requests from this IP and now requires solving a captcha to continue. " +
                                                    "One of the following things can be done to work around this:\n" +
                                                    "- Manually solve the captcha in a browser and export the cookie. " +
                                                    "Read here how to use that cookie with " +
                                                    "youtube-transcript-api: https://github.com/thoroldvix/youtube-transcript-api#cookies\n" +
                                                    "- Use a different IP address\n" +
                                                    "- Wait until the ban on your IP has been lifted";
    private static final String TRANSCRIPTS_DISABLED = "Transcripts are disabled for this video.";

    private final YoutubeClient client;
    private final String videoId;

    TranscriptListExtractor(YoutubeClient client, String videoId) {
        this.client = client;
        this.videoId = videoId;
    }

    TranscriptList extract(String videoPageHtml) throws TranscriptRetrievalException {
        String json = getJsonFromHtml(videoPageHtml, videoId);
        JsonNode parsedJson = parseJson(json);
        checkIfTranscriptsDisabled(parsedJson);
        return createTranscriptList(parsedJson);
    }

    private static String getJsonFromHtml(String videoPageHtml, String videoId) throws TranscriptRetrievalException {
        String[] splitHtml = videoPageHtml.split("\"captions\":");
        checkIfHtmlContainsJson(videoPageHtml, videoId, splitHtml);
        return splitHtml[1].split(",\"videoDetails")[0].replace("\n", "");
    }

    private static void checkIfHtmlContainsJson(String videoPageHtml, String videoId, String[] splitHtml) throws TranscriptRetrievalException {
        //no captions json in html
        if (splitHtml.length <= 1) {
            //recaptcha
            if (videoPageHtml.contains("class=\"g-recaptcha\"")) {
                throw new TranscriptRetrievalException(videoId, TOO_MANY_REQUESTS);
            }
            //non playable
            if (!videoPageHtml.contains("\"playabilityStatus\":")) {
                throw new TranscriptRetrievalException(videoId, "This video is no longer available.");
            }
            throw new TranscriptRetrievalException(videoId, TRANSCRIPTS_DISABLED);
        }
    }

    private JsonNode parseJson(String json) throws TranscriptRetrievalException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parsedJson;
        try {
            parsedJson = objectMapper.readTree(json).get("playerCaptionsTracklistRenderer");
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(videoId, "Failed to parse transcript JSON.", e);
        }
        return parsedJson;
    }

    private void checkIfTranscriptsDisabled(JsonNode parsedJson) throws TranscriptRetrievalException {
        if (parsedJson == null) {
            throw new TranscriptRetrievalException(videoId, TRANSCRIPTS_DISABLED);
        }
        if (!parsedJson.has("captionTracks")) {
            throw new TranscriptRetrievalException(videoId, TRANSCRIPTS_DISABLED);
        }
    }

    private TranscriptList createTranscriptList(JsonNode jsonNode) {
        return new DefaultTranscriptList(videoId,
                getManualTranscripts(jsonNode),
                getGeneratedTranscripts(jsonNode),
                getTranslationLanguages(jsonNode)
        );
    }

    private Map<String, String> getTranslationLanguages(JsonNode json) {
        if (!json.has("translationLanguages")) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(json.get("translationLanguages").spliterator(), false)
                .collect(Collectors.toMap(
                        jsonNode -> jsonNode.get("languageCode").asText(),
                        jsonNode -> jsonNode.get("languageName").get("simpleText").asText()
                ));
    }

    private Map<String, Transcript> getManualTranscripts(JsonNode json) {
        return getTranscripts(json, jsonNode -> !jsonNode.has("kind"));
    }

    private Map<String, Transcript> getGeneratedTranscripts(JsonNode json) {
        return getTranscripts(json, jsonNode -> jsonNode.has("kind"));
    }

    private Map<String, Transcript> getTranscripts(JsonNode json, Predicate<JsonNode> filter) {
        Map<String, String> translationLanguages = getTranslationLanguages(json);
        return StreamSupport.stream(json.get("captionTracks").spliterator(), false)
                .filter(filter)
                .map(jsonNode -> getTranscript(client, jsonNode, translationLanguages))
                .collect(Collectors.toMap(
                        Transcript::getLanguageCode,
                        transcript -> transcript,
                        (existing, replacement) -> existing)
                );
    }

    private Transcript getTranscript(YoutubeClient client, JsonNode jsonNode, Map<String, String> translationLanguages) {
        return new DefaultTranscript(
                client,
                videoId,
                jsonNode.get("baseUrl").asText(),
                jsonNode.get("name").get("simpleText").asText(),
                jsonNode.get("languageCode").asText(),
                jsonNode.has("kind"),
                translationLanguages
        );
    }
}

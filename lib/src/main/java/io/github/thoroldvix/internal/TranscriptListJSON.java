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
 * Represents transcript JSON which is extracted from a video page HTML.
 * It contains methods for retrieving transcript data from JSON.
 */
final class TranscriptListJSON {

    private static final String TOO_MANY_REQUESTS = "YouTube is receiving too many requests from this IP and now requires solving a captcha to continue. " +
                                                    "One of the following things can be done to work around this:\n" +
                                                    "- Manually solve the captcha in a browser and export the cookie. " +
                                                    "Read here how to use that cookie with " +
                                                    "youtube-transcript-api: https://github.com/thoroldvix/youtube-transcript-api#cookies\n" +
                                                    "- Use a different IP address\n" +
                                                    "- Wait until the ban on your IP has been lifted";
    private static final String TRANSCRIPTS_DISABLED = "Transcripts are disabled for this video.";

    private final JsonNode json;
    private final YoutubeClient client;
    private final String videoId;

    private TranscriptListJSON(JsonNode json, YoutubeClient client, String videoId) {
        this.json = json;
        this.client = client;
        this.videoId = videoId;
    }

    static TranscriptListJSON from(String videoPageHtml, YoutubeClient client, String videoId) throws TranscriptRetrievalException {
        String json = getJsonFromHtml(videoPageHtml, videoId);
        JsonNode parsedJson = parseJson(json, videoId);
        checkIfTranscriptsDisabled(videoId, parsedJson);
        return new TranscriptListJSON(parsedJson, client, videoId);
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

    private static JsonNode parseJson(String json, String videoId) throws TranscriptRetrievalException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode parsedJson;
        try {
            parsedJson = objectMapper.readTree(json).get("playerCaptionsTracklistRenderer");
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(videoId, "Failed to parse transcript JSON.", e);
        }
        return parsedJson;
    }

    private static void checkIfTranscriptsDisabled(String videoId, JsonNode parsedJson) throws TranscriptRetrievalException {
        if (parsedJson == null) {
            throw new TranscriptRetrievalException(videoId, TRANSCRIPTS_DISABLED);
        }
        if (!parsedJson.has("captionTracks")) {
            throw new TranscriptRetrievalException(videoId, TRANSCRIPTS_DISABLED);
        }
    }

    TranscriptList transcriptList() {
        return new DefaultTranscriptList(videoId, getManualTranscripts(), getGeneratedTranscripts(), getTranslationLanguages());
    }

    private Map<String, String> getTranslationLanguages() {
        if (!json.has("translationLanguages")) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(json.get("translationLanguages").spliterator(), false)
                .collect(Collectors.toMap(
                        jsonNode -> jsonNode.get("languageCode").asText(),
                        jsonNode -> jsonNode.get("languageName").get("simpleText").asText()
                ));
    }

    private Map<String, Transcript> getManualTranscripts() {
        return getTranscripts(client, jsonNode -> !jsonNode.has("kind"));
    }

    private Map<String, Transcript> getGeneratedTranscripts() {
        return getTranscripts(client, jsonNode -> jsonNode.has("kind"));
    }

    private Map<String, Transcript> getTranscripts(YoutubeClient client, Predicate<JsonNode> filter) {
        Map<String, String> translationLanguages = getTranslationLanguages();
        return StreamSupport.stream(json.get("captionTracks").spliterator(), false)
                .filter(filter)
                .map(jsonNode -> getTranscript(client, jsonNode, translationLanguages))
                .collect(Collectors.toMap(Transcript::getLanguageCode, transcript -> transcript));
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

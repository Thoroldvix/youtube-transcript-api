package io.github.thoroldvix.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.TranscriptRetrievalException;

import java.util.ArrayList;
import java.util.List;

final class YoutubeApiResponseParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static String getChannelId(String channelJson, String channelName) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(channelJson);
        JsonNode channelId = jsonNode.get("items").
                get(0)
                .get("snippet")
                .get("channelId");
        if (channelId == null) {
            throw new TranscriptRetrievalException("Could not find channel id for the channel with the name: " + channelName);
        }
        return channelId.asText();
    }

    static List<String> getVideoIds(String playlistJson) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(playlistJson);
        List<String> videoIds = new ArrayList<>();

        jsonNode.get("items").forEach(item -> {
            String videoId = item.get("snippet").get("resourceId").get("videoId").asText();
            videoIds.add(videoId);
        });

        return videoIds;
    }

    static String getNextPageToken(String playlistJson) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(playlistJson);
        JsonNode nextPageToken = jsonNode.get("nextPageToken");

        if (nextPageToken == null) {
            return null;
        }

        return nextPageToken.asText();
    }

    static String getChannelPlaylistId(String channelJson) throws TranscriptRetrievalException {
        JsonNode jsonNode = parseJson(channelJson);
        return jsonNode.get("items")
                .get(0)
                .get("contentDetails")
                .get("relatedPlaylists")
                .get("uploads")
                .asText();
    }

    private static JsonNode parseJson(String json) throws TranscriptRetrievalException {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new TranscriptRetrievalException("Failed to parse YouTube API response JSON.", e);
        }
    }
}

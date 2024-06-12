package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.PLAYLIST_ITEMS;

/**
 * Default implementation of {@link YoutubePlaylistTranscripts}
 */
class DefaultYoutubePlaylistTranscripts implements YoutubePlaylistTranscripts {
    private static final String MAX_RESULTS = "50";
    private final YoutubeClient client;
    private final YoutubeTranscriptApi youtubeTranscriptApi;
    private final ObjectMapper objectMapper;
    private final String playlistId;
    private final String apiKey;

    DefaultYoutubePlaylistTranscripts(YoutubeClient client, YoutubeTranscriptApi youtubeTranscriptApi, String playlistId, String apiKey) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
        this.youtubeTranscriptApi = youtubeTranscriptApi;
        this.playlistId = playlistId;
        this.apiKey = apiKey;
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(boolean continueOnError, String cookiesPath) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new HashMap<>();
        List<String> videoIds = getVideoIds();

        for (String videoId : videoIds) {
            try {
                TranscriptList transcriptList;
                if (cookiesPath != null) {
                    transcriptList = youtubeTranscriptApi.listTranscriptsWithCookies(videoId, cookiesPath);
                } else {
                    transcriptList = youtubeTranscriptApi.listTranscripts(videoId);
                }
                transcriptLists.put(videoId, transcriptList);
            } catch (TranscriptRetrievalException e) {
                if (continueOnError) continue;
                throw e;
            }
        }

        return transcriptLists;
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(boolean continueOnError) throws TranscriptRetrievalException {
        return listTranscriptsForPlaylist(continueOnError, null);
    }

    private List<String> getVideoIds() throws TranscriptRetrievalException {
        Map<String, String> params = new HashMap<>(5);
        params.put("key", apiKey);
        params.put("maxResults", MAX_RESULTS);
        params.put("playlistId", playlistId);
        params.put("part", "snippet");

        List<String> videoIds = new ArrayList<>();
        while (true) {
            String playlistJson = client.get(PLAYLIST_ITEMS, params);

            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(playlistJson);
            } catch (JsonProcessingException e) {
                throw new TranscriptRetrievalException("Could not parse playlist JSON for the playlist: " + playlistId, e);
            }

            jsonNode.get("items").forEach(item -> {
                String videoId = item.get("snippet")
                        .get("resourceId")
                        .get("videoId")
                        .asText();
                videoIds.add(videoId);
            });

            JsonNode nextPageToken = jsonNode.get("nextPageToken");
            if (nextPageToken == null) {
                break;
            }
            params.put("pageToken", nextPageToken.asText());
        }

        return videoIds;
    }

}

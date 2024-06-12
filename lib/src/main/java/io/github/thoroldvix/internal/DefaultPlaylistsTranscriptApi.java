package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.*;

/**
 * Default implementation of {@link PlaylistsTranscriptApi}
 */
class DefaultPlaylistsTranscriptApi implements PlaylistsTranscriptApi {
    private static final String MAX_RESULTS = "50";
    private final YoutubeClient client;
    private final YoutubeTranscriptApi youtubeTranscriptApi;
    private final ObjectMapper objectMapper;

    DefaultPlaylistsTranscriptApi(YoutubeClient client, YoutubeTranscriptApi youtubeTranscriptApi) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
        this.youtubeTranscriptApi = youtubeTranscriptApi;
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, String apiKey, String cookiesPath, boolean continueOnError) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new HashMap<>();
        List<String> videoIds = getVideoIds(playlistId, apiKey);

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
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, String apiKey, boolean continueOnError) throws TranscriptRetrievalException {
        return listTranscriptsForPlaylist(playlistId, apiKey, null, continueOnError);
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, String apiKey, String cookiesPath, boolean continueOnError) throws TranscriptRetrievalException {
        String channelId = getChannelId(channelName, apiKey);
        String channelPlaylistId = getChannelPlaylistId(channelId, apiKey);
        return listTranscriptsForPlaylist(channelPlaylistId, apiKey, cookiesPath, continueOnError);
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, String apiKey, boolean continueOnError) throws TranscriptRetrievalException {
        return listTranscriptsForChannel(channelName, apiKey, null, continueOnError);
    }


    private String getChannelPlaylistId(String channelId, String apiKey) throws TranscriptRetrievalException {
        HashMap<String, String> params = new HashMap<>(4);
        params.put("key", apiKey);
        params.put("part", "contentDetails");
        params.put("id", channelId);

        String channelJson = client.get(CHANNELS, params);

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(channelJson);
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException("Could not parse channel JSON for the channel with id: " + channelId, e);
        }
        JsonNode channel = jsonNode.get("items").get(0);

        return channel.get("contentDetails").get("relatedPlaylists").get("uploads").asText();
    }

    private String getChannelId(String channelName, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = new HashMap<>(4);
        params.put("key", apiKey);
        params.put("q", channelName);
        params.put("part", "snippet");
        params.put("type", "channel");

        String searchJson = client.get(SEARCH, params);

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(searchJson);
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException("Could not parse search JSON for the channel: " + channelName, e);
        }

        for (JsonNode item : jsonNode.get("items")) {
            JsonNode snippet = item.get("snippet");
            if (snippet.get("title").asText().equals(channelName)) {
                return snippet.get("channelId").asText();
            }
        }

        throw new TranscriptRetrievalException("Could not find channel with the name: " + channelName);
    }


    private List<String> getVideoIds(String playlistId, String apiKey) throws TranscriptRetrievalException {
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

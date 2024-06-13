package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.github.thoroldvix.api.YtApiV3Endpoint.*;

/**
 * Default implementation of {@link PlaylistsTranscriptApi}
 */
class DefaultPlaylistsTranscriptApi implements PlaylistsTranscriptApi {
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
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = getVideoIds(playlistId, apiKey);

        List<CompletableFuture<Void>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return getTranscriptList(videoId, cookiesPath);
                    } catch (TranscriptRetrievalException e) {
                        if (!continueOnError) {
                            throw new CompletionException(e);
                        }
                    }
                    return null;
                }).thenAccept(transcriptList -> {
                    if (transcriptList != null) {
                        transcriptLists.put(transcriptList.getVideoId(), transcriptList);
                    }
                }))
                .collect(Collectors.toList());

        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof TranscriptRetrievalException) {
                throw (TranscriptRetrievalException) e.getCause();
            } else {
                throw new TranscriptRetrievalException("Failed to retrieve transcripts for playlist: " + playlistId, e);
            }
        }

        return transcriptLists;
    }

    private TranscriptList getTranscriptList(String videoId, String cookiesPath) throws TranscriptRetrievalException {
        if (cookiesPath != null) {
            return youtubeTranscriptApi.listTranscriptsWithCookies(videoId, cookiesPath);
        }
        return youtubeTranscriptApi.listTranscripts(videoId);
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
        Map<String, String> params = createParams(
                "key", apiKey,
                "part", "contentDetails",
                "id", channelId
        );
        String channelJson = client.get(CHANNELS, params);

        JsonNode jsonNode = parseJson(channelJson,
                "Could not parse channel JSON for the channel with id: " + channelId);

        JsonNode channel = jsonNode.get("items").get(0);

        return channel.get("contentDetails").get("relatedPlaylists").get("uploads").asText();
    }

    private String getChannelId(String channelName, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams(
                "key", apiKey,
                "q", channelName,
                "part", "snippet",
                "type", "channel"
        );

        String searchJson = client.get(SEARCH, params);

        JsonNode jsonNode = parseJson(searchJson,
                "Could not parse search JSON for the channel: " + channelName);

        for (JsonNode item : jsonNode.get("items")) {
            JsonNode snippet = item.get("snippet");
            if (snippet.get("title").asText().equals(channelName)) {
                return snippet.get("channelId").asText();
            }
        }

        throw new TranscriptRetrievalException("Could not find channel with the name: " + channelName);
    }


    private List<String> getVideoIds(String playlistId, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams(
                "key", apiKey,
                "playlistId", playlistId,
                "part", "snippet",
                "maxResults", "50"
        );

        List<String> videoIds = new ArrayList<>();

        while (true) {
            String playlistJson = client.get(PLAYLIST_ITEMS, params);
            JsonNode jsonNode = parseJson(playlistJson,
                    "Could not parse playlist JSON for the playlist: " + playlistId);
            extractVideoId(jsonNode, videoIds);
            JsonNode nextPageToken = jsonNode.get("nextPageToken");

            if (nextPageToken == null) {
                break;
            }

            params.put("pageToken", nextPageToken.asText());
        }

        return videoIds;
    }

    private void extractVideoId(JsonNode jsonNode, List<String> videoIds) {
        jsonNode.get("items").forEach(item -> {
            String videoId = item.get("snippet")
                    .get("resourceId")
                    .get("videoId")
                    .asText();
            videoIds.add(videoId);
        });
    }

    private Map<String, String> createParams(String... params) {
        Map<String, String> map = new HashMap<>(params.length / 2);
        for (int i = 0; i < params.length; i += 2) {
            map.put(params[i], params[i + 1]);
        }
        return map;
    }

    private JsonNode parseJson(String json, String errorMessage) throws TranscriptRetrievalException {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new TranscriptRetrievalException(errorMessage, e);
        }
    }
}

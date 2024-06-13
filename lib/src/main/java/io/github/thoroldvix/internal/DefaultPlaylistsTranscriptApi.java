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

    private static void joinFutures(List<CompletableFuture<Void>> futures, String playlistId) throws TranscriptRetrievalException {
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
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, TranscriptRequest request) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = getVideoIds(playlistId, request.getApiKey());

        List<CompletableFuture<Void>> futures = videoIds.stream().map(videoId -> CompletableFuture.supplyAsync(() -> {
            try {
                return getTranscriptList(videoId, request.getCookiesPath());
            } catch (TranscriptRetrievalException e) {
                if (request.isStopOnError()) {
                    throw new CompletionException(e);
                }
            }
            return null;
        }).thenAccept(transcriptList -> {
            if (transcriptList != null) {
                transcriptLists.put(transcriptList.getVideoId(), transcriptList);
            }
        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcriptLists;
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, TranscriptRequest request) throws TranscriptRetrievalException {
        String channelId = getChannelId(channelName, request.getApiKey());
        String channelPlaylistId = getChannelPlaylistId(channelId, request.getApiKey());
        return listTranscriptsForPlaylist(channelPlaylistId, request);
    }

    @Override
    public Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = listTranscriptsForPlaylist(playlistId, request);
        Map<String, TranscriptContent> transcripts = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = transcriptLists.values().stream().map(transcriptList -> CompletableFuture.supplyAsync(() -> {
            try {
                return transcriptList.findTranscript(languageCodes).fetch();
            } catch (TranscriptRetrievalException e) {
                if (request.isStopOnError()) {
                    throw new CompletionException(e);
                }
            }
            return null;
        }).thenAccept(transcriptContent -> {
            if (transcriptContent != null) {
                transcripts.put(transcriptList.getVideoId(), transcriptContent);
            }
        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcripts;
    }

    @Override
    public Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        String channelId = getChannelId(channelName, request.getApiKey());
        String channelPlaylistId = getChannelPlaylistId(channelId, request.getApiKey());
        return getTranscriptsForPlaylist(channelPlaylistId, request, languageCodes);
    }

    private String getChannelPlaylistId(String channelId, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams("key", apiKey, "part", "contentDetails", "id", channelId);
        String channelJson = client.get(CHANNELS, params);

        JsonNode jsonNode = parseJson(channelJson, "Could not parse channel JSON for the channel with id: " + channelId);

        JsonNode channel = jsonNode.get("items").get(0);

        return channel.get("contentDetails").get("relatedPlaylists").get("uploads").asText();
    }

    private String getChannelId(String channelName, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams("key", apiKey, "q", channelName, "part", "snippet", "type", "channel");

        String searchJson = client.get(SEARCH, params);

        JsonNode jsonNode = parseJson(searchJson, "Could not parse search JSON for the channel: " + channelName);

        for (JsonNode item : jsonNode.get("items")) {
            JsonNode snippet = item.get("snippet");
            if (snippet.get("title").asText().equals(channelName)) {
                return snippet.get("channelId").asText();
            }
        }

        throw new TranscriptRetrievalException("Could not find channel with the name: " + channelName);
    }


    private List<String> getVideoIds(String playlistId, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams("key", apiKey, "playlistId", playlistId, "part", "snippet", "maxResults", "50");

        List<String> videoIds = new ArrayList<>();

        while (true) {
            String playlistJson = client.get(PLAYLIST_ITEMS, params);
            JsonNode jsonNode = parseJson(playlistJson, "Could not parse playlist JSON for the playlist: " + playlistId);
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
            String videoId = item.get("snippet").get("resourceId").get("videoId").asText();
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

    private TranscriptList getTranscriptList(String videoId, String cookiesPath) throws TranscriptRetrievalException {
        if (cookiesPath != null) {
            return youtubeTranscriptApi.listTranscriptsWithCookies(videoId, cookiesPath);
        }
        return youtubeTranscriptApi.listTranscripts(videoId);
    }
}

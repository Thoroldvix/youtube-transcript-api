package io.github.thoroldvix.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static io.github.thoroldvix.api.YtApiV3Endpoint.*;

/**
 * Default implementation of {@link PlaylistsTranscriptApi}
 */
class DefaultPlaylistsTranscriptApi implements PlaylistsTranscriptApi {
    private final YoutubeClient client;
    private final YoutubeTranscriptApi youtubeTranscriptApi;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    DefaultPlaylistsTranscriptApi(YoutubeClient client, YoutubeTranscriptApi youtubeTranscriptApi) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
        this.youtubeTranscriptApi = youtubeTranscriptApi;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, String apiKey, String cookiesPath, boolean continueOnError) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = getVideoIds(playlistId, apiKey);
        List<Future<Void>> futures = new ArrayList<>();

        for (String videoId : videoIds) {
            futures.add(executor.submit(() -> {
                try {
                    TranscriptList transcriptList = getTranscriptList(videoId, cookiesPath);
                    transcriptLists.put(videoId, transcriptList);
                } catch (TranscriptRetrievalException e) {
                    if (!continueOnError) throw e;
                }
                return null;
            }));
        }

        executor.shutdown();

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                if (!continueOnError) {
                    executor.shutdownNow();
                    throw new TranscriptRetrievalException("Error retrieving transcripts", e);
                }
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

        List<Future<String>> futures = new ArrayList<>();
        List<String> videoIds = new ArrayList<>();

        while (true) {

            futures.add(executor.submit(() -> client.get(PLAYLIST_ITEMS, params)));

            for (Future<String> future : futures) {
                try {
                    String playlistJson = future.get();
                    JsonNode jsonNode = parseJson(playlistJson,
                            "Could not parse playlist JSON for the playlist: " + playlistId);

                    extractVideoId(jsonNode, videoIds);

                    JsonNode nextPageToken = jsonNode.get("nextPageToken");
                    if (nextPageToken == null) {
                        return videoIds;
                    }
                    params.put("pageToken", nextPageToken.asText());

                } catch (InterruptedException | ExecutionException e) {
                        executor.shutdownNow();
                        throw new TranscriptRetrievalException("Error retrieving transcripts for playlist: " + playlistId, e);
                    }
                }
            }
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

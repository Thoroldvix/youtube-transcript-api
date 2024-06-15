package io.github.thoroldvix.internal;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.thoroldvix.api.YtApiV3Endpoint.*;

final class YoutubeApi {

    private final YoutubeClient client;

    YoutubeApi(YoutubeClient client) {
        this.client = client;
    }

    String getChannelPlaylistId(String channelName, String apiKey) throws TranscriptRetrievalException {
        String channelId = getChannelId(channelName, apiKey);
        Map<String, String> params = createParams("key", apiKey, "part", "contentDetails", "id", channelId);
        String channelJson = client.get(CHANNELS, params);

        return YoutubeApiResponseParser.getChannelPlaylistId(channelJson);
    }

    List<String> getVideoIds(String playlistId, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams("key", apiKey, "playlistId", playlistId, "part", "snippet", "maxResults", "50");
        List<String> videoIds = new ArrayList<>();

        while (true) {
            String playlistJson = client.get(PLAYLIST_ITEMS, params);

            videoIds.addAll(YoutubeApiResponseParser.getVideoIds(playlistJson));
            String nextPageToken = YoutubeApiResponseParser.getNextPageToken(playlistJson);

            if (nextPageToken == null) {
                break;
            }

            params.put("pageToken", nextPageToken);
        }

        return videoIds;
    }

    private String getChannelId(String channelName, String apiKey) throws TranscriptRetrievalException {
        Map<String, String> params = createParams("key", apiKey, "q", channelName, "part", "snippet", "type", "channel");

        String searchJson = client.get(SEARCH, params);
        return YoutubeApiResponseParser.getChannelId(searchJson, channelName);
    }


    private Map<String, String> createParams(String... params) {
        Map<String, String> map = new HashMap<>(params.length / 2);
        for (int i = 0; i < params.length; i += 2) {
            map.put(params[i], params[i + 1]);
        }
        return map;
    }
}

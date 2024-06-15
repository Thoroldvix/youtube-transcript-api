package io.github.thoroldvix.internal;


import io.github.thoroldvix.api.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link YoutubeTranscriptApi}.
 */
final class DefaultYoutubeTranscriptApi implements YoutubeTranscriptApi {
    private final VideoPageFetcher videoPageFetcher;
    private final YoutubeApi youtubeApi;
    private final YoutubeClient client;

    DefaultYoutubeTranscriptApi(YoutubeClient client, FileLinesReader fileLinesReader) {
        this.videoPageFetcher = new VideoPageFetcher(client, fileLinesReader);
        this.youtubeApi = new YoutubeApi(client);
        this.client = client;
    }

    private static TranscriptContent transcriptContentSupplier(TranscriptRequest request, String[] languageCodes, TranscriptList transcriptList) {
        try {
            return transcriptList.findTranscript(languageCodes).fetch();
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }
        return null;
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
    public TranscriptContent getTranscriptWithCookies(String videoId, String cookiesPath, String... languageCodes) throws TranscriptRetrievalException {
        return listTranscriptsWithCookies(videoId, cookiesPath)
                .findTranscript(languageCodes)
                .fetch();
    }

    @Override
    public TranscriptContent getTranscript(String videoId, String... languageCodes) throws TranscriptRetrievalException {
        return listTranscripts(videoId)
                .findTranscript(languageCodes)
                .fetch();
    }

    @Override
    public TranscriptList listTranscriptsWithCookies(String videoId, String cookiesPath) throws TranscriptRetrievalException {
        validateVideoId(videoId);
        TranscriptListExtractor extractor = new TranscriptListExtractor(client, videoId);
        String videoPageHtml = videoPageFetcher.fetch(videoId, cookiesPath);
        return extractor.extract(videoPageHtml);
    }

    @Override
    public TranscriptList listTranscripts(String videoId) throws TranscriptRetrievalException {
        validateVideoId(videoId);
        TranscriptListExtractor extractor = new TranscriptListExtractor(client, videoId);
        String videoPageHtml = videoPageFetcher.fetch(videoId);
        return extractor.extract(videoPageHtml);
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForPlaylist(String playlistId, TranscriptRequest request) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = new ConcurrentHashMap<>();
        List<String> videoIds = youtubeApi.getVideoIds(playlistId, request.getApiKey());

        List<CompletableFuture<Void>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(() -> transcriptListSupplier(request, videoId))
                        .thenAccept(transcriptList -> {
                            if (transcriptList != null) {
                                transcriptLists.put(transcriptList.getVideoId(), transcriptList);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcriptLists;
    }

    @Override
    public Map<String, TranscriptContent> getTranscriptsForPlaylist(String playlistId, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        Map<String, TranscriptList> transcriptLists = listTranscriptsForPlaylist(playlistId, request);
        Map<String, TranscriptContent> transcripts = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = transcriptLists.values().stream()
                .map(transcriptList -> CompletableFuture.supplyAsync(() -> transcriptContentSupplier(request, languageCodes, transcriptList))
                        .thenAccept(transcriptContent -> {
                            if (transcriptContent != null) {
                                transcripts.put(transcriptList.getVideoId(), transcriptContent);
                            }
                        })).collect(Collectors.toList());

        joinFutures(futures, playlistId);

        return transcripts;
    }

    @Override
    public Map<String, TranscriptList> listTranscriptsForChannel(String channelName, TranscriptRequest request) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.getChannelPlaylistId(channelName, request.getApiKey());
        return listTranscriptsForPlaylist(channelPlaylistId, request);
    }

    @Override
    public Map<String, TranscriptContent> getTranscriptsForChannel(String channelName, TranscriptRequest request, String... languageCodes) throws TranscriptRetrievalException {
        String channelPlaylistId = youtubeApi.getChannelPlaylistId(channelName, request.getApiKey());
        return getTranscriptsForPlaylist(channelPlaylistId, request, languageCodes);
    }

    private void validateVideoId(String videoId) {
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            throw new IllegalArgumentException("Invalid video id: " + videoId);
        }
    }

    private TranscriptList transcriptListSupplier(TranscriptRequest request, String videoId) {
        try {
            String cookiesPath = request.getCookiesPath();
            if (cookiesPath != null) {
                return listTranscriptsWithCookies(videoId, cookiesPath);
            }
            return listTranscripts(videoId);
        } catch (TranscriptRetrievalException e) {
            if (request.isStopOnError()) {
                throw new CompletionException(e);
            }
        }
        return null;
    }
}

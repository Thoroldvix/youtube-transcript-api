package io.github.thoroldvix.internal;


import io.github.thoroldvix.api.*;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link YoutubeTranscriptApi}.
 */
final class DefaultYoutubeTranscriptApi implements YoutubeTranscriptApi {

    private static final String FAILED_TO_GIVE_COOKIES_CONSENT = "Failed to automatically give consent to saving cookies";
    private static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";

    private final YoutubeClient client;
    private final FileLinesReader fileLinesReader;

    DefaultYoutubeTranscriptApi(YoutubeClient client, FileLinesReader fileLinesReader) {
        this.client = client;
        this.fileLinesReader = fileLinesReader;
    }

    private static HttpCookie createCookie(String[] parts) {
        String domain = parts[0];
        boolean secure = Boolean.parseBoolean(parts[1]);
        String path = parts[2];
        boolean httpOnly = Boolean.parseBoolean(parts[3]);
        long expiration = Long.parseLong(parts[4]);
        String name = parts[5];
        String value = parts[6];

        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(expiration);
        return cookie;
    }

    private static boolean containsConsentPage(String videoPageHtml) {
        String consentPagePattern = "action=\"https://consent.youtube.com/s\"";
        return videoPageHtml.contains(consentPagePattern);
    }

    private static String extractConsentCookie(String videoId, String html) throws TranscriptRetrievalException {
        Pattern consentCookiePattern = Pattern.compile("name=\"v\" value=\"(.*?)\"");
        Matcher matcher = consentCookiePattern.matcher(html);
        if (!matcher.find()) {
            throw new TranscriptRetrievalException(videoId, FAILED_TO_GIVE_COOKIES_CONSENT);
        }
        return String.format("CONSENT=YES+%s", matcher.group(1));
    }

    @Override
    public TranscriptList listTranscriptsWithCookies(String videoId, String cookiesPath) throws TranscriptRetrievalException {
        validateVideoId(videoId);
        List<HttpCookie> cookies = loadCookies(videoId, cookiesPath);
        String cookieHeader = cookies.stream()
                .map(HttpCookie::toString)
                .collect(Collectors.joining("; "));
        String videoPageHtml = fetchVideoPageHtml(videoId, cookieHeader);

        return TranscriptListJSON.from(videoPageHtml, client, videoId)
                .transcriptList();
    }

    private void validateVideoId(String videoId) {
        if (!videoId.matches("[a-zA-Z0-9_-]{11}")) {
            throw new IllegalArgumentException("Invalid video id: " + videoId);
        }
    }

    private List<HttpCookie> loadCookies(String videoId, String cookiesPath) throws TranscriptRetrievalException {
        try {
            List<String> cookieLines = fileLinesReader.readLines(cookiesPath);
            return cookieLines.stream()
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> line.split("\t"))
                    .filter(parts -> parts.length >= 7)
                    .map(DefaultYoutubeTranscriptApi::createCookie)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new TranscriptRetrievalException(videoId, String.format("Failed to load cookies from a file: %s.", cookiesPath), e);
        }
    }

    private String fetchVideoPageHtml(String videoId, String cookieHeader) throws TranscriptRetrievalException {
        Map<String, String> requestHeaders = createRequestHeaders(cookieHeader);
        return client.get(YOUTUBE_WATCH_URL + videoId, requestHeaders);
    }

    private Map<String, String> createRequestHeaders(String cookieHeader) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Language", "en-US");
        if (cookieHeader != null) {
            headers.put("Cookie", cookieHeader);
        }
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public TranscriptList listTranscripts(String videoId) throws TranscriptRetrievalException {
        validateVideoId(videoId);
        String videoPageHtml = fetchVideoPageHtml(videoId, null);
        if (containsConsentPage(videoPageHtml)) {
            videoPageHtml = retryWithConsentCookie(videoId, videoPageHtml);
        }
        return TranscriptListJSON.from(videoPageHtml, client, videoId)
                .transcriptList();
    }

    private String retryWithConsentCookie(String videoId, String videoPageHtml) throws TranscriptRetrievalException {
        String consentCookie = extractConsentCookie(videoId, videoPageHtml);
        Map<String, String> requestHeaders = createRequestHeaders(consentCookie);
        videoPageHtml = client.get(YOUTUBE_WATCH_URL + videoId, requestHeaders);
        if (containsConsentPage(videoPageHtml)) {
            throw new TranscriptRetrievalException(videoId, FAILED_TO_GIVE_COOKIES_CONSENT);
        }
        return videoPageHtml;
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
}

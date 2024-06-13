# 📝 YouTube Transcript API

![Java CI](https://github.com/thoroldvix/youtube-transcript-api/actions/workflows/ci.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.thoroldvix/youtube-transcript-api)](https://search.maven.org/artifact/io.github.thoroldvix/youtube-transcript-api)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://thoroldvix.github.io/youtube-transcript-api/javadoc/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## ⚠️WARNING ⚠️

### This library uses undocumented YouTube API, so it's possible that it will stop working at any time. Use at your own risk.

## 📖 Introduction

Java library which allows you to retrieve subtitles/transcripts for a YouTube video.
It supports manual and automatically generated subtitles, bulk transcript retrieval for all videos in the playlist or
on the channel and does not use headless browser for scraping.
Inspired by [Python library](https://github.com/jdepoix/youtube-transcript-api).

## 🤖 Features

✅ Manual transcripts retrieval

✅ Automatically generated transcripts retrieval

✅ Bulk transcript retrieval for all videos in the playlist or channel

✅ Transcript translation

✅ Transcript formatting

✅ Easy-to-use API

✅ Minimal dependencies (Jackson for XML/JSON processing and Apache Commons Text for XML tag unescaping)

✅ Supports Java 11 and above

## 🛠️ Installation

### Maven

```xml

<dependency>
    <groupId>io.github.thoroldvix</groupId>
    <artifactId>youtube-transcript-api</artifactId>
    <version>0.2.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.thoroldvix:youtube-transcript-api:0.2.0'
```

### Gradle (kts)

```kotlin
implementation("io.github.thoroldvix:youtube-transcript-api:0.2.0")
```

## 🔰 Getting Started

To start using YouTube Transcript API, you need to create an instance of `YoutubeTranscriptApi` by
calling `createDefault`
method of `TranscriptApiFactory`. Then you can call `listTranscripts` to get a list of all available transcripts for a
video:

```java
// Create a new default YoutubeTranscriptApi instance
YoutubeTranscriptApi youtubeTranscriptApi = TranscriptApiFactory.createDefault();

// Retrieve all available transcripts for a given video
TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts("videoId");
```

`TranscripList` is an iterable which contains all available transcripts for a video and provides methods
for [finding specific transcripts](#find-transcripts) by language or by type (manual or automatically generated).

```java
TranscriptList transcriptList = youtubeTranscriptApi.listTranscripts("videoId");

// Iterate over transcript list
for(Transcript transcript : transcriptList) {
        System.out.println(transcript);
}

// Find transcript in specific language
Transcript transcript = transcriptList.findTranscript("en");

// Find manually created transcript
Transcript manualyCreatedTranscript = transcriptList.findManualTranscript("en");

// Find automatically generated transcript
Transcript automaticallyGeneratedTranscript = transcriptList.findGeneratedTranscript("en");
```

`Transcript` object contains [transcript metadata](#transcript-metadata) and provides methods for translating the
transcript to another language
and fetching the actual content of the transcript.

```java
Transcript transcript = transcriptList.findTranscript("en");

// Translate transcript to another language
Transcript translatedTranscript = transcript.translate("de");

// Retrieve transcript content
TranscriptContent transcriptContent = transcript.fetch();
```

`TranscriptContent` contains actual transcript content, storing it as a list of `Fragment`.
Each `Fragment` contains 'text', 'start' and 'duration'
attributes. If you try to print the `TranscriptContent`, you will get the output looking like this:

```text
content=[{text='Text',start=0.0,dur=1.54},{text='Another text',start=1.54,dur=4.16}]
```

> **Note:** If you want to get transcript content in a different format, refer
> to [Use Formatters](#use-formatters).

You can also use `getTranscript`:

```java
TranscriptContent transcriptContent = youtubeTranscriptApi.getTranscript("videoId", "en");
```

This is equivalent to:

```java
TranscriptContent transcriptContent = youtubeTranscriptApi.listTranscripts("videoId")
        .findTranscript("en")
        .fetch();
```

Given that English is the most common language, you can omit the language code, and it will default to English:

```java
// Retrieve transcript content in english
TranscriptContent transcriptContent = youtubeTranscriptApi.listTranscripts("videoId")
        //no language code defaults to english
        .findTranscript()
        .fetch();
// Or
TranscriptContent transcriptContent = youtubeTranscriptApi.getTranscript("videoId");
```

For bulk transcript retrieval see [Bulk Transcript Retrieval](#bulk-transcript-retrieval).

## 🔧 Detailed Usage

### Use fallback language

In case if desired language is not available, instead of getting an exception you can pass some other languages that
will be used as a fallback.

For example:

```java
TranscriptContent transcriptContent = youtubeTranscriptApi.listTranscripts("videoId")
        .findTranscript("de", "en")
        .fetch();

// Or
TranscriptContent transcriptContent = youtubeTranscriptApi.getTranscript("videoId", "de", "en");
```

It will first look for a transcript in German, and if it doesn't find one, it will then look for one in English, and so
on.

### Find transcripts

By default, `findTranscript` will always pick manually created transcripts first and then automatically generated ones.
If you want to get only automatically generated or only manually created transcripts, you can use `findManualTranscript`
or `findGeneratedTranscript`.

```java
// Retrieve manually created transcript
Transcript manualyCreatedTranscript = transcriptList.findManualTranscript("en");

// Retrieve automatically generated transcript
Transcript automaticallyGeneratedTranscript = transcriptList.findGeneratedTranscript("en");
```

`findGeneratedTranscript` and `findManualTranscript` both
support [fallback languages](#use-fallback-language).

### Transcript metadata

`Transcript` object contains several methods for retrieving transcript metadata:

```java
String videoId = transcript.getVideoId();

String language = transcript.getLanguage();

String languageCode = transcript.getLanguageCode();

// API URL used to fetch transcript content
String apiUrl = transcript.getApiUrl();

// Whether it has been manually created or automatically generated by YouTube
boolean isGenerated = transcript.isGenerated();

// Whether this transcript can be translated or not
boolean isTranslatable = transcript.isTranslatable();

// Set of language codes which represent available translation languages
Set<String> translationLanguages = transcript.getTranslationLanguages();
```

### Use Formatters

By default, if you try to print `TranscriptContent` it will return the following string representation:

```text
content=[{text='Text',start=0.0,dur=1.54},{text='Another text',start=1.54,dur=4.16}]
```

Since this default format may not be suitable for all scenarios, you can implement the `TranscriptFormatter` interface
to customize the formatting of the content.

```java
// Create a new custom formatter
Formatter transcriptFormatter = new MyCustomFormatter();

// Format transcript content
String formattedContent = transcriptFormatter.format(transcriptContent);
```

The library offers several built-in formatters:

- `JSONFormatter` - Formats content as JSON
- `JSONPrettyFormatter` - Formats content as pretty-printed JSON
- `TextFormatter` - Formats content as plain text without timestamps
- `WebVTTFormatter` - Formats content as [WebVTT](https://developer.mozilla.org/en-US/docs/Web/API/WebVTT_API)
- `SRTFormatter` - Formats content as [SRT](https://www.3playmedia.com/blog/create-srt-file/)

These formatters can be accessed from the `TranscriptFormatters` class:

```java
// Get json formatter
TranscriptFormatter jsonFormatter = TranscriptFormatters.jsonFormatter();

String formattedContent = jsonFormatter.format(transcriptContent);
````

### YoutubeClient Customization

By default, `YoutubeTranscriptApi` uses Java 11 HttpClient for making requests to YouTube, if you want to use a
different client,
you can create your own YouTube client by implementing the `YoutubeClient` interface and passing it to
the `YoutubeTranscriptApiFactory` `createWithClient` method.

```java
// Create a new custom YoutubeClient
YoutubeClient youtubeClient = new MyCustomYoutubeClient();

// Create YoutubeTranscriptApi instance with custom YouTubeClient
YoutubeTranscriptApi youtubeTranscriptApi = TranscriptApiFactory.createWithClient(youtubeClient);
```

### Cookies

Some videos may be age-restricted, requiring authentication to access the transcript.
To achieve this, obtain access to the desired video in a browser and download the cookies in Netscape format, storing
them as a TXT file.
You can use extensions
like [Get cookies.txt LOCALLY](https://chromewebstore.google.com/detail/get-cookiestxt-locally/cclelndahbckbenkjhflpdbgdldlbecc)
for Chrome or [cookies.txt](https://addons.mozilla.org/en-US/firefox/addon/cookies-txt/) for Firefox to do this.
`YoutubeTranscriptApi` contains `listTranscriptsWithCookies` and `getTranscriptWithCookies` which accept a path to the
cookies.txt file.

```java
// Retrieve transcript list
TranscriptList transcriptList = youtubeTranscriptApi.listTranscriptsWithCookies("videoId", "path/to/cookies.txt");

// Get transcript content
TranscriptContent transcriptContent = youtubeTranscriptApi.getTranscriptWithCookies("videoId", "path/to/cookies.txt", "en");
```

### Bulk Transcript Retrieval

All bulk transcript retrieval operations are done via the `PlaylistsTranscriptApi` interface. Same as with the
`YoutubeTranscriptApi`,
you can create a new instance of the PlaylistsTranscriptApi by calling the `createDefaultPlaylistsApi` method of the
`TranscriptApiFactory`.
Playlists and channels information is retrieved from
the [YouTube V3 API](https://developers.google.com/youtube/v3/docs/),
so you will need to provide API key for all methods.

```java
// Create a new default PlaylistsTranscriptApi instance
PlaylistsTranscriptApi playlistsTranscriptApi = TranscriptApiFactory.createDefaultPlaylistsApi();

// Retrieve all available transcripts for a given playlist
Map<String, TranscriptList> transcriptLists = playlistsTranscriptApi.listTranscriptsForPlaylist(
        "playlistId",
        "apiKey",
        true);

// Retrieve all available transcripts for a given channel
Map<String, TranscriptList> transcriptLists = playlistsTranscriptApi.listTranscriptsForChannel(
        "channelName",
        "apiKey",
        true);
```

As you can see, there is also a boolean flag `continueOnError`, which tells whether to continue if transcript retrieval
fails for a video or not. For example, if it's set to `true`, all transcripts that could not be retrieved will be
skipped, if
it's set to `false`, operation will fail fast on the first error.

All methods are also have overloaded versions which accept path to [cookies.txt](#cookies) file.

```java
// Retrieve all available transcripts for a given playlist
Map<String, TranscriptList> transcriptLists = playlistsTranscriptApi.listTranscriptsForPlaylist(
        "playlistId",
        "apiKey",
        true,
        "path/to/cookies.txt"
);

// Retrieve all available transcripts for a given channel
Map<String, TranscriptList> transcriptLists = playlistsTranscriptApi.listTranscriptsForChannel(
        "channelName",
        "apiKey",
        true,
        "path/to/cookies.txt"
);
```

## 🤓 How it works

Within each YouTube video page, there exists JSON data containing all the transcript information, including an
undocumented API URL embedded within its HTML. This JSON looks like this:

```json
{
  "captions": {
    "playerCaptionsTracklistRenderer": {
      "captionTracks": [
        {
          "baseUrl": "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ&asr_langs=de,en,es,fr,it,ja,ko,nl,pt,ru&caps=asr&xorp=true&hl=de&ip=0.0.0.0&ipbits=0&expire=1570645639&sparams=ip,ipbits,expire,v,asr_langs,caps,xorp&signature=5939E534881E9A14C14BCEDF370DE7A4E5FD4BE0.01ABE3BA9B2BCDEC6C51D6A9D9F898460495F0F2&key=yt8&lang=de",
          "name": {
            "simpleText": "Deutsch"
          },
          "vssId": ".de",
          "languageCode": "de",
          "isTranslatable": true
        },
        {
          "baseUrl": "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ&asr_langs=de,en,es,fr,it,ja,ko,nl,pt,ru&caps=asr&xorp=true&hl=de&ip=0.0.0.0&ipbits=0&expire=1570645639&sparams=ip,ipbits,expire,v,asr_langs,caps,xorp&signature=5939E534881E9A14C14BCEDF370DE7A4E5FD4BE0.01ABE3BA9B2BCDEC6C51D6A9D9F898460495F0F2&key=yt8&lang=en",
          "name": {
            "simpleText": "Englisch"
          },
          "vssId": ".en",
          "languageCode": "en",
          "kind": "asr",
          "isTranslatable": true
        }
      ],
      "translationLanguages": [
        {
          "languageCode": "af",
          "languageName": {
            "simpleText": "Afrikaans"
          }
        }
      ]
    }
  }
}
```

This library works by making a single GET request to the YouTube page of the specified video, extracting the JSON data
from the HTML, and parsing it to obtain a list of all available transcripts. To fetch the transcript content, it then
sends a GET request to the API URL extracted from the JSON. The YouTube API returns the transcript content in XML
format, like this:

```xml
<?xml version="1.0" encoding="utf-8" ?>
<transcript>
    <text start="0" dur="1.54">Some text</text>
    <text start="1.54" dur="4.16">Some additional text</text>
</transcript>
```

## 📖 License

This library is licensed under the MIT License. See
the [LICENSE](https://github.com/dignifiedquire/youtube-transcript-api/blob/master/LICENSE) file for more information.


       







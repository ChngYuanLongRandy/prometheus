package com.prometheus.watcher.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.LanguageCode;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 * Wraps the Amazon Polly SynthesizeSpeech API.
 * Standard Joanna voice, 3,000-character limit per call.
 * Text longer than 3,000 chars is chunked and the MP3 streams are concatenated.
 */
@Service
public class PollyTtsService {

    private static final Logger log = LoggerFactory.getLogger(PollyTtsService.class);

    // Polly standard voice limit per SynthesizeSpeech call.
    private static final int CHUNK_SIZE = 3_000;
    // Cap total input to 3 chunks; anything beyond is truncated.
    private static final int MAX_CHARS = 9_000;

    private final PollyClient polly;

    public PollyTtsService(PollyClient polly) {
        this.polly = polly;
    }

    public byte[] synthesize(String text) throws IOException {
        String input = text.length() > MAX_CHARS
                ? text.substring(0, MAX_CHARS) + "... content truncated."
                : text;

        if (input.length() <= CHUNK_SIZE) {
            return synthesizeChunk(input);
        }

        List<String> chunks = splitAtWordBoundary(input, CHUNK_SIZE);
        log.debug("Splitting {} chars into {} Polly chunks", input.length(), chunks.size());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String chunk : chunks) {
            byte[] part = synthesizeChunk(chunk);
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private byte[] synthesizeChunk(String text) throws IOException {
        SynthesizeSpeechRequest req = SynthesizeSpeechRequest.builder()
                .engine(Engine.STANDARD)
                .voiceId(VoiceId.JOANNA)
                .languageCode(LanguageCode.EN_US)
                .outputFormat(OutputFormat.MP3)
                .text(text)
                .build();
        try (ResponseInputStream<SynthesizeSpeechResponse> stream = polly.synthesizeSpeech(req)) {
            return stream.readAllBytes();
        }
    }

    private static List<String> splitAtWordBoundary(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) end = lastSpace + 1;
            }
            result.add(text.substring(start, end).strip());
            start = end;
        }
        return result;
    }
}

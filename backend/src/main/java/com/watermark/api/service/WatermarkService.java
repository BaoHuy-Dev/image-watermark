package com.watermark.api.service;

import com.watermark.sdk.WatermarkEngine;
import com.watermark.sdk.WatermarkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkService {

    private final WatermarkEngine engine;

    public byte[] embedUserWatermark(byte[] imageBytes, String userId, String email) throws IOException {
        log.info("Embedding watermark for user: {} ({})", userId, email);
        byte[] result = engine.embedUserInfo(imageBytes, userId, email);
        log.info("Watermark embedded. Size: {} → {} bytes", imageBytes.length, result.length);
        return result;
    }

    public byte[] embedCustomText(byte[] imageBytes, String text) throws IOException {
        log.info("Embedding custom watermark: '{}'", text);
        return engine.embed(imageBytes, text);
    }

    public WatermarkResult extractWatermark(byte[] imageBytes) throws IOException {
        WatermarkResult result = engine.extract(imageBytes);
        log.info("Extract result: {}", result);
        return result;
    }
}

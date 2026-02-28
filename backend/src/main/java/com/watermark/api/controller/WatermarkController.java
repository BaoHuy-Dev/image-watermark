package com.watermark.api.controller;

import com.watermark.api.service.WatermarkService;
import com.watermark.sdk.WatermarkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final WatermarkService service;

    /**
     * POST /api/watermark/embed
     * Nhúng watermark ẩn chứa thông tin user vào ảnh.
     */
    @PostMapping("/embed")
    public ResponseEntity<byte[]> embed(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("userEmail") String userEmail) {
        try {
            byte[] result = service.embedUserWatermark(file.getBytes(), userId, userEmail);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"watermarked.png\"")
                    .body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Embed failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Embed error", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * POST /api/watermark/extract
     * Trích xuất watermark ẩn từ ảnh.
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extract(
            @RequestParam("file") MultipartFile file) {
        try {
            WatermarkResult result = service.extractWatermark(file.getBytes());
            if (!result.isFound()) {
                return ResponseEntity.ok(Map.of(
                        "found", false,
                        "message", "No watermark found in this image."));
            }
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "watermark", result.getText()));
        } catch (IOException e) {
            log.error("Extract error", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/watermark/health
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "watermark-api");
    }
}

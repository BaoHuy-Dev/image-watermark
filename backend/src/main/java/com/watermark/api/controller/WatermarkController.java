package com.watermark.api.controller;

import com.watermark.sdk.WatermarkEngine;
import com.watermark.sdk.WatermarkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final WatermarkEngine engine;

    @PostMapping("/embed")
    public ResponseEntity<byte[]> embed(@RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "userEmail", defaultValue = "") String email) {
        try {
            byte[] result = engine.embedUserInfo(file.getBytes(), userId, email);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"watermarked.png\"")
                    .body(result);
        } catch (Exception e) {
            log.error("Embed failed: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            WatermarkResult result = engine.extractAuto(file.getBytes(), file.getOriginalFilename());
            return ResponseEntity.ok(Map.of(
                    "found", result.isFound(),
                    "watermark", result.isFound() ? result.getText() : ""));
        } catch (Exception e) {
            log.error("Extract failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("found", false, "watermark", ""));
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "watermark-api", "status", "ok");
    }
}

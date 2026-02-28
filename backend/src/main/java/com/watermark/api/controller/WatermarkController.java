package com.watermark.api.controller;

import com.watermark.api.entity.WatermarkFingerprint;
import com.watermark.api.repository.FingerprintRepository;
import com.watermark.sdk.PerceptualHash;
import com.watermark.sdk.WatermarkEngine;
import com.watermark.sdk.WatermarkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final WatermarkEngine engine;
    private final FingerprintRepository fingerprintRepository;

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
            String fileName = file.getOriginalFilename();
            byte[] fileBytes = file.getBytes();

            // Strategy 1: Try direct watermark extraction (metadata, LSB, QIM)
            WatermarkResult directResult = engine.extractAuto(fileBytes, fileName);
            if (directResult.isFound()) {
                log.info("Watermark found via direct extraction");
                return ResponseEntity.ok(Map.of(
                        "found", true,
                        "watermark", directResult.getText(),
                        "method", "direct"));
            }

            // Strategy 2: Fingerprint matching (works for screenshots!)
            Map<String, Object> fpResult = matchFingerprint(fileBytes, fileName);
            if (fpResult != null) {
                log.info("Watermark found via fingerprint matching");
                return ResponseEntity.ok(fpResult);
            }

            // Not found
            return ResponseEntity.ok(Map.of("found", false, "watermark", ""));
        } catch (Exception e) {
            log.error("Extract failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("found", false, "watermark", ""));
        }
    }

    /**
     * Match an uploaded image against stored fingerprints.
     * This is the key feature that makes SCREENSHOT matching work.
     */
    private Map<String, Object> matchFingerprint(byte[] fileBytes, String fileName) {
        try {
            String uploadedHash;

            // For PDFs, hash the first page
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(fileBytes);
                org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                BufferedImage page = renderer.renderImageWithDPI(0, 150f);
                uploadedHash = PerceptualHash.computeHash(page);
                doc.close();
            } else {
                // Image: compute hash directly
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (img == null)
                    return null;
                uploadedHash = PerceptualHash.computeHash(img);
            }

            if (uploadedHash == null)
                return null;

            // Search all stored fingerprints for a match
            List<WatermarkFingerprint> all = fingerprintRepository.findAll();
            WatermarkFingerprint bestMatch = null;
            int bestDistance = Integer.MAX_VALUE;

            for (WatermarkFingerprint fp : all) {
                int dist = PerceptualHash.hammingDistance(uploadedHash, fp.getPageHash());
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestMatch = fp;
                }
            }

            if (bestMatch != null && bestDistance <= PerceptualHash.MATCH_THRESHOLD) {
                log.info("Fingerprint match found! distance={} user={} product={}",
                        bestDistance, bestMatch.getUserEmail(), bestMatch.getProductTitle());

                String jsonPayload = String.format(
                        "{\"userId\":\"%s\",\"email\":\"%s\",\"product\":\"%s\",\"page\":%d,\"matchConfidence\":\"%d%%\"}",
                        bestMatch.getUserId(),
                        bestMatch.getUserEmail(),
                        bestMatch.getProductTitle(),
                        bestMatch.getPageNumber(),
                        (int) ((64 - bestDistance) * 100.0 / 64));

                Map<String, Object> result = new HashMap<>();
                result.put("found", true);
                result.put("watermark", jsonPayload);
                result.put("method", "fingerprint");
                result.put("confidence", (int) ((64 - bestDistance) * 100.0 / 64));
                return result;
            }
        } catch (Exception e) {
            log.warn("Fingerprint matching failed: {}", e.getMessage());
        }
        return null;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "watermark-api", "status", "ok");
    }
}

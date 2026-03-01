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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private static final int SUBREGION_MATCH_THRESHOLD = 16;
    private static final int SUBREGION_AMBIGUITY_GAP = 3;

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
            List<HashCandidate> uploadedCandidates = buildHashCandidates(fileBytes, fileName);
            if (uploadedCandidates.isEmpty())
                return null;

            // Search all stored fingerprints for a match
            List<WatermarkFingerprint> all = fingerprintRepository.findAll();
            if (all.isEmpty())
                return null;

            WatermarkFingerprint bestMatch = null;
            int bestDistance = Integer.MAX_VALUE;
            int secondBestDistance = Integer.MAX_VALUE;
            HashCandidate bestCandidate = null;

            for (WatermarkFingerprint fp : all) {
                for (HashCandidate candidate : uploadedCandidates) {
                    int dist = PerceptualHash.hammingDistance(candidate.hash(), fp.getPageHash());
                    if (dist < bestDistance) {
                        secondBestDistance = bestDistance;
                        bestDistance = dist;
                        bestMatch = fp;
                        bestCandidate = candidate;
                    } else if (dist < secondBestDistance) {
                        secondBestDistance = dist;
                    }
                }
            }

            boolean strictMatch = bestDistance <= PerceptualHash.MATCH_THRESHOLD;
            boolean subregionMatch = bestDistance <= SUBREGION_MATCH_THRESHOLD
                    && (bestDistance + SUBREGION_AMBIGUITY_GAP <= secondBestDistance);

            if (bestMatch != null && (strictMatch || subregionMatch)) {
                String method = (bestCandidate != null && bestCandidate.fullFrame())
                        ? "fingerprint"
                        : "fingerprint-subregion";

                log.info("Fingerprint match found! distance={} secondBest={} method={} user={} product={} source={}",
                        bestDistance,
                        secondBestDistance,
                        method,
                        bestMatch.getUserEmail(),
                        bestMatch.getProductTitle(),
                        bestCandidate != null ? bestCandidate.label() : "n/a");

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
                result.put("method", method);
                result.put("confidence", (int) ((64 - bestDistance) * 100.0 / 64));
                result.put("distance", bestDistance);
                result.put("source", bestCandidate != null ? bestCandidate.label() : "full");
                return result;
            }
        } catch (Exception e) {
            log.warn("Fingerprint matching failed: {}", e.getMessage());
        }
        return null;
    }

    private List<HashCandidate> buildHashCandidates(byte[] fileBytes, String fileName) {
        List<HashCandidate> candidates = new ArrayList<>();

        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(fileBytes)) {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                    int pages = Math.min(doc.getNumberOfPages(), 3);
                    for (int i = 0; i < pages; i++) {
                        BufferedImage page = renderer.renderImageWithDPI(i, 150f);
                        candidates.addAll(buildImageCandidates(page, "pdf-page-" + i, true));
                    }
                }
            } else {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (img == null)
                    return candidates;
                candidates.addAll(buildImageCandidates(img, "upload", true));
            }
        } catch (Exception e) {
            log.warn("Failed to build hash candidates: {}", e.getMessage());
        }

        return candidates;
    }

    private List<HashCandidate> buildImageCandidates(BufferedImage source, String labelPrefix,
            boolean includeSubregions) {
        List<HashCandidate> candidates = new ArrayList<>();
        if (source == null || source.getWidth() < 32 || source.getHeight() < 32)
            return candidates;

        String fullHash = PerceptualHash.computeHash(source);
        if (fullHash != null) {
            candidates.add(new HashCandidate(fullHash, labelPrefix + ":full", true));
        }

        if (!includeSubregions)
            return candidates;

        int width = source.getWidth();
        int height = source.getHeight();

        double[] areaScales = { 0.90, 0.75, 0.60, 0.45 };
        double[] aspectRatios = { 0.70, 0.90, 1.00, 1.33, 1.78 };
        double[] anchors = { 0.0, 0.5, 1.0 };

        for (double scale : areaScales) {
            double targetArea = width * (double) height * scale;

            for (double ratio : aspectRatios) {
                int candidateW = (int) Math.round(Math.sqrt(targetArea * ratio));
                int candidateH = (int) Math.round(candidateW / ratio);

                if (candidateW < 40 || candidateH < 40 || candidateW > width || candidateH > height) {
                    continue;
                }

                for (double ax : anchors) {
                    for (double ay : anchors) {
                        int x = (int) Math.round((width - candidateW) * ax);
                        int y = (int) Math.round((height - candidateH) * ay);

                        BufferedImage sub = source.getSubimage(x, y, candidateW, candidateH);
                        String hash = PerceptualHash.computeHash(sub);
                        if (hash == null)
                            continue;

                        String cropLabel = String.format("%s:crop[s=%.2f,r=%.2f,ax=%.1f,ay=%.1f]",
                                labelPrefix, scale, ratio, ax, ay);
                        candidates.add(new HashCandidate(hash, cropLabel, false));
                    }
                }
            }
        }

        return candidates;
    }

    private record HashCandidate(String hash, String label, boolean fullFrame) {
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "watermark-api", "status", "ok");
    }
}

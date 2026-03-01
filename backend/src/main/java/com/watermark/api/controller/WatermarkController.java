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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private static final int CANDIDATE_MATCH_THRESHOLD = 16;
    private static final int CANDIDATE_AMBIGUITY_GAP = 2;
    private static final int GROUP_SCORE_GAP = 14;
    private static final int GROUP_MIN_HITS = 2;
    private static final int HARD_DISTANCE_ACCEPT = 5;
    private static final int MAX_CANDIDATES = 420;

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

            Map<MatchKey, MatchScore> scoreByGroup = new HashMap<>();

            for (HashCandidate candidate : uploadedCandidates) {
                WatermarkFingerprint bestFp = null;
                int bestDistance = Integer.MAX_VALUE;
                int secondBestDistance = Integer.MAX_VALUE;

                for (WatermarkFingerprint fp : all) {
                    int dist = PerceptualHash.hammingDistance(candidate.hash(), fp.getPageHash());
                    if (dist < bestDistance) {
                        secondBestDistance = bestDistance;
                        bestDistance = dist;
                        bestFp = fp;
                    } else if (dist < secondBestDistance) {
                        secondBestDistance = dist;
                    }
                }

                boolean strictMatch = bestDistance <= PerceptualHash.MATCH_THRESHOLD;
                boolean candidateMatch = bestDistance <= CANDIDATE_MATCH_THRESHOLD
                        && (bestDistance + CANDIDATE_AMBIGUITY_GAP <= secondBestDistance);

                if (bestFp != null && (strictMatch || candidateMatch)) {
                    MatchKey key = MatchKey.from(bestFp);
                    MatchScore score = scoreByGroup.computeIfAbsent(key, ignored -> new MatchScore());
                    score.addHit(bestDistance, secondBestDistance, candidate);
                }
            }

            if (scoreByGroup.isEmpty()) {
                return null;
            }

            List<Map.Entry<MatchKey, MatchScore>> ranked = scoreByGroup.entrySet()
                    .stream()
                    .sorted(Comparator
                            .comparingInt((Map.Entry<MatchKey, MatchScore> e) -> e.getValue().score).reversed()
                            .thenComparingInt(e -> e.getValue().hits).reversed()
                            .thenComparingInt(e -> e.getValue().bestDistance))
                    .toList();

            Map.Entry<MatchKey, MatchScore> top = ranked.get(0);
            MatchScore topScore = top.getValue();
            int secondScore = ranked.size() > 1 ? ranked.get(1).getValue().score : 0;

            boolean passByHits = topScore.hits >= GROUP_MIN_HITS;
            boolean passByDistance = topScore.bestDistance <= HARD_DISTANCE_ACCEPT;
            boolean passByGap = topScore.score >= secondScore + GROUP_SCORE_GAP || passByDistance;

            if (!(passByGap && (passByHits || passByDistance))) {
                log.info("Fingerprint rejected: topScore={} secondScore={} hits={} bestDistance={}",
                        topScore.score, secondScore, topScore.hits, topScore.bestDistance);
                return null;
            }

            MatchKey bestMatch = top.getKey();
            String method = topScore.fullFrameHits > 0 && topScore.fullFrameHits >= (topScore.hits / 2)
                    ? "fingerprint"
                    : "fingerprint-subregion";

            int confidence = computeConfidence(topScore);

            log.info(
                    "Fingerprint match found! method={} user={} product={} page={} score={} secondScore={} hits={} fullFrameHits={} bestDistance={} source={}",
                    method,
                    bestMatch.userEmail(),
                    bestMatch.productTitle(),
                    bestMatch.pageNumber(),
                    topScore.score,
                    secondScore,
                    topScore.hits,
                    topScore.fullFrameHits,
                    topScore.bestDistance,
                    topScore.bestSource);

            String jsonPayload = String.format(
                    "{\"userId\":\"%s\",\"email\":\"%s\",\"product\":\"%s\",\"page\":%d,\"matchConfidence\":\"%d%%\"}",
                    bestMatch.userId(),
                    bestMatch.userEmail(),
                    bestMatch.productTitle(),
                    bestMatch.pageNumber(),
                    confidence);

            Map<String, Object> result = new HashMap<>();
            result.put("found", true);
            result.put("watermark", jsonPayload);
            result.put("method", method);
            result.put("confidence", confidence);
            result.put("distance", topScore.bestDistance);
            result.put("source", topScore.bestSource);
            result.put("hits", topScore.hits);
            return result;
        } catch (Exception e) {
            log.warn("Fingerprint matching failed: {}", e.getMessage());
        }
        return null;
    }

    private int computeConfidence(MatchScore score) {
        double distanceConfidence = (64 - score.bestDistance) * 100.0 / 64.0;
        double hitBonus = Math.min(22.0, score.hits * 3.5);
        int combined = (int) Math.round(distanceConfidence * 0.78 + hitBonus);
        return Math.max(55, Math.min(99, combined));
    }

    private List<HashCandidate> buildHashCandidates(byte[] fileBytes, String fileName) {
        Map<String, HashCandidate> uniqueCandidates = new LinkedHashMap<>();

        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(fileBytes)) {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                    int pages = Math.min(doc.getNumberOfPages(), 3);
                    for (int i = 0; i < pages; i++) {
                        BufferedImage page = renderer.renderImageWithDPI(i, 150f);
                        addCandidates(uniqueCandidates, buildImageCandidates(page, "pdf-page-" + i, true));
                    }
                }
            } else {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (img == null)
                    return List.of();
                addCandidates(uniqueCandidates, buildImageCandidates(img, "upload", true));
            }
        } catch (Exception e) {
            log.warn("Failed to build hash candidates: {}", e.getMessage());
        }

        return uniqueCandidates.values().stream().limit(MAX_CANDIDATES).toList();
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

        double[] areaScales = { 0.90, 0.75, 0.60, 0.45, 0.32, 0.22, 0.15 };
        double[] aspectRatios = { 0.56, 0.70, 0.90, 1.00, 1.33, 1.78 };
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

        int minDim = Math.min(width, height);
        int[] tileSizes = {
                Math.max(42, (int) (minDim * 0.16)),
                Math.max(56, (int) (minDim * 0.24)),
                Math.max(72, (int) (minDim * 0.32))
        };

        for (int tileSize : tileSizes) {
            if (tileSize > width || tileSize > height)
                continue;
            int step = Math.max(14, tileSize / 2);
            for (int y = 0; y <= height - tileSize; y += step) {
                for (int x = 0; x <= width - tileSize; x += step) {
                    BufferedImage sub = source.getSubimage(x, y, tileSize, tileSize);
                    String hash = PerceptualHash.computeHash(sub);
                    if (hash == null)
                        continue;
                    String tileLabel = String.format("%s:tile[size=%d,x=%d,y=%d]", labelPrefix, tileSize, x, y);
                    candidates.add(new HashCandidate(hash, tileLabel, false));
                }
            }
        }

        return candidates;
    }

    private void addCandidates(Map<String, HashCandidate> uniqueCandidates, List<HashCandidate> candidates) {
        for (HashCandidate candidate : candidates) {
            uniqueCandidates.putIfAbsent(candidate.hash(), candidate);
        }
    }

    private record HashCandidate(String hash, String label, boolean fullFrame) {
    }

    private record MatchKey(UUID userId, String userEmail, UUID productId, String productTitle, int pageNumber) {
        private static MatchKey from(WatermarkFingerprint fp) {
            return new MatchKey(fp.getUserId(), fp.getUserEmail(), fp.getProductId(), fp.getProductTitle(),
                    fp.getPageNumber());
        }
    }

    private static class MatchScore {
        private int score;
        private int hits;
        private int fullFrameHits;
        private int bestDistance = Integer.MAX_VALUE;
        private String bestSource = "n/a";

        private void addHit(int distance, int secondBestDistance, HashCandidate candidate) {
            this.hits++;
            this.score += (65 - distance);
            if (!candidate.fullFrame()) {
                this.score += 4;
            } else {
                this.fullFrameHits++;
            }

            int separation = Math.max(0, secondBestDistance - distance);
            this.score += Math.min(8, separation);

            if (distance < this.bestDistance) {
                this.bestDistance = distance;
                this.bestSource = candidate.label();
            }
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "watermark-api", "status", "ok");
    }
}

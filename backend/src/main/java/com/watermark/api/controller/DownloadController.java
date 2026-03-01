package com.watermark.api.controller;

import com.watermark.api.entity.Product;
import com.watermark.api.entity.User;
import com.watermark.api.entity.WatermarkFingerprint;
import com.watermark.api.repository.FingerprintRepository;
import com.watermark.api.repository.ProductRepository;
import com.watermark.api.service.OrderService;
import com.watermark.sdk.PerceptualHash;
import com.watermark.sdk.WatermarkEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
public class DownloadController {

        private static final int MAX_STORED_HASHES_PER_PAGE = 180;

        private final OrderService orderService;
        private final ProductRepository productRepository;
        private final WatermarkEngine watermarkEngine;
        private final FingerprintRepository fingerprintRepository;

        @Value("${app.storage.path}")
        private String storagePath;

        @GetMapping("/{productId}")
        public ResponseEntity<?> download(@AuthenticationPrincipal User user,
                        @PathVariable UUID productId) {
                if (!orderService.ownsProduct(user.getId(), productId))
                        return ResponseEntity.status(403).body(Map.of("error", "You have not purchased this product"));

                Product product = productRepository.findById(productId).orElse(null);
                if (product == null)
                        return ResponseEntity.notFound().build();

                try {
                        byte[] fileBytes = Files.readAllBytes(Paths.get(storagePath, product.getFileUrl()));

                        // Embed metadata watermark (invisible, for direct file scanning)
                        byte[] watermarked = watermarkEngine.embedAuto(
                                        fileBytes, product.getFileUrl(),
                                        user.getId().toString(), user.getEmail());

                        // Store perceptual hashes for screenshot matching
                        storeFingerprints(watermarked, product, user);

                        String contentType = product.getProductType() == Product.ProductType.PDF
                                        ? "application/pdf"
                                        : "image/png";
                        String ext = product.getProductType() == Product.ProductType.PDF ? ".pdf" : ".png";
                        String fileName = product.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ext;

                        log.info("Download: user={} product={} type={} (fingerprints stored)",
                                        user.getEmail(), product.getTitle(), product.getProductType());

                        return ResponseEntity.ok()
                                        .contentType(MediaType.parseMediaType(contentType))
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"" + fileName + "\"")
                                        .body(watermarked);
                } catch (IOException e) {
                        log.error("Download failed: {}", e.getMessage());
                        return ResponseEntity.status(500).body(Map.of("error", "Failed to process download"));
                }
        }

        /**
         * Store perceptual hashes for each page of the downloaded file.
         * These are used later to match uploaded screenshots.
         */
        private void storeFingerprints(byte[] fileBytes, Product product, User user) {
                try {
                        fingerprintRepository.deleteByUserIdAndProductId(user.getId(), product.getId());

                        if (product.getProductType() == Product.ProductType.PDF) {
                                // PDF: hash each page
                                try (PDDocument doc = Loader.loadPDF(fileBytes)) {
                                        PDFRenderer renderer = new PDFRenderer(doc);
                                        for (int i = 0; i < doc.getNumberOfPages(); i++) {
                                                BufferedImage pageImg = renderer.renderImageWithDPI(i, 150f);
                                                Set<String> hashes = collectHashes(pageImg);
                                                for (String hash : hashes) {
                                                        fingerprintRepository.save(WatermarkFingerprint.builder()
                                                                        .userId(user.getId())
                                                                        .userEmail(user.getEmail())
                                                                        .productId(product.getId())
                                                                        .productTitle(product.getTitle())
                                                                        .pageNumber(i)
                                                                        .pageHash(hash)
                                                                        .downloadedAt(Instant.now())
                                                                        .build());
                                                }
                                        }
                                }
                        } else {
                                // Image: single hash
                                BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileBytes));
                                if (img != null) {
                                        Set<String> hashes = collectHashes(img);
                                        for (String hash : hashes) {
                                                fingerprintRepository.save(WatermarkFingerprint.builder()
                                                                .userId(user.getId())
                                                                .userEmail(user.getEmail())
                                                                .productId(product.getId())
                                                                .productTitle(product.getTitle())
                                                                .pageNumber(0)
                                                                .pageHash(hash)
                                                                .downloadedAt(Instant.now())
                                                                .build());
                                        }
                                }
                        }
                        log.info("Stored fingerprints for user={} product={}", user.getEmail(), product.getTitle());
                } catch (Exception e) {
                        log.warn("Failed to store fingerprints: {}", e.getMessage());
                        // Non-fatal: download still proceeds
                }
        }

        private Set<String> collectHashes(BufferedImage source) {
                Set<String> hashes = new LinkedHashSet<>();
                if (source == null || source.getWidth() < 32 || source.getHeight() < 32) {
                        return hashes;
                }

                addHash(hashes, source);

                int width = source.getWidth();
                int height = source.getHeight();

                double[] areaScales = { 0.85, 0.65, 0.50, 0.35, 0.25 };
                double[] aspectRatios = { 0.56, 0.75, 1.00, 1.33, 1.78 };
                double[] anchors = { 0.0, 0.5, 1.0 };

                for (double scale : areaScales) {
                        if (hashes.size() >= MAX_STORED_HASHES_PER_PAGE)
                                break;

                        double targetArea = width * (double) height * scale;
                        for (double ratio : aspectRatios) {
                                int candidateW = (int) Math.round(Math.sqrt(targetArea * ratio));
                                int candidateH = (int) Math.round(candidateW / ratio);

                                if (candidateW < 40 || candidateH < 40 || candidateW > width || candidateH > height) {
                                        continue;
                                }

                                for (double ax : anchors) {
                                        for (double ay : anchors) {
                                                if (hashes.size() >= MAX_STORED_HASHES_PER_PAGE)
                                                        break;
                                                int x = (int) Math.round((width - candidateW) * ax);
                                                int y = (int) Math.round((height - candidateH) * ay);
                                                BufferedImage sub = source.getSubimage(x, y, candidateW, candidateH);
                                                addHash(hashes, sub);
                                        }
                                }
                        }
                }

                int minDim = Math.min(width, height);
                int[] tileSizes = {
                                Math.max(48, (int) (minDim * 0.22)),
                                Math.max(56, (int) (minDim * 0.30)),
                                Math.max(64, (int) (minDim * 0.40))
                };

                for (int tileSize : tileSizes) {
                        if (hashes.size() >= MAX_STORED_HASHES_PER_PAGE)
                                break;
                        if (tileSize > width || tileSize > height)
                                continue;

                        int step = Math.max(16, tileSize / 2);
                        for (int y = 0; y <= height - tileSize; y += step) {
                                for (int x = 0; x <= width - tileSize; x += step) {
                                        if (hashes.size() >= MAX_STORED_HASHES_PER_PAGE)
                                                break;
                                        BufferedImage sub = source.getSubimage(x, y, tileSize, tileSize);
                                        addHash(hashes, sub);
                                }
                        }
                }

                return hashes;
        }

        private void addHash(Set<String> hashes, BufferedImage img) {
                String hash = PerceptualHash.computeHash(img);
                if (hash != null) {
                        hashes.add(hash);
                }
        }
}

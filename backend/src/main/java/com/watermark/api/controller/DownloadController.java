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
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
public class DownloadController {

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
                        if (product.getProductType() == Product.ProductType.PDF) {
                                // PDF: hash each page
                                try (PDDocument doc = Loader.loadPDF(fileBytes)) {
                                        PDFRenderer renderer = new PDFRenderer(doc);
                                        for (int i = 0; i < doc.getNumberOfPages(); i++) {
                                                BufferedImage pageImg = renderer.renderImageWithDPI(i, 150f);
                                                String hash = PerceptualHash.computeHash(pageImg);
                                                if (hash != null) {
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
                                        String hash = PerceptualHash.computeHash(img);
                                        if (hash != null) {
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
}

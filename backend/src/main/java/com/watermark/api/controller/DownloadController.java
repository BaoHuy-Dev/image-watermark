package com.watermark.api.controller;

import com.watermark.api.entity.Product;
import com.watermark.api.entity.User;
import com.watermark.api.repository.ProductRepository;
import com.watermark.api.service.OrderService;
import com.watermark.sdk.WatermarkEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
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
                        byte[] watermarked = watermarkEngine.embedAuto(
                                        fileBytes, product.getFileUrl(),
                                        user.getId().toString(), user.getEmail());

                        String contentType = product.getProductType() == Product.ProductType.PDF
                                        ? "application/pdf"
                                        : "image/png";
                        String ext = product.getProductType() == Product.ProductType.PDF ? ".pdf" : ".png";
                        String fileName = product.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ext;

                        log.info("Download: user={} product={} type={}", user.getEmail(), product.getTitle(),
                                        product.getProductType());

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
}

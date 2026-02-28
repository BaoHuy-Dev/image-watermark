package com.watermark.sdk;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * High-level API for the invisible watermark tool.
 *
 * <h3>Quick start – embed user info:</h3>
 * 
 * <pre>{@code
 * WatermarkEngine engine = new WatermarkEngine();
 *
 * // Embed
 * byte[] watermarked = engine.embedUserInfo(imageBytes, "user-123", "user@email.com");
 *
 * // Extract
 * WatermarkResult result = engine.extract(watermarked);
 * System.out.println(result.getText());
 * // →
 * // {"userId":"user-123","email":"user@email.com","ts":"2026-02-28T12:00:00Z"}
 * }</pre>
 *
 * <h3>Integration into Spring Boot:</h3>
 * 
 * <pre>
 * {@code
 * // 1. Add dependency in pom.xml:
 * //    <dependency>
 * //        <groupId>com.watermark</groupId>
 * //        <artifactId>watermark-sdk</artifactId>
 * //        <version>1.0.0</version>
 * //    </dependency>
 *
 * // 2. Create a @Bean:
 * &#64;Bean
 * public WatermarkEngine watermarkEngine() {
 *     return new WatermarkEngine();
 * }
 *
 * // 3. Inject and use:
 * @Autowired WatermarkEngine engine;
 * }
 * </pre>
 */
public class WatermarkEngine {

    /**
     * Embed a plain-text watermark into an image.
     *
     * @param imageBytes PNG/BMP image bytes
     * @param text       watermark text
     * @return watermarked PNG bytes
     */
    public byte[] embed(byte[] imageBytes, String text) throws IOException {
        return LsbSteganography.embed(imageBytes, text);
    }

    /**
     * Embed a JSON payload with user identification into an image.
     *
     * @param imageBytes PNG/BMP image bytes
     * @param userId     user identifier
     * @param email      user email
     * @return watermarked PNG bytes
     */
    public byte[] embedUserInfo(byte[] imageBytes, String userId, String email) throws IOException {
        String json = String.format(
                "{\"userId\":\"%s\",\"email\":\"%s\",\"ts\":\"%s\"}",
                escape(userId), escape(email), Instant.now().toString());
        return LsbSteganography.embed(imageBytes, json);
    }

    /**
     * Embed arbitrary key-value metadata as JSON into an image.
     *
     * @param imageBytes PNG/BMP image bytes
     * @param metadata   key-value pairs
     * @return watermarked PNG bytes
     */
    public byte[] embedMetadata(byte[] imageBytes, Map<String, String> metadata) throws IOException {
        String json = metadata.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":\"" + escape(e.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
        return LsbSteganography.embed(imageBytes, json);
    }

    /**
     * Extract a hidden watermark from an image.
     *
     * @param imageBytes image bytes to inspect
     * @return extraction result (check {@link WatermarkResult#isFound()})
     */
    public WatermarkResult extract(byte[] imageBytes) throws IOException {
        String text = LsbSteganography.extract(imageBytes);
        return text != null ? WatermarkResult.found(text) : WatermarkResult.notFound();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

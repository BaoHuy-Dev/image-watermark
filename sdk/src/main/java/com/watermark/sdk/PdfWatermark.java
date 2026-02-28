package com.watermark.sdk;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Screenshot-proof invisible PDF watermark using:
 *
 * Layer 1: PDF Document Metadata — for direct PDF file scanning
 * Layer 2: QIM Robust Watermark on every page — survives screenshots
 *
 * Each page is rasterized, watermarked with the robust QIM algorithm
 * (block-average quantization), then repackaged into the PDF.
 *
 * If someone screenshots ANY page and saves it in ANY format (PNG, JPG...),
 * the watermark can be extracted from that screenshot.
 */
public final class PdfWatermark {

    private static final String META_KEY = "X-Aura-Sig";
    private static final String META_PAGES_KEY = "X-Aura-PgSig";
    private static final String SIGNATURE_PREFIX = "AURA::";
    private static final float RENDER_DPI = 150f;

    private PdfWatermark() {
    }

    /**
     * Embed a screenshot-proof invisible watermark into every page of a PDF.
     */
    public static byte[] embed(byte[] pdfBytes, String userId, String email) throws IOException {
        String timestamp = Instant.now().toString();
        String jsonPayload = String.format(
                "{\"userId\":\"%s\",\"email\":\"%s\",\"ts\":\"%s\"}",
                escape(userId), escape(email), timestamp);

        String encoded = Base64.getEncoder().encodeToString(
                jsonPayload.getBytes(StandardCharsets.UTF_8));
        String signature = SIGNATURE_PREFIX + encoded;

        try (PDDocument srcDoc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(srcDoc);
            int pageCount = srcDoc.getNumberOfPages();

            try (PDDocument destDoc = new PDDocument()) {

                for (int i = 0; i < pageCount; i++) {
                    PDRectangle originalSize = srcDoc.getPage(i).getMediaBox();

                    // Render page to image
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, RENDER_DPI);

                    // === ROBUST WATERMARK: embed using QIM block algorithm ===
                    // This survives screenshots, scaling, and JPEG compression
                    BufferedImage watermarkedImage = RobustWatermark.embedIntoImage(pageImage, jsonPayload);

                    // Create new page with watermarked image
                    PDPage newPage = new PDPage(originalSize);
                    destDoc.addPage(newPage);

                    PDImageXObject pdImage = LosslessFactory.createFromImage(destDoc, watermarkedImage);
                    try (PDPageContentStream cs = new PDPageContentStream(destDoc, newPage)) {
                        cs.drawImage(pdImage, 0, 0,
                                originalSize.getWidth(), originalSize.getHeight());
                    }
                }

                // === Layer 1: Document metadata (for direct PDF scanning) ===
                PDDocumentInformation info = destDoc.getDocumentInformation();
                info.setCustomMetadataValue(META_KEY, signature);
                info.setCustomMetadataValue(META_PAGES_KEY, String.valueOf(pageCount));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                destDoc.save(out);
                return out.toByteArray();
            }
        }
    }

    /**
     * Extract the invisible watermark from a PDF.
     *
     * Strategy:
     * 1. Try metadata (fastest)
     * 2. Try robust QIM extraction from rendered pages (works for modified PDFs)
     */
    public static String extract(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {

            // === Try Layer 1: Document metadata ===
            PDDocumentInformation info = doc.getDocumentInformation();
            String metaValue = info.getCustomMetadataValue(META_KEY);

            if (metaValue != null && metaValue.startsWith(SIGNATURE_PREFIX)) {
                String enc = metaValue.substring(SIGNATURE_PREFIX.length());
                try {
                    byte[] decoded = Base64.getDecoder().decode(enc);
                    return new String(decoded, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // Corrupted, fall through
                }
            }

            // === Try Layer 2: Robust QIM extraction from rendered pages ===
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < Math.min(doc.getNumberOfPages(), 3); i++) {
                try {
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, RENDER_DPI);
                    String extracted = RobustWatermark.extractFromImage(pageImage);
                    if (extracted != null && extracted.contains("userId")) {
                        return extracted;
                    }
                } catch (Exception e) {
                    // Try next page
                }
            }

            return null;
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

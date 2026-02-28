package com.watermark.sdk;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Invisible PDF watermark using dual-layer steganography:
 * 
 * Layer 1: PDF Document Metadata (custom properties) - primary, reliable
 * Layer 2: Fully transparent text on every page (0% opacity) - backup forensic
 * layer
 * 
 * Both layers are completely INVISIBLE to the human eye.
 * Only the admin extraction tool can read them.
 */
public final class PdfWatermark {

    private static final String META_KEY = "X-Aura-Sig";
    private static final String META_PAGES_KEY = "X-Aura-PgSig";
    private static final String SIGNATURE_PREFIX = "AURA::";

    private PdfWatermark() {
    }

    /**
     * Embed an invisible watermark into every page of a PDF.
     * Uses two steganographic layers:
     * 1. Custom PDF metadata property (invisible, reliable extraction)
     * 2. Fully transparent text on each page (invisible, forensic backup)
     */
    public static byte[] embed(byte[] pdfBytes, String userId, String email) throws IOException {
        String timestamp = Instant.now().toString();
        String payload = String.format(
                "{\"userId\":\"%s\",\"email\":\"%s\",\"ts\":\"%s\"}",
                escape(userId), escape(email), timestamp);

        // Encode payload as Base64 for safe storage in metadata
        String encoded = Base64.getEncoder().encodeToString(
                payload.getBytes(StandardCharsets.UTF_8));

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {

            // === Layer 1: Document metadata (primary) ===
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setCustomMetadataValue(META_KEY, SIGNATURE_PREFIX + encoded);
            info.setCustomMetadataValue(META_PAGES_KEY,
                    String.valueOf(doc.getNumberOfPages()));

            // === Layer 2: Invisible text on every page (forensic backup) ===
            // Alpha 0.0 = completely transparent = invisible to human eye
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.0f); // FULLY transparent
            gs.setStrokingAlphaConstant(0.0f); // FULLY transparent

            String invisibleText = SIGNATURE_PREFIX + encoded;

            for (PDPage page : doc.getPages()) {
                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.setGraphicsStateParameters(gs);
                    cs.setNonStrokingColor(1.0f, 1.0f, 1.0f); // White (invisible on any bg)

                    cs.beginText();
                    cs.setFont(font, 1f); // Tiny font size
                    cs.newLineAtOffset(0, 0); // Bottom-left corner
                    cs.showText(invisibleText);
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Extract the invisible watermark from a PDF.
     * Tries Layer 1 (metadata) first, falls back to Layer 2 (invisible text).
     */
    public static String extract(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {

            // === Try Layer 1: Document metadata ===
            PDDocumentInformation info = doc.getDocumentInformation();
            String metaValue = info.getCustomMetadataValue(META_KEY);

            if (metaValue != null && metaValue.startsWith(SIGNATURE_PREFIX)) {
                String encoded = metaValue.substring(SIGNATURE_PREFIX.length());
                try {
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    return new String(decoded, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // Corrupted metadata, try Layer 2
                }
            }

            // === Try Layer 2: Invisible text extraction ===
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String allText = stripper.getText(doc);

            if (allText != null) {
                int idx = allText.indexOf(SIGNATURE_PREFIX);
                if (idx >= 0) {
                    // Extract the Base64 payload after the prefix
                    String remaining = allText.substring(idx + SIGNATURE_PREFIX.length()).trim();
                    // Take until whitespace or end of string
                    String encoded = remaining.split("\\s+")[0].trim();
                    try {
                        byte[] decoded = Base64.getDecoder().decode(encoded);
                        return new String(decoded, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        // Corrupted text layer
                    }
                }
            }

            return null;
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

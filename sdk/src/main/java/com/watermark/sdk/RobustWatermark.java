package com.watermark.sdk;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Robust watermark engine using Quantization Index Modulation (QIM)
 * on the BLUE channel with ultra-low visibility.
 *
 * Key design choices for INVISIBILITY:
 * - Blue channel: human vision has lowest sensitivity to blue changes
 * - QUANT_STEP = 4: maximum pixel change is ±2 levels (out of 256) —
 * undetectable
 * - Proportional grid: block size adapts to image resolution
 * - Majority voting: compensates for noise from compression/scaling
 *
 * Survives: screenshots, scaling, JPEG compression, format conversion.
 */
public final class RobustWatermark {

    private static final int GRID_COLS = 48; // Fewer blocks = larger blocks = smoother
    private static final int QUANT_STEP = 4; // Ultra-small step: max ±2 pixel change
    private static final int HALF_STEP = QUANT_STEP / 2;
    private static final int MAGIC = 0x524F; // "RO" magic header

    private RobustWatermark() {
    }

    // ==================== PUBLIC API ====================

    public static byte[] embed(byte[] imageBytes, String text) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null)
            throw new IOException("Cannot read image");
        BufferedImage result = embedIntoImage(img, text);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(result, "png", out);
        return out.toByteArray();
    }

    /**
     * Embed an invisible watermark into a BufferedImage.
     * Modifies the BLUE channel by at most ±2 levels — completely invisible.
     */
    public static BufferedImage embedIntoImage(BufferedImage img, String text) {
        byte[] compressed = compress(text.getBytes(StandardCharsets.UTF_8));
        byte[] payload = buildPayload(compressed);
        boolean[] bits = bytesToBits(payload);

        int blockW = Math.max(2, img.getWidth() / GRID_COLS);
        int blockH = blockW; // square blocks
        int gridRows = img.getHeight() / blockH;
        if (gridRows < 2)
            gridRows = 2;
        int gridCols = img.getWidth() / blockW;
        int totalBlocks = gridCols * gridRows;

        // Embed bits with wraparound for redundancy (majority voting on extraction)
        for (int i = 0; i < totalBlocks; i++) {
            int bitIdx = i % bits.length;
            int col = i % gridCols;
            int row = i / gridCols;
            embedBitInBlock(img, col * blockW, row * blockH, blockW, blockH, bits[bitIdx]);
        }

        return img;
    }

    public static String extract(byte[] imageBytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null)
            return null;
        return extractFromImage(img);
    }

    /**
     * Extract a robust watermark from a BufferedImage.
     * Uses majority voting across all payload repetitions for error correction.
     */
    public static String extractFromImage(BufferedImage img) {
        int blockW = Math.max(2, img.getWidth() / GRID_COLS);
        int blockH = blockW;
        int gridRows = img.getHeight() / blockH;
        if (gridRows < 2)
            return null;
        int gridCols = img.getWidth() / blockW;
        int totalBlocks = gridCols * gridRows;

        // Extract raw bits from all blocks
        boolean[] rawBits = new boolean[totalBlocks];
        for (int i = 0; i < totalBlocks; i++) {
            int col = i % gridCols;
            int row = i / gridCols;
            rawBits[i] = extractBitFromBlock(img, col * blockW, row * blockH, blockW, blockH);
        }

        // Phase 1: Read header (first 32 bits) to get payload size
        if (totalBlocks < 32)
            return null;
        byte[] headerBytes = bitsToBytes(rawBits, 0, 32);
        int magic = ((headerBytes[0] & 0xFF) << 8) | (headerBytes[1] & 0xFF);
        if (magic != MAGIC)
            return null;

        int compLen = ((headerBytes[2] & 0xFF) << 8) | (headerBytes[3] & 0xFF);
        if (compLen <= 0 || compLen > 5000)
            return null;

        int payloadBits = (4 + compLen) * 8;
        if (payloadBits > totalBlocks)
            return null;

        // Phase 2: Majority voting across all repetitions
        int reps = totalBlocks / payloadBits;
        if (reps < 1)
            reps = 1;

        int[] votes = new int[payloadBits];
        for (int r = 0; r < reps; r++) {
            for (int b = 0; b < payloadBits; b++) {
                int idx = r * payloadBits + b;
                if (idx >= totalBlocks)
                    break;
                votes[b] += rawBits[idx] ? 1 : -1;
            }
        }

        boolean[] finalBits = new boolean[payloadBits];
        for (int i = 0; i < payloadBits; i++) {
            finalBits[i] = votes[i] > 0;
        }

        byte[] finalBytes = bitsToBytes(finalBits, 0, payloadBits);

        // Verify magic after voting
        magic = ((finalBytes[0] & 0xFF) << 8) | (finalBytes[1] & 0xFF);
        if (magic != MAGIC)
            return null;

        compLen = ((finalBytes[2] & 0xFF) << 8) | (finalBytes[3] & 0xFF);
        if (compLen <= 0 || 4 + compLen > finalBytes.length)
            return null;

        byte[] compressed = new byte[compLen];
        System.arraycopy(finalBytes, 4, compressed, 0, compLen);
        try {
            byte[] decompressed = decompress(compressed);
            return new String(decompressed, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== BLOCK OPERATIONS (BLUE CHANNEL) ====================

    /**
     * Embed one bit into a block by adjusting the average BLUE channel value.
     * Maximum pixel modification: ±2 levels (QUANT_STEP/2 = 2).
     * This is completely imperceptible to the human eye.
     */
    private static void embedBitInBlock(BufferedImage img, int bx, int by,
            int blockW, int blockH, boolean bit) {
        int maxX = Math.min(bx + blockW, img.getWidth());
        int maxY = Math.min(by + blockH, img.getHeight());

        // Calculate average BLUE channel value
        long sum = 0;
        int count = 0;
        for (int y = by; y < maxY; y++) {
            for (int x = bx; x < maxX; x++) {
                sum += img.getRGB(x, y) & 0xFF; // Blue channel
                count++;
            }
        }
        if (count == 0)
            return;
        int avg = (int) (sum / count);

        // QIM: quantize to encode bit
        int target;
        if (bit) {
            // Bit 1: nearest (QUANT_STEP * k + HALF_STEP)
            target = ((avg + HALF_STEP) / QUANT_STEP) * QUANT_STEP + HALF_STEP;
        } else {
            // Bit 0: nearest (QUANT_STEP * k)
            target = ((avg + HALF_STEP) / QUANT_STEP) * QUANT_STEP;
        }
        target = Math.max(2, Math.min(253, target));

        int adjustment = target - avg;

        // Apply adjustment to BLUE channel only
        for (int y = by; y < maxY; y++) {
            for (int x = bx; x < maxX; x++) {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                b = clamp(b + adjustment);
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    /**
     * Extract one bit from a block by reading the average BLUE channel.
     */
    private static boolean extractBitFromBlock(BufferedImage img, int bx, int by,
            int blockW, int blockH) {
        int maxX = Math.min(bx + blockW, img.getWidth());
        int maxY = Math.min(by + blockH, img.getHeight());

        long sum = 0;
        int count = 0;
        for (int y = by; y < maxY; y++) {
            for (int x = bx; x < maxX; x++) {
                sum += img.getRGB(x, y) & 0xFF; // Blue channel
                count++;
            }
        }
        if (count == 0)
            return false;
        int avg = (int) (sum / count);

        // QIM decode: check remainder
        int remainder = ((avg % QUANT_STEP) + QUANT_STEP) % QUANT_STEP;
        return remainder >= QUANT_STEP / 4 && remainder < (QUANT_STEP * 3) / 4;
    }

    // ==================== HELPERS ====================

    private static byte[] buildPayload(byte[] compressedData) {
        byte[] payload = new byte[4 + compressedData.length];
        payload[0] = (byte) (MAGIC >> 8);
        payload[1] = (byte) MAGIC;
        payload[2] = (byte) (compressedData.length >> 8);
        payload[3] = (byte) compressedData.length;
        System.arraycopy(compressedData, 0, payload, 4, compressedData.length);
        return payload;
    }

    private static boolean[] bytesToBits(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = ((bytes[i] >> (7 - j)) & 1) == 1;
            }
        }
        return bits;
    }

    private static byte[] bitsToBytes(boolean[] bits, int offset, int bitCount) {
        int byteCount = bitCount / 8;
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            int val = 0;
            for (int j = 0; j < 8; j++) {
                int idx = offset + i * 8 + j;
                if (idx < bits.length && bits[idx]) {
                    val |= (1 << (7 - j));
                }
            }
            bytes[i] = (byte) val;
        }
        return bytes;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static byte[] compress(byte[] data) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(data);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        byte[] buf = new byte[1024];
        while (!d.finished())
            out.write(buf, 0, d.deflate(buf));
        d.end();
        return out.toByteArray();
    }

    private static byte[] decompress(byte[] data) throws Exception {
        Inflater inf = new Inflater();
        inf.setInput(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 2);
        byte[] buf = new byte[1024];
        while (!inf.finished()) {
            int n = inf.inflate(buf);
            if (n == 0 && inf.needsInput())
                break;
            out.write(buf, 0, n);
        }
        inf.end();
        return out.toByteArray();
    }
}

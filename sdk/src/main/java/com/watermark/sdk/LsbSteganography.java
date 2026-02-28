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
 * Core LSB (Least Significant Bit) steganography engine.
 *
 * <p>
 * Embeds data invisibly into the 2 least-significant bits of
 * the Blue channel of each pixel. The modification is imperceptible
 * to the human eye.
 * </p>
 *
 * <h3>Payload structure:</h3>
 * 
 * <pre>
 * [MAGIC 4B] [LENGTH 4B] [DEFLATE-COMPRESSED DATA]
 * </pre>
 *
 * <p>
 * <b>Important:</b> Only PNG/BMP (lossless) formats are supported.
 * JPEG will destroy the watermark due to lossy compression.
 * </p>
 */
public final class LsbSteganography {

    static final int MAGIC_HEADER = 0x574D524B; // "WMRK"
    private static final int BITS_PER_PIXEL = 2;

    private LsbSteganography() {
    }

    // ==================== PUBLIC API ====================

    /**
     * Embed a text watermark into an image.
     *
     * @param imageBytes source image bytes (PNG/BMP)
     * @param watermark  text to embed
     * @return watermarked image bytes (PNG)
     */
    public static byte[] embed(byte[] imageBytes, String watermark) throws IOException {
        BufferedImage image = readImage(imageBytes);
        BufferedImage argb = toARGB(image);

        byte[] compressed = compress(watermark.getBytes(StandardCharsets.UTF_8));
        byte[] payload = buildPayload(compressed);

        validateCapacity(argb, payload.length);
        embedBits(argb, payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(argb, "png", out);
        return out.toByteArray();
    }

    /**
     * Extract a hidden watermark from an image.
     *
     * @param imageBytes watermarked image bytes
     * @return extracted text, or {@code null} if no watermark found
     */
    public static String extract(byte[] imageBytes) throws IOException {
        BufferedImage image = readImage(imageBytes);
        BufferedImage argb = toARGB(image);

        int magic = extractInt(argb, 0);
        if (magic != MAGIC_HEADER)
            return null;

        int dataLength = extractInt(argb, 32);
        if (dataLength <= 0 || dataLength > 10_000_000)
            return null;

        byte[] compressed = extractBytes(argb, 64, dataLength);
        byte[] decompressed = decompress(compressed);
        return new String(decompressed, StandardCharsets.UTF_8);
    }

    // ==================== EMBEDDING ====================

    private static void embedBits(BufferedImage image, byte[] payload) {
        int width = image.getWidth();
        int px = 0;
        for (byte b : payload) {
            for (int pair = 3; pair >= 0; pair--) {
                int x = px % width, y = px / width;
                int rgb = image.getRGB(x, y);
                int bits = (b >> (pair * 2)) & 0x03;
                image.setRGB(x, y, (rgb & 0xFFFFFFFC) | bits);
                px++;
            }
        }
    }

    // ==================== EXTRACTION ====================

    private static int extractInt(BufferedImage image, int bitOffset) {
        int width = image.getWidth();
        int value = 0;
        int start = bitOffset / BITS_PER_PIXEL;
        for (int i = 0; i < 16; i++) {
            int x = (start + i) % width, y = (start + i) / width;
            value = (value << 2) | (image.getRGB(x, y) & 0x03);
        }
        return value;
    }

    private static byte[] extractBytes(BufferedImage image, int bitOffset, int count) {
        int width = image.getWidth();
        byte[] result = new byte[count];
        int px = bitOffset / BITS_PER_PIXEL;
        for (int i = 0; i < count; i++) {
            int val = 0;
            for (int p = 0; p < 4; p++) {
                int x = px % width, y = px / width;
                val = (val << 2) | (image.getRGB(x, y) & 0x03);
                px++;
            }
            result[i] = (byte) val;
        }
        return result;
    }

    // ==================== HELPERS ====================

    private static BufferedImage readImage(byte[] bytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img == null)
            throw new IOException("Cannot read image. Ensure PNG or BMP format.");
        return img;
    }

    private static BufferedImage toARGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB)
            return src;
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // Copy pixel-by-pixel to preserve exact RGB values (including LSBs).
        // DO NOT use Graphics.drawImage() – it applies alpha compositing
        // which can alter the least-significant bits and destroy watermark data.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                argb.setRGB(x, y, src.getRGB(x, y));
            }
        }
        return argb;
    }

    private static byte[] buildPayload(byte[] data) {
        byte[] payload = new byte[8 + data.length];
        writeInt(payload, 0, MAGIC_HEADER);
        writeInt(payload, 4, data.length);
        System.arraycopy(data, 0, payload, 8, data.length);
        return payload;
    }

    private static void writeInt(byte[] arr, int offset, int value) {
        arr[offset] = (byte) (value >> 24);
        arr[offset + 1] = (byte) (value >> 16);
        arr[offset + 2] = (byte) (value >> 8);
        arr[offset + 3] = (byte) value;
    }

    private static void validateCapacity(BufferedImage img, int payloadBytes) {
        int needed = payloadBytes * 8;
        int available = img.getWidth() * img.getHeight() * BITS_PER_PIXEL;
        if (needed > available) {
            throw new IllegalArgumentException(String.format(
                    "Image too small: need %d bits but only %d available (%dx%d px).",
                    needed, available, img.getWidth(), img.getHeight()));
        }
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

    private static byte[] decompress(byte[] data) throws IOException {
        try {
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
        } catch (Exception e) {
            throw new IOException("Failed to decompress watermark: " + e.getMessage(), e);
        }
    }
}

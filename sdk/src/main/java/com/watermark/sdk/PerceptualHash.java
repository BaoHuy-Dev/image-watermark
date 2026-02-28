package com.watermark.sdk;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Perceptual Hash (dHash) for image fingerprinting.
 *
 * Unlike pixel-level watermarks, perceptual hashing works by creating
 * a compact "fingerprint" of the image's visual structure.
 *
 * This fingerprint is:
 * - Robust to screenshots, scaling, JPEG compression, format changes
 * - Does NOT modify the original image (zero visual impact)
 * - Compared using Hamming distance for fuzzy matching
 *
 * Algorithm (Difference Hash / dHash):
 * 1. Resize to 9x8 grayscale
 * 2. Compare each pixel with its right neighbor
 * 3. If left > right → 1, else → 0
 * 4. Result: 64-bit hash representing visual structure
 */
public final class PerceptualHash {

    private static final int HASH_WIDTH = 9;
    private static final int HASH_HEIGHT = 8;
    private static final int HASH_BITS = (HASH_WIDTH - 1) * HASH_HEIGHT; // 64 bits

    /**
     * Maximum Hamming distance to consider two images as matching.
     * Lower = stricter matching. 10 out of 64 bits ≈ 84% similarity.
     */
    public static final int MATCH_THRESHOLD = 10;

    private PerceptualHash() {
    }

    /**
     * Compute the perceptual hash (dHash) of an image.
     *
     * @param imageBytes image in any format
     * @return 64-character hex string representing the hash, or null on error
     */
    public static String computeHash(byte[] imageBytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null)
            return null;
        return computeHash(img);
    }

    /**
     * Compute the perceptual hash from a BufferedImage.
     */
    public static String computeHash(BufferedImage img) {
        // Step 1: Resize to 9x8
        BufferedImage small = resize(img, HASH_WIDTH, HASH_HEIGHT);

        // Step 2: Convert to grayscale values
        int[][] gray = new int[HASH_HEIGHT][HASH_WIDTH];
        for (int y = 0; y < HASH_HEIGHT; y++) {
            for (int x = 0; x < HASH_WIDTH; x++) {
                int rgb = small.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y][x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }

        // Step 3: Compute difference hash
        long hash = 0;
        for (int y = 0; y < HASH_HEIGHT; y++) {
            for (int x = 0; x < HASH_WIDTH - 1; x++) {
                hash <<= 1;
                if (gray[y][x] > gray[y][x + 1]) {
                    hash |= 1;
                }
            }
        }

        return String.format("%016x", hash);
    }

    /**
     * Compute Hamming distance between two hashes.
     * Lower distance = more similar.
     *
     * @return number of differing bits (0 = identical, 64 = completely different)
     */
    public static int hammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null)
            return 64;
        if (hash1.length() != 16 || hash2.length() != 16)
            return 64;

        try {
            long h1 = Long.parseUnsignedLong(hash1, 16);
            long h2 = Long.parseUnsignedLong(hash2, 16);
            return Long.bitCount(h1 ^ h2);
        } catch (NumberFormatException e) {
            return 64;
        }
    }

    /**
     * Check if two hashes match within the threshold.
     */
    public static boolean isMatch(String hash1, String hash2) {
        return hammingDistance(hash1, hash2) <= MATCH_THRESHOLD;
    }

    /**
     * Resize image using smooth scaling (area averaging).
     */
    private static BufferedImage resize(BufferedImage src, int w, int h) {
        Image scaled = src.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return result;
    }
}

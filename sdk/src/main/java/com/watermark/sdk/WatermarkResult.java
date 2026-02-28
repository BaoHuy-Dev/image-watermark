package com.watermark.sdk;

/**
 * Result of a watermark extraction operation.
 */
public class WatermarkResult {

    private final boolean found;
    private final String text;

    private WatermarkResult(boolean found, String text) {
        this.found = found;
        this.text = text;
    }

    public static WatermarkResult found(String text) {
        return new WatermarkResult(true, text);
    }

    public static WatermarkResult notFound() {
        return new WatermarkResult(false, null);
    }

    /** Whether a watermark was found in the image. */
    public boolean isFound() {
        return found;
    }

    /** The watermark text (null if not found). */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return found ? "WatermarkResult{found=true, text='" + text + "'}"
                : "WatermarkResult{found=false}";
    }
}

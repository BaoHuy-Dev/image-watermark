package com.watermark.sdk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WatermarkEngineTest {

    private static byte[] testImage;
    private final WatermarkEngine engine = new WatermarkEngine();

    @BeforeAll
    static void setup() throws IOException {
        testImage = createTestImage(300, 300);
    }

    @Test
    @DisplayName("Embed and extract plain text")
    void plainText() throws IOException {
        byte[] wm = engine.embed(testImage, "hello-world");
        WatermarkResult r = engine.extract(wm);
        assertTrue(r.isFound());
        assertEquals("hello-world", r.getText());
    }

    @Test
    @DisplayName("Embed and extract user info JSON")
    void userInfo() throws IOException {
        byte[] wm = engine.embedUserInfo(testImage, "usr-42", "test@mail.com");
        WatermarkResult r = engine.extract(wm);
        assertTrue(r.isFound());
        assertTrue(r.getText().contains("\"userId\":\"usr-42\""));
        assertTrue(r.getText().contains("\"email\":\"test@mail.com\""));
        assertTrue(r.getText().contains("\"ts\":"));
    }

    @Test
    @DisplayName("Embed and extract metadata map")
    void metadataMap() throws IOException {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("license", "premium");
        meta.put("org", "ACME Corp");

        byte[] wm = engine.embedMetadata(testImage, meta);
        WatermarkResult r = engine.extract(wm);
        assertTrue(r.isFound());
        assertTrue(r.getText().contains("\"license\":\"premium\""));
        assertTrue(r.getText().contains("\"org\":\"ACME Corp\""));
    }

    @Test
    @DisplayName("Extract from clean image returns not-found")
    void noWatermark() throws IOException {
        WatermarkResult r = engine.extract(testImage);
        assertFalse(r.isFound());
        assertNull(r.getText());
    }

    @Test
    @DisplayName("Unicode (Vietnamese) watermark roundtrip")
    void unicode() throws IOException {
        String vn = "Người dùng: Trần Văn B – Mã: VN-999";
        byte[] wm = engine.embed(testImage, vn);
        assertEquals(vn, engine.extract(wm).getText());
    }

    @Test
    @DisplayName("Image too small throws")
    void tooSmall() throws IOException {
        byte[] tiny = createTestImage(4, 4);
        assertThrows(IllegalArgumentException.class,
                () -> engine.embed(tiny, "this text is way too long for a 4x4 image!!!"));
    }

    // ========== HELPER ==========

    private static byte[] createTestImage(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                img.setRGB(x, y, new Color((x * 7) % 256, (y * 11) % 256, ((x + y) * 3) % 256, 255).getRGB());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}

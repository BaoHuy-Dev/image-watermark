package com.watermark.api.config;

import com.watermark.api.entity.Product;
import com.watermark.api.entity.User;
import com.watermark.api.repository.ProductRepository;
import com.watermark.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.storage.path}")
    private String storagePath;

    @Override
    public void run(String... args) throws Exception {
        Path storage = Paths.get(storagePath);
        Files.createDirectories(storage);

        // Seed users
        if (!userRepository.existsByEmail("admin@aura.digital")) {
            userRepository.save(User.builder()
                    .email("admin@aura.digital")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Marcus Thorne")
                    .role(User.Role.SELLER)
                    .build());
        }
        if (!userRepository.existsByEmail("alex@aura.digital")) {
            userRepository.save(User.builder()
                    .email("alex@aura.digital")
                    .password(passwordEncoder.encode("demo123"))
                    .fullName("Alexander Vance")
                    .role(User.Role.USER)
                    .build());
        }

        if (productRepository.count() > 0)
            return;

        // === Featured eBooks (matching stitch landing page) ===
        createImageProduct(storage, "art-of-stillness.png",
                "The Art of Stillness", "Julian Vought",
                "A masterful guide to finding clarity in an age of constant noise. Julian Vought explores the intersection of minimalist photography and mindfulness in this highly acclaimed digital volume. This premium eBook includes over 200 high-resolution plates and detailed essays on the philosophy of visual minimalism.",
                new BigDecimal("19.00"), new BigDecimal("34.00"),
                Product.ProductType.PDF, "eBooks", "photography,minimalism,mindfulness",
                50, 128, true,
                new Color(46, 125, 50), new Color(21, 101, 192));

        createImageProduct(storage, "modern-ux-theory.png",
                "Modern UX Theory", "Elena Rossi",
                "Comprehensive guide to modern user experience design principles. From research methodologies to interaction patterns, covering everything a designer needs to know.",
                new BigDecimal("24.00"), null,
                Product.ProductType.PDF, "eBooks", "design,ux,theory",
                45, 94, true,
                new Color(239, 235, 225), new Color(175, 148, 110));

        createImageProduct(storage, "content-masterclass.png",
                "Content Masterclass", "Marcus Thorne",
                "Expert knowledge in digital marketing strategies. Learn to create compelling content that drives engagement and converts. Includes case studies from Fortune 500 companies.",
                new BigDecimal("29.00"), null,
                Product.ProductType.PDF, "eBooks", "marketing,content,strategy",
                50, 215, true,
                new Color(178, 235, 242), new Color(0, 172, 193));

        createImageProduct(storage, "urban-sprawl.png",
                "Urban Sprawl", "Sarah Jenkins",
                "Architecture and interior design exploration through the lens of urban photography. 150+ stunning images of modern living spaces around the world.",
                new BigDecimal("15.00"), null,
                Product.ProductType.PDF, "eBooks", "architecture,interior,photography",
                40, 52, true,
                new Color(250, 245, 230), new Color(90, 91, 69));

        // === Premium Artwork (matching stitch gallery) ===
        createImageProduct(storage, "chromatic-fusion.png",
                "Chromatic Fusion", "Digital Abstract Collection",
                "Vibrant abstract digital artwork, perfect for modern interiors. 4K resolution, ready for large-format printing. Personal and commercial license included.",
                new BigDecimal("45.00"), null,
                Product.ProductType.IMAGE, "Artwork", "abstract,4k,colorful",
                48, 87, true,
                new Color(255, 87, 34), new Color(63, 81, 181));

        createImageProduct(storage, "desert-monolith.png",
                "Desert Monolith", "Minimalist Architecture",
                "Geometric minimalist architectural forms in warm desert light. Ultra high-resolution 4K image. Perfect for professional printing and large canvas displays.",
                new BigDecimal("39.00"), null,
                Product.ProductType.IMAGE, "Artwork", "minimalist,architecture,4k",
                46, 63, false,
                new Color(255, 183, 77), new Color(230, 74, 25));

        createImageProduct(storage, "oceanic-depths.png",
                "Oceanic Depths", "Ethereal Renderings",
                "Deep ocean abstract digital render with flowing organic lines. Mesmerizing blue tones that create a sense of depth and tranquility. 4K resolution.",
                new BigDecimal("55.00"), null,
                Product.ProductType.IMAGE, "Artwork", "ocean,abstract,4k,blue",
                50, 142, true,
                new Color(0, 105, 148), new Color(0, 37, 56));

        // === PDF Books ===
        createPdfProduct(storage, "java-clean-code.pdf",
                "Java Clean Code Guide", "Robert Martinez",
                "Complete guide to writing clean, maintainable Java code. Covers SOLID principles, design patterns, testing strategies, and Spring Boot best practices. 214 pages of expert knowledge.",
                new BigDecimal("49.00"), new BigDecimal("69.00"),
                "Programming", "java,clean-code,spring-boot",
                48, 312, false);

        createPdfProduct(storage, "design-patterns-practical.pdf",
                "Design Patterns in Practice", "Anna Chen",
                "Real-world applications of GoF design patterns with modern Java examples. Each pattern is explained with production-ready code and practical use cases.",
                new BigDecimal("35.00"), null,
                "Programming", "design-patterns,java,software-engineering",
                47, 198, false);

        log.info("Seeded {} products for Aura Digital", productRepository.count());
    }

    private void createImageProduct(Path storage, String fileName, String title, String author,
            String desc, BigDecimal price, BigDecimal originalPrice,
            Product.ProductType type, String category, String tags,
            int rating, int reviewCount, boolean featured,
            Color c1, Color c2) throws IOException {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(0, 0, c1, 800, 600, c2);
        g.setPaint(gp);
        g.fillRect(0, 0, 800, 600);

        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(100, 100, 300, 300);
        g.fillOval(450, 150, 250, 250);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (800 - fm.stringWidth(title)) / 2, 290);

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(255, 255, 255, 200));
        fm = g.getFontMetrics();
        g.drawString("by " + author, (800 - fm.stringWidth("by " + author)) / 2, 330);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        Files.write(storage.resolve(fileName), out.toByteArray());

        productRepository.save(Product.builder()
                .title(title).author(author).description(desc)
                .price(price).originalPrice(originalPrice)
                .productType(type).fileUrl(fileName)
                .thumbnailUrl("https://placehold.co/400x500/1f2838/ffffff?text=" + title.replace(" ", "+"))
                .category(category).tags(tags)
                .rating(rating).reviewCount(reviewCount).featured(featured)
                .build());
    }

    private void createPdfProduct(Path storage, String fileName, String title, String author,
            String desc, BigDecimal price, BigDecimal originalPrice,
            String category, String tags,
            int rating, int reviewCount, boolean featured) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                cs.beginText();
                cs.setFont(titleFont, 28);
                cs.newLineAtOffset(50, 750);
                cs.showText(title);
                cs.endText();
                cs.beginText();
                cs.setFont(bodyFont, 14);
                cs.newLineAtOffset(50, 710);
                cs.showText("by " + author);
                cs.endText();
                cs.beginText();
                cs.setFont(bodyFont, 12);
                cs.newLineAtOffset(50, 670);
                cs.showText("This is a sample PDF for the Aura Digital marketplace.");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            Files.write(storage.resolve(fileName), out.toByteArray());
        }

        productRepository.save(Product.builder()
                .title(title).author(author).description(desc)
                .price(price).originalPrice(originalPrice)
                .productType(Product.ProductType.PDF).fileUrl(fileName)
                .thumbnailUrl("https://placehold.co/400x500/1f2838/ffffff?text=" + title.replace(" ", "+"))
                .category(category).tags(tags)
                .rating(rating).reviewCount(reviewCount).featured(featured)
                .build());
    }
}

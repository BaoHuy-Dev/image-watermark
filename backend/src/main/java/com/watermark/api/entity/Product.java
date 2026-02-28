package com.watermark.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal originalPrice; // for discount display

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    private String fileUrl; // path in storage
    private String thumbnailUrl; // image URL
    private String category;
    private String tags; // comma-separated

    @Builder.Default
    private Integer rating = 50; // out of 50 (5.0 stars)

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ProductType {
        PDF, IMAGE
    }
}

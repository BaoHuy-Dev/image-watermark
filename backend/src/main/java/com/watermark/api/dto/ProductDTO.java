package com.watermark.api.dto;

import com.watermark.api.entity.Product;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private UUID id;
    private String title;
    private String author;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String productType;
    private String thumbnailUrl;
    private String category;
    private String tags;
    private Integer rating;
    private Integer reviewCount;
    private Boolean featured;
    private String createdAt;

    public static ProductDTO from(Product p) {
        return ProductDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .author(p.getAuthor())
                .description(p.getDescription())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .productType(p.getProductType().name())
                .thumbnailUrl(p.getThumbnailUrl())
                .category(p.getCategory())
                .tags(p.getTags())
                .rating(p.getRating())
                .reviewCount(p.getReviewCount())
                .featured(p.getFeatured())
                .createdAt(p.getCreatedAt().toString())
                .build();
    }
}

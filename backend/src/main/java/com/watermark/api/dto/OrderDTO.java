package com.watermark.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private UUID id;
    private BigDecimal totalAmount;
    private BigDecimal processingFee;
    private String status;
    private String createdAt;
    private List<OrderItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDTO {
        private UUID productId;
        private String title;
        private String productType;
        private String thumbnailUrl;
        private BigDecimal price;
    }
}

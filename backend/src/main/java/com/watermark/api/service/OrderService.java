package com.watermark.api.service;

import com.watermark.api.dto.OrderDTO;
import com.watermark.api.entity.*;
import com.watermark.api.repository.OrderRepository;
import com.watermark.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private static final BigDecimal PROCESSING_FEE_RATE = new BigDecimal("0.025"); // 2.5%

    @Transactional
    public OrderDTO createOrder(User user, List<UUID> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        if (products.isEmpty())
            throw new IllegalArgumentException("No valid products found");

        BigDecimal subtotal = products.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fee = subtotal.multiply(PROCESSING_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(fee);

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .processingFee(fee)
                .status(Order.OrderStatus.COMPLETED)
                .build();

        List<OrderItem> items = products.stream()
                .map(p -> OrderItem.builder().order(order).product(p).price(p.getPrice()).build())
                .collect(Collectors.toList());
        order.setItems(items);

        orderRepository.save(order);
        return toDTO(order);
    }

    public List<OrderDTO> getMyOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public boolean ownsProduct(UUID userId, UUID productId) {
        return orderRepository.existsByUserIdAndItems_ProductId(userId, productId);
    }

    private OrderDTO toDTO(Order o) {
        return OrderDTO.builder()
                .id(o.getId())
                .totalAmount(o.getTotalAmount())
                .processingFee(o.getProcessingFee())
                .status(o.getStatus().name())
                .createdAt(o.getCreatedAt().toString())
                .items(o.getItems().stream().map(i -> OrderDTO.OrderItemDTO.builder()
                        .productId(i.getProduct().getId())
                        .title(i.getProduct().getTitle())
                        .productType(i.getProduct().getProductType().name())
                        .thumbnailUrl(i.getProduct().getThumbnailUrl())
                        .price(i.getPrice())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}

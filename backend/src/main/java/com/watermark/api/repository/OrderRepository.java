package com.watermark.api.repository;

import com.watermark.api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndItems_ProductId(UUID userId, UUID productId);
}

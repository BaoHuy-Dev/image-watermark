package com.watermark.api.controller;

import com.watermark.api.dto.OrderDTO;
import com.watermark.api.entity.User;
import com.watermark.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

        private final OrderService orderService;

        @PostMapping
        public ResponseEntity<?> createOrder(@AuthenticationPrincipal User user,
                        @RequestBody Map<String, List<UUID>> body) {
                List<UUID> productIds = body.get("productIds");
                if (productIds == null || productIds.isEmpty())
                        return ResponseEntity.badRequest().body(Map.of("error", "productIds required"));
                try {
                        return ResponseEntity.ok(orderService.createOrder(user, productIds));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/my")
        public List<OrderDTO> myOrders(@AuthenticationPrincipal User user) {
                return orderService.getMyOrders(user.getId());
        }

        @GetMapping("/owns/{productId}")
        public Map<String, Boolean> ownsProduct(@AuthenticationPrincipal User user,
                        @PathVariable UUID productId) {
                return Map.of("owns", orderService.ownsProduct(user.getId(), productId));
        }
}

package com.watermark.api.repository;

import com.watermark.api.entity.WatermarkFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FingerprintRepository extends JpaRepository<WatermarkFingerprint, UUID> {
    List<WatermarkFingerprint> findAll();

    void deleteByUserIdAndProductId(UUID userId, UUID productId);
}

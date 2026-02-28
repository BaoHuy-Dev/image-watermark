package com.watermark.api.repository;

import com.watermark.api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByProductType(Product.ProductType type);

    List<Product> findByFeaturedTrue();

    List<Product> findByCategory(String category);

    List<Product> findByTitleContainingIgnoreCase(String keyword);
}

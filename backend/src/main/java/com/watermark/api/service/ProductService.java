package com.watermark.api.service;

import com.watermark.api.dto.ProductDTO;
import com.watermark.api.entity.Product;
import com.watermark.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductDTO> findAll(String type, String category, String search) {
        List<Product> products;

        if (search != null && !search.isBlank()) {
            products = productRepository.findByTitleContainingIgnoreCase(search);
        } else if (type != null && !type.isBlank()) {
            products = productRepository.findByProductType(Product.ProductType.valueOf(type.toUpperCase()));
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }

        return products.stream().map(ProductDTO::from).collect(Collectors.toList());
    }

    public Optional<ProductDTO> findById(UUID id) {
        return productRepository.findById(id).map(ProductDTO::from);
    }

    public List<ProductDTO> findFeatured() {
        return productRepository.findByFeaturedTrue()
                .stream().map(ProductDTO::from).collect(Collectors.toList());
    }

    public ProductDTO createProduct(Product product) {
        return ProductDTO.from(productRepository.save(product));
    }
}

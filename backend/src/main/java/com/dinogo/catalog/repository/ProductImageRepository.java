package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.catalog.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProductProductIdOrderBySortOrderAsc(Integer productId);

    Optional<ProductImage> findFirstByProductProductIdAndIsMainTrue(Integer productId);

    List<ProductImage> findByProductProductId(Integer productId);
}
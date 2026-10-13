package com.dinogo.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.catalog.entity.ProductSku;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {
    List<ProductSku> findByProductProductId(Integer productId);

    @Modifying
    @Query("UPDATE ProductSku sku SET sku.stock = sku.stock - :quantity "
            + "WHERE sku.skuId = :skuId AND sku.status = 1 AND sku.stock >= :quantity")
    int deductStockIfAvailable(@Param("skuId") Integer skuId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE ProductSku sku SET sku.stock = sku.stock + :quantity WHERE sku.skuId = :skuId")
    int restoreStock(@Param("skuId") Integer skuId, @Param("quantity") Integer quantity);

    List<ProductSku> findByProductProductIdAndStatus(
            Integer productId,
            Byte status);
}

package com.dinogo.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.catalog.entity.Product;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface ProductRepository extends JpaRepository<Product, Integer>,
                JpaSpecificationExecutor<Product> {

        List<Product> findBySeller_SellerIdOrderByProductIdDesc(Integer sellerId);

        Optional<Product> findBySeller_SellerIdAndProductId(
                        Integer sellerId,
                        Integer productId);

        List<Product> findBySeller_SellerIdAndStatusNotOrderByProductIdDesc(
                        Integer sellerId,
                        Byte status);

        long countBySeller_SellerIdAndStatus(Integer sellerId, Byte status);

        @Query("""
                        SELECT COALESCE(SUM(p.soldCount), 0)
                        FROM Product p
                        WHERE p.seller.sellerId = :sellerId
                        """)
        long sumSoldCountBySellerId(@Param("sellerId") Integer sellerId);

        @Modifying
        @Query("""
                        UPDATE Product p
                        SET p.soldCount = p.soldCount + :quantity
                        WHERE p.productId = :productId
                        """)
        int increaseSoldCount(
                        @Param("productId") Integer productId,
                        @Param("quantity") Integer quantity);

        @Modifying
        @Query("""
                        UPDATE Product p
                        SET p.soldCount =
                            CASE
                                WHEN p.soldCount >= :quantity
                                THEN p.soldCount - :quantity
                                ELSE 0
                            END
                        WHERE p.productId = :productId
                        """)
        int decreaseSoldCount(
                        @Param("productId") Integer productId,
                        @Param("quantity") Integer quantity);

}

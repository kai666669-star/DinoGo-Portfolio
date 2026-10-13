package com.dinogo.seller.dto;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import java.math.BigDecimal;
import java.util.Comparator;

public record SellerProductResponse(
        Integer productId,
        Integer sellerId,
        String productName,
        BigDecimal basePrice,
        Integer stock,
        String status,
        String imageUrl) {

    public static SellerProductResponse from(Product product) {
        Integer stock = product.getSkus()
                .stream()
                .map(ProductSku::getStock)
                .filter(value -> value != null)
                .reduce(0, Integer::sum);

        return new SellerProductResponse(
                product.getProductId(),
                product.getSeller().getSellerId(),
                product.getProductName(),
                product.getBasePrice(),
                stock,
                statusLabel(product.getStatus()),
                mainImageUrl(product));
    }

    private static String mainImageUrl(Product product) {
        return product.getImages()
                .stream()
                .sorted(Comparator
                        .comparing(ProductImage::getIsMain, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductImage::getImageId, Comparator.nullsLast(Integer::compareTo)))
                .map(ProductImage::getImageUrl)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String statusLabel(Byte status) {
        if (status == null) {
            return "DRAFT";
        }
        return switch (status) {
            case 1 -> "ACTIVE";
            case 2 -> "INACTIVE";
            default -> "DRAFT";
        };
    }
}

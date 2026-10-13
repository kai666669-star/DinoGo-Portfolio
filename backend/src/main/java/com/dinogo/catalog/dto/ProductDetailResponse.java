package com.dinogo.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Integer productId;

    private Integer sellerId;

    private String productName;

    private String description;

    private BigDecimal basePrice;

    private Byte status;

    private Integer viewCount;

    private Integer soldCount;

    // 品牌
    private Integer brandId;
    private String brandName;

    // 分類
    private Integer subcategoryId;
    private String subcategoryName;

    private Integer categoryId;
    private String categoryName;

    // 商品圖片
    private List<ProductImageResponse> images;

    // 商品規格
    private List<ProductSkuResponse> skus;
}
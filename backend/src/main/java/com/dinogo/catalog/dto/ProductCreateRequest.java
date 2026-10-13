package com.dinogo.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest {

    // 暫時由前端傳，之後登入功能完成後改成從登入身分取得
    @NotNull(message = "賣家不可為空")
    private Integer sellerId;

    @NotNull(message = "商品分類不可為空")
    private Integer subcategoryId;

    @NotNull(message = "品牌不可為空")
    private Integer brandId;

    @NotBlank(message = "商品名稱不可為空")
    @Size(max = 50, message = "商品名稱不可超過50字")
    private String productName;

    @Size(max = 3000, message = "商品描述不可超過3000字")
    private String description;

    @NotNull(message = "商品價格不可為空")
    @DecimalMin(value = "0.0", inclusive = false, message = "商品價格必須大於0")
    private BigDecimal basePrice;

    // SKU
    @NotNull(message = "商品庫存不可為空")
    @Min(value = 0, message = "商品庫存不可小於0")
    private List<ProductSkuCreateRequest> skus;

    // 商品圖片
    @NotBlank(message = "商品圖片不可為空")
    private List<ProductImageCreateRequest> images;

    // status 0 = 草稿、1 = 上架、2 = 下架
    @Min(value = 0, message = "商品狀態錯誤")
    @Max(value = 2, message = "商品狀態錯誤")
    private Byte status;
}
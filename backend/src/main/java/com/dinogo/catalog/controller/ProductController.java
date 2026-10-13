package com.dinogo.catalog.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.dto.ProductImageResponse;
import com.dinogo.catalog.dto.ProductImageSortUpdateRequest;
import com.dinogo.catalog.dto.ProductMainImageUpdateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductSkuCreateRequest;
import com.dinogo.catalog.dto.ProductSkuResponse;
import com.dinogo.catalog.dto.ProductSkuUpdateRequest;
import com.dinogo.catalog.dto.ProductUpdateRequest;
import com.dinogo.catalog.service.ProductService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 初始路徑返回所有產品列表
    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer subcategoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating) {

        return productService.getProducts(
                keyword,
                categoryId,
                subcategoryId,
                brandId,
                sellerId,
                minPrice,
                maxPrice,
                minRating,
                page,
                size,
                sort);
    }

    // 建立商品
    @PostMapping
    public ProductResponse createProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request, member.memberId());
    }

    // 上架商品
    @PatchMapping("/{productId}/publish")
    public ProductResponse publishProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId) {

        return productService.publishProduct(productId, member.memberId());
    }

    // 下架商品
    @PatchMapping("/{productId}/unpublish")
    public ProductResponse unpublishProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId) {

        return productService.unpublishProduct(productId, member.memberId());
    }

    // 商品詳情
    @GetMapping("/{productId}")
    public ProductDetailResponse getProductDetail(
            @PathVariable Integer productId) {

        return productService.getProductDetail(productId);
    }

    // 修改商品詳情
    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @RequestBody ProductUpdateRequest request) {

        return productService.updateProduct(productId, request, member.memberId());
    }

    // 修改商品SKU
    @PutMapping("/{productId}/skus/{skuId}")
    public ProductSkuResponse updateSku(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @PathVariable Integer skuId,
            @Valid @RequestBody ProductSkuUpdateRequest request) {

        return productService.updateSku(productId, skuId, request, member.memberId());
    }

    @PostMapping("/{productId}/skus/batch")
    public List<ProductSkuResponse> createSkus(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @Valid @RequestBody List<ProductSkuCreateRequest> requests) {

        return productService.createSkus(productId, requests, member.memberId());
    }

    @PatchMapping("/{productId}/skus/{skuId}/disable")
    public ProductSkuResponse disableSku(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @PathVariable Integer skuId) {

        return productService.disableSku(productId, skuId, member.memberId());
    }

    // 修改商品主圖
    @PutMapping("/{productId}/images/main")
    public ProductImageResponse updateMainImage(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @RequestBody ProductMainImageUpdateRequest request) {

        return productService.updateMainImage(
                productId,
                request.getImageId(),
                member.memberId());
    }

    // 修改商品排序
    @PutMapping("/{productId}/images/sort")
    public List<ProductImageResponse> updateImageSort(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @RequestBody List<ProductImageSortUpdateRequest> requests) {

        return productService.updateImageSort(productId, requests, member.memberId());
    }

    // 刪除商品圖片
    @DeleteMapping("/{productId}/images/{imageId}")
    public void deleteImage(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId,
            @PathVariable Integer imageId) {

        productService.deleteImage(productId, imageId, member.memberId());
    }

    @PostMapping("/{productId}/images/upload")
    public List<ProductImageResponse> uploadProductImages(
            @PathVariable Integer productId,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal AuthenticatedMember principal) {

        System.out.println("=== upload image debug ===");
        System.out.println("principal = " + principal);

        if (principal == null) {
            throw new RuntimeException("找不到登入會員 principal");
        }

        Integer memberId = principal.memberId();

        System.out.println("memberId = " + memberId);
        System.out.println("files length = " + files.length);

        return productService.uploadProductImages(
                productId,
                files,
                memberId);
    }

    // 商品軟刪除
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId) {

        productService.deleteProduct(productId, member.memberId());

        return ResponseEntity.noContent().build();
    }

    // 商品圖片上傳
    @PostMapping(value = "/description-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadDescriptionImage(
            @RequestParam("file") MultipartFile file) {

        return productService.uploadDescriptionImage(file);
    }
}

package com.dinogo.catalog.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dinogo.catalog.dto.ProductCreateRequest;
import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.dto.ProductImageCreateRequest;
import com.dinogo.catalog.dto.ProductImageResponse;
import com.dinogo.catalog.dto.ProductImageSortUpdateRequest;
import com.dinogo.catalog.dto.ProductResponse;
import com.dinogo.catalog.dto.ProductSkuCreateRequest;
import com.dinogo.catalog.dto.ProductSkuResponse;
import com.dinogo.catalog.dto.ProductSkuUpdateRequest;
import com.dinogo.catalog.dto.ProductUpdateRequest;
import com.dinogo.catalog.entity.Brand;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.entity.Subcategory;
import com.dinogo.catalog.repository.BrandRepository;
import com.dinogo.catalog.repository.ProductImageRepository;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.catalog.repository.SubcategoryRepository;
import com.dinogo.review.entity.StarEntity;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;
import com.dinogo.seller.service.CurrentSellerService;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

        private final ProductRepository productRepository;
        private final SellerRepository sellerRepository;
        private final SubcategoryRepository subcategoryRepository;
        private final BrandRepository brandRepository;
        private final ProductSkuRepository productSkuRepository;
        private final ProductImageRepository productImageRepository;
        private final CurrentSellerService currentSellerService;
        private final Cloudinary cloudinary;

        private ProductResponse toProductResponse(Product product) {

                BigDecimal minPrice = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .filter(sku -> sku.getPrice() != null)
                                .map(ProductSku::getPrice)
                                .min(BigDecimal::compareTo)
                                .orElse(product.getBasePrice());

                BigDecimal maxPrice = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .filter(sku -> sku.getPrice() != null)
                                .map(ProductSku::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(product.getBasePrice());

                return new ProductResponse(
                                product.getProductId(),
                                product.getSeller().getSellerId(),
                                product.getSubcategory().getSubcategoryId(),
                                product.getBrand().getBrandId(),
                                product.getProductName(),
                                product.getDescription(),
                                product.getBasePrice(),
                                minPrice,
                                maxPrice,
                                product.getSkus()
                                                .stream()
                                                .filter(sku -> sku.getStatus() == 1)
                                                .mapToInt(ProductSku::getStock)
                                                .sum(),
                                product.getImages().isEmpty()
                                                ? null
                                                : product.getImages().get(0).getImageUrl(),
                                product.getStatus(),
                                product.getSoldCount());
        }

        private ProductSkuResponse toProductSkuResponse(ProductSku sku) {

                return new ProductSkuResponse(
                                sku.getSkuId(),
                                sku.getProduct().getProductId(),
                                sku.getSpec1Name(),
                                sku.getSpec1Value(),
                                sku.getSpec2Name(),
                                sku.getSpec2Value(),
                                sku.getPrice(),
                                sku.getStock(),
                                sku.getStatus());
        }

        private Product requireSellerProduct(Integer productId, Integer memberId) {
                Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
                return productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));
        }

        // 建立商品
        @Transactional
        public ProductResponse createProduct(ProductCreateRequest request, Integer memberId) {
                Integer sellerId = currentSellerService.requireMatchingActiveSellerId(
                                memberId,
                                request.getSellerId());
                request.setSellerId(sellerId);
                return createProduct(request);
        }

        @Transactional
        public ProductResponse createProduct(ProductCreateRequest request) {
                Seller seller = sellerRepository
                                .findById(request.getSellerId())
                                .orElseThrow(() -> new RuntimeException("找不到賣家"));

                Subcategory subcategory = subcategoryRepository
                                .findById(request.getSubcategoryId())
                                .orElseThrow(() -> new RuntimeException("找不到子分類"));

                Brand brand = brandRepository
                                .findById(request.getBrandId())
                                .orElseThrow(() -> new RuntimeException("找不到品牌"));

                // =====================
                // 1. 建立 Product
                // =====================

                Product product = new Product();

                product.setSeller(seller);
                product.setSubcategory(subcategory);
                product.setBrand(brand);
                product.setProductName(request.getProductName());
                product.setDescription(request.getDescription());
                product.setBasePrice(request.getBasePrice());

                // 沒傳 status 就預設草稿
                product.setStatus(
                                request.getStatus() != null
                                                ? request.getStatus()
                                                : (byte) 0);

                Product savedProduct = productRepository.save(product);

                // =====================
                // 2. 建立 ProductSku
                // =====================

                if (request.getSkus() != null) {
                        for (ProductSkuCreateRequest skuCreateRequest : request.getSkus()) {

                                ProductSku sku = ProductSku.builder()
                                                .product(product)
                                                .spec1Name(skuCreateRequest.getSpec1Name())
                                                .spec1Value(skuCreateRequest.getSpec1Value())
                                                .spec2Name(skuCreateRequest.getSpec2Name())
                                                .spec2Value(skuCreateRequest.getSpec2Value())
                                                .price(skuCreateRequest.getPrice())
                                                .stock(skuCreateRequest.getStock())
                                                .status((byte) 1)
                                                .build();

                                ProductSku savedSku = productSkuRepository.save(sku);
                                savedProduct.getSkus().add(savedSku);
                        }
                }

                // =====================
                // 3. 建立 ProductImage
                // =====================
                if (request.getImages() != null) {
                        for (int i = 0; i < request.getImages().size(); i++) {

                                ProductImageCreateRequest imageRequest = request.getImages().get(i);

                                ProductImage image = ProductImage.builder()
                                                .product(product)
                                                .imageUrl(imageRequest.getImageUrl())
                                                .sortOrder(imageRequest.getSortOrder())
                                                .isMain(i == 0)
                                                .build();

                                ProductImage savedImg = productImageRepository.save(image);
                                savedProduct.getImages().add(savedImg);
                        }
                }

                savedProduct.getSkus().forEach(sku -> {
                        System.out.println(
                                        "skuId = " + sku.getSkuId()
                                                        + ", stock = " + sku.getStock()
                                                        + ", status = " + sku.getStatus());
                });

                System.out.println("圖片數量 = " + savedProduct.getImages().size());

                savedProduct.getImages().forEach(image -> {
                        System.out.println(
                                        "imageId = " + image.getImageId()
                                                        + ", imageUrl = " + image.getImageUrl()
                                                        + ", isMain = " + image.getIsMain());
                });

                return toProductResponse(savedProduct);
        }

        // 上架商品
        public ProductResponse publishProduct(Integer productId, Integer memberId) {
                Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
                return publishProductForSeller(productId, sellerId);
        }

        public ProductResponse publishProduct(Integer productId) {
                return publishProductForSeller(productId, 1);
        }

        private ProductResponse publishProductForSeller(Integer productId, Integer sellerId) {
                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                // 草稿 0 或下架 2 才能上架
                if (product.getStatus() != 0 && product.getStatus() != 2) {
                        throw new IllegalArgumentException("目前狀態不可上架");
                }

                product.setStatus((byte) 1);

                Product savedProduct = productRepository.save(product);

                return toProductResponse(savedProduct);
        }

        // 下架商品
        public ProductResponse unpublishProduct(Integer productId, Integer memberId) {
                Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
                return unpublishProductForSeller(productId, sellerId);
        }

        public ProductResponse unpublishProduct(Integer productId) {
                return unpublishProductForSeller(productId, 1);
        }

        private ProductResponse unpublishProductForSeller(Integer productId, Integer sellerId) {
                Product product = productRepository
                                .findBySeller_SellerIdAndProductId(sellerId, productId)
                                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的商品"));

                // 只有上架 1 才能下架
                if (product.getStatus() != 1) {
                        throw new IllegalArgumentException("只有上架中的商品可以下架");
                }

                product.setStatus((byte) 2);

                Product savedProduct = productRepository.save(product);

                return toProductResponse(savedProduct);
        }

        // 讀取商品列表
        public Page<ProductResponse> getProducts(
                        String keyword,
                        Integer categoryId,
                        Integer subcategoryId,
                        Integer brandId,
                        Integer sellerId,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        Double minRating,
                        Integer page,
                        Integer size,
                        String sort) {

                Specification<Product> spec = (root, query, cb) -> {

                        List<Predicate> predicates = new ArrayList<>();

                        // 買家只能看到已上架商品
                        predicates.add(
                                        cb.equal(root.get("status"), (byte) 1));

                        if (keyword != null && !keyword.isBlank()) {
                                predicates.add(
                                                cb.like(
                                                                cb.lower(root.get("productName")),
                                                                "%" + keyword.toLowerCase() + "%"));
                        }
                        // 大分類
                        if (categoryId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("subcategory")
                                                                                .join("category")
                                                                                .get("categoryId"),
                                                                categoryId));
                        }

                        // 子分類
                        if (subcategoryId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("subcategory")
                                                                                .get("subcategoryId"),
                                                                subcategoryId));
                        }

                        // 品牌
                        if (brandId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("brand")
                                                                                .get("brandId"),
                                                                brandId));
                        }
                        // 賣家
                        if (sellerId != null) {
                                predicates.add(
                                                cb.equal(
                                                                root.join("seller")
                                                                                .get("sellerId"),
                                                                sellerId));
                        }
                        // 最低價格
                        if (minPrice != null) {
                                predicates.add(
                                                cb.greaterThanOrEqualTo(
                                                                root.get("minSkuPrice"),
                                                                minPrice));
                        }

                        // 最高價格
                        if (maxPrice != null) {
                                predicates.add(
                                                cb.lessThanOrEqualTo(
                                                                root.get("minSkuPrice"),
                                                                maxPrice));
                        }

                        // 商品評價
                        if (minRating != null) {

                                Subquery<Double> ratingSubquery = query.subquery(Double.class);
                                Root<StarEntity> starRoot = ratingSubquery.from(StarEntity.class);

                                ratingSubquery.select(
                                                cb.avg(starRoot.get("fiveStar")));

                                ratingSubquery.where(
                                                cb.equal(
                                                                starRoot.get("productId"),
                                                                root.get("productId")),
                                                cb.isNotNull(starRoot.get("fiveStar")));

                                predicates.add(
                                                cb.greaterThanOrEqualTo(
                                                                ratingSubquery,
                                                                minRating));
                        }
                        return cb.and(
                                        predicates.toArray(new Predicate[0]));
                };

                Sort sorting = switch (sort == null ? "" : sort) {
                        case "priceAsc" ->
                                Sort.by(Sort.Direction.ASC, "minSkuPrice");

                        case "priceDesc" ->
                                Sort.by(Sort.Direction.DESC, "minSkuPrice");

                        case "salesDesc" ->
                                Sort.by(Sort.Direction.DESC, "soldCount");

                        case "newest" ->
                                Sort.by(Sort.Direction.DESC, "createdAt");

                        default ->
                                Sort.by(Sort.Direction.DESC, "productId");
                };

                Pageable pageable = PageRequest.of(page, size, sorting);

                Page<Product> products = productRepository.findAll(spec, pageable);

                return products.map(this::toProductResponse);
        }

        // 讀取商品詳情
        public ProductDetailResponse getProductDetail(Integer productId) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品"));

                if (product.getStatus() != 1) {
                        throw new RuntimeException("商品不存在或目前未上架");
                }

                List<ProductImageResponse> images = product.getImages()
                                .stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .isMain(image.getIsMain())
                                                .build())
                                .toList();

                List<ProductSkuResponse> skus = product.getSkus()
                                .stream()
                                .filter(sku -> sku.getStatus() == 1)
                                .map(sku -> ProductSkuResponse.builder()
                                                .skuId(sku.getSkuId())
                                                .spec1Name(sku.getSpec1Name())
                                                .spec1Value(sku.getSpec1Value())
                                                .spec2Name(sku.getSpec2Name())
                                                .spec2Value(sku.getSpec2Value())
                                                .price(sku.getPrice())
                                                .stock(sku.getStock())
                                                .status(sku.getStatus())
                                                .build())
                                .toList();

                return ProductDetailResponse.builder()
                                .productId(product.getProductId())
                                .sellerId(product.getSeller().getSellerId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .basePrice(product.getBasePrice())
                                .status(product.getStatus())
                                .viewCount(product.getViewCount())
                                .soldCount(product.getSoldCount())

                                .brandId(product.getBrand().getBrandId())
                                .brandName(product.getBrand().getBrandName())

                                .subcategoryId(product.getSubcategory().getSubcategoryId())
                                .subcategoryName(product.getSubcategory().getSubcategoryName())

                                .categoryId(product.getSubcategory().getCategory().getCategoryId())
                                .categoryName(product.getSubcategory().getCategory().getCategoryName())

                                .images(images)
                                .skus(skus)

                                .build();
        }

        // 賣家讀取商品詳情
        // 包含已停用的 SKU，供編輯商品使用
        public ProductDetailResponse getSellerProductDetail(Integer productId, Integer memberId) {
                requireSellerProduct(productId, memberId);
                return getSellerProductDetail(productId);
        }

        public ProductDetailResponse getSellerProductDetail(Integer productId) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品"));

                List<ProductImageResponse> images = product.getImages()
                                .stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .isMain(image.getIsMain())
                                                .build())
                                .toList();

                // 賣家需要看到全部 SKU，包括 status = 0
                List<ProductSkuResponse> skus = product.getSkus()
                                .stream()
                                .map(sku -> ProductSkuResponse.builder()
                                                .skuId(sku.getSkuId())
                                                .spec1Name(sku.getSpec1Name())
                                                .spec1Value(sku.getSpec1Value())
                                                .spec2Name(sku.getSpec2Name())
                                                .spec2Value(sku.getSpec2Value())
                                                .price(sku.getPrice())
                                                .stock(sku.getStock())
                                                .status(sku.getStatus())
                                                .build())
                                .toList();

                return ProductDetailResponse.builder()
                                .productId(product.getProductId())
                                .sellerId(product.getSeller().getSellerId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .basePrice(product.getBasePrice())
                                .status(product.getStatus())
                                .viewCount(product.getViewCount())
                                .soldCount(product.getSoldCount())

                                .brandId(product.getBrand().getBrandId())
                                .brandName(product.getBrand().getBrandName())

                                .subcategoryId(product.getSubcategory().getSubcategoryId())
                                .subcategoryName(product.getSubcategory().getSubcategoryName())

                                .categoryId(product.getSubcategory().getCategory().getCategoryId())
                                .categoryName(product.getSubcategory().getCategory().getCategoryName())

                                .images(images)
                                .skus(skus)

                                .build();
        }

        // 修改商品
        public ProductResponse updateProduct(
                        Integer productId,
                        ProductUpdateRequest request,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                return updateProduct(productId, request);
        }

        public ProductResponse updateProduct(
                        Integer productId,
                        ProductUpdateRequest request) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品：" + productId));

                // 修改子分類
                if (request.getSubcategoryId() != null) {
                        Subcategory subcategory = subcategoryRepository
                                        .findById(request.getSubcategoryId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到子分類：" + request.getSubcategoryId()));

                        product.setSubcategory(subcategory);
                }

                // 修改品牌
                if (request.getBrandId() != null) {
                        Brand brand = brandRepository
                                        .findById(request.getBrandId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到品牌：" + request.getBrandId()));

                        product.setBrand(brand);
                }

                // 修改商品名稱
                if (request.getProductName() != null) {
                        product.setProductName(request.getProductName());
                }

                // 修改商品描述
                if (request.getDescription() != null) {
                        product.setDescription(request.getDescription());
                }

                // 修改基本價格
                if (request.getBasePrice() != null) {
                        product.setBasePrice(request.getBasePrice());
                }
                // 修改狀態
                if (request.getStatus() != null) {
                        product.setStatus(request.getStatus());
                }
                Product updatedProduct = productRepository.save(product);

                return toProductResponse(updatedProduct);
        }

        // 修改商品Sku
        public ProductSkuResponse updateSku(
                        Integer productId,
                        Integer skuId,
                        ProductSkuUpdateRequest request,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                return updateSku(productId, skuId, request);
        }

        public ProductSkuResponse updateSku(
                        Integer productId,
                        Integer skuId,
                        ProductSkuUpdateRequest request) {

                ProductSku sku = productSkuRepository.findById(skuId)
                                .orElseThrow(() -> new RuntimeException("找不到 SKU：" + skuId));

                // 確認 SKU 真的是這個商品的
                if (!sku.getProduct().getProductId().equals(productId)) {
                        throw new RuntimeException("此 SKU 不屬於指定商品");
                }

                if (request.getSpec1Name() != null) {
                        sku.setSpec1Name(request.getSpec1Name());
                }

                if (request.getSpec1Value() != null) {
                        sku.setSpec1Value(request.getSpec1Value());
                }

                if (request.getSpec2Name() != null) {
                        sku.setSpec2Name(request.getSpec2Name());
                }

                if (request.getSpec2Value() != null) {
                        sku.setSpec2Value(request.getSpec2Value());
                }

                if (request.getPrice() != null) {
                        sku.setPrice(request.getPrice());
                }

                if (request.getStock() != null) {
                        sku.setStock(request.getStock());
                }

                if (request.getStatus() != null) {
                        sku.setStatus(request.getStatus());
                }

                ProductSku updatedSku = productSkuRepository.save(sku);

                return toProductSkuResponse(updatedSku);
        }

        // 批次新增商品 SKU
        @Transactional
        public List<ProductSkuResponse> createSkus(
                        Integer productId,
                        List<ProductSkuCreateRequest> requests,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                return createSkus(productId, requests);
        }

        @Transactional
        public List<ProductSkuResponse> createSkus(
                        Integer productId,
                        List<ProductSkuCreateRequest> requests) {

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品：" + productId));

                if (requests == null || requests.isEmpty()) {
                        throw new RuntimeException("至少需要一筆 SKU");
                }

                List<ProductSku> existingSkus = productSkuRepository.findByProductProductId(productId);

                // 先檢查整批資料
                for (int i = 0; i < requests.size(); i++) {

                        ProductSkuCreateRequest request = requests.get(i);
                        boolean noSpec = request.getSpec1Name() == null &&
                                        request.getSpec1Value() == null &&
                                        request.getSpec2Name() == null &&
                                        request.getSpec2Value() == null;

                        if (!noSpec) {

                                // 有規格時，規格一名稱和值必須都有
                                if (request.getSpec1Name() == null ||
                                                request.getSpec1Name().isBlank() ||
                                                request.getSpec1Value() == null ||
                                                request.getSpec1Value().isBlank()) {

                                        throw new IllegalArgumentException(
                                                        "有規格商品必須提供規格一名稱與規格值");
                                }

                                // spec2Name / spec2Value 必須同時存在
                                boolean hasSpec2Name = request.getSpec2Name() != null &&
                                                !request.getSpec2Name().isBlank();

                                boolean hasSpec2Value = request.getSpec2Value() != null &&
                                                !request.getSpec2Value().isBlank();

                                if (hasSpec2Name != hasSpec2Value) {
                                        throw new IllegalArgumentException(
                                                        "規格二名稱與規格值必須同時提供");
                                }
                        }

                        // 檢查是否與資料庫既有 SKU 重複
                        boolean duplicateExisting = existingSkus.stream().anyMatch(sku -> Objects.equals(
                                        sku.getSpec1Value(),
                                        request.getSpec1Value())
                                        &&
                                        Objects.equals(
                                                        sku.getSpec2Value(),
                                                        request.getSpec2Value()));

                        if (duplicateExisting) {
                                throw new RuntimeException(
                                                "SKU 已存在："
                                                                + request.getSpec1Value()
                                                                + " / "
                                                                + request.getSpec2Value());
                        }

                        // 檢查這次送進來的 batch 本身是否重複
                        for (int j = i + 1; j < requests.size(); j++) {

                                ProductSkuCreateRequest other = requests.get(j);

                                boolean duplicateInBatch = Objects.equals(
                                                request.getSpec1Value(),
                                                other.getSpec1Value())
                                                &&
                                                Objects.equals(
                                                                request.getSpec2Value(),
                                                                other.getSpec2Value());

                                if (duplicateInBatch) {
                                        throw new RuntimeException(
                                                        "批次資料中有重複 SKU："
                                                                        + request.getSpec1Value()
                                                                        + " / "
                                                                        + request.getSpec2Value());
                                }
                        }
                }

                // 全部驗證成功後才開始新增
                List<ProductSkuResponse> result = new ArrayList<>();

                for (ProductSkuCreateRequest request : requests) {

                        ProductSku sku = new ProductSku();

                        sku.setProduct(product);

                        sku.setSpec1Name(request.getSpec1Name());
                        sku.setSpec1Value(request.getSpec1Value());

                        sku.setSpec2Name(request.getSpec2Name());
                        sku.setSpec2Value(request.getSpec2Value());

                        sku.setPrice(request.getPrice());
                        sku.setStock(request.getStock());
                        sku.setStatus((byte) 1);

                        ProductSku savedSku = productSkuRepository.save(sku);

                        result.add(toProductSkuResponse(savedSku));
                }

                return result;
        }

        public ProductSkuResponse disableSku(
                        Integer productId,
                        Integer skuId,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                return disableSku(productId, skuId);
        }

        public ProductSkuResponse disableSku(
                        Integer productId,
                        Integer skuId) {

                ProductSku sku = productSkuRepository.findById(skuId)
                                .orElseThrow(() -> new RuntimeException("找不到 SKU：" + skuId));

                if (!sku.getProduct().getProductId().equals(productId)) {
                        throw new RuntimeException("此 SKU 不屬於指定商品");
                }

                sku.setStatus((byte) 0);

                ProductSku updatedSku = productSkuRepository.save(sku);

                return toProductSkuResponse(updatedSku);
        }

        // 修改商品主圖
        @Transactional
        public ProductImageResponse updateMainImage(
                        Integer productId,
                        Integer imageId,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                return updateMainImage(productId, imageId);
        }

        @Transactional
        public ProductImageResponse updateMainImage(
                        Integer productId,
                        Integer imageId) {

                ProductImage targetImage = productImageRepository.findById(imageId)
                                .orElseThrow(() -> new RuntimeException("找不到圖片：" + imageId));

                // 確認圖片屬於指定商品
                if (!targetImage.getProduct()
                                .getProductId()
                                .equals(productId)) {

                        throw new RuntimeException(
                                        "此圖片不屬於商品：" + productId);
                }

                List<ProductImage> images = productImageRepository.findByProductProductId(productId);

                // 1. 先把目前所有主圖取消
                for (ProductImage image : images) {
                        if (Boolean.TRUE.equals(image.getIsMain())) {
                                image.setIsMain(false);
                        }
                }

                // 2. 強制先寫入 DB
                productImageRepository.saveAll(images);
                productImageRepository.flush();

                // 3. 再把指定圖片設為主圖
                targetImage.setIsMain(true);

                ProductImage savedImage = productImageRepository.saveAndFlush(targetImage);

                return ProductImageResponse.builder()
                                .imageId(savedImage.getImageId())
                                .imageUrl(savedImage.getImageUrl())
                                .sortOrder(savedImage.getSortOrder())
                                .isMain(savedImage.getIsMain())
                                .build();
        }

        // 修改商品圖片排序
        @Transactional
        public List<ProductImageResponse> updateImageSort(
                        Integer productId,
                        List<ProductImageSortUpdateRequest> requests,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                return updateImageSort(productId, requests);
        }

        @Transactional
        public List<ProductImageResponse> updateImageSort(
                        Integer productId,
                        List<ProductImageSortUpdateRequest> requests) {

                if (requests == null || requests.isEmpty()) {
                        throw new RuntimeException("圖片排序資料不可為空");
                }

                List<ProductImage> images = new ArrayList<>();

                // ① 先把所有 request 對應的圖片查出來並驗證
                for (ProductImageSortUpdateRequest request : requests) {

                        ProductImage image = productImageRepository
                                        .findById(request.getImageId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "找不到圖片：" + request.getImageId()));

                        // 確認圖片屬於這個商品
                        if (!image.getProduct()
                                        .getProductId()
                                        .equals(productId)) {

                                throw new RuntimeException(
                                                "圖片不屬於商品：" + productId);
                        }

                        // sortOrder 從 1 開始
                        if (request.getSortOrder() == null ||
                                        request.getSortOrder() < 1) {

                                throw new RuntimeException(
                                                "圖片排序必須大於等於 1");
                        }

                        images.add(image);
                }

                // ② 第一階段：
                // 先全部改成暫時、不會與正式排序衝突的值
                for (int i = 0; i < images.size(); i++) {

                        ProductImage image = images.get(i);

                        // 使用大數字當暫存 sortOrder
                        image.setSortOrder(10000 + i);
                }

                productImageRepository.saveAll(images);

                // 強制先寫進 DB
                productImageRepository.flush();

                // ③ 第二階段：
                // 再設定真正的 sortOrder
                for (int i = 0; i < requests.size(); i++) {

                        ProductImageSortUpdateRequest request = requests.get(i);
                        ProductImage image = images.get(i);

                        image.setSortOrder(request.getSortOrder());
                }

                productImageRepository.saveAll(images);
                productImageRepository.flush();

                // ④ 回傳結果
                return images.stream()
                                .map(image -> ProductImageResponse.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .sortOrder(image.getSortOrder())
                                                .isMain(image.getIsMain())
                                                .build())
                                .toList();
        }

        @Transactional
        public void deleteImage(
                        Integer productId,
                        Integer imageId,
                        Integer memberId) {

                requireSellerProduct(productId, memberId);
                deleteImage(productId, imageId);
        }

        @Transactional
        public void deleteImage(
                        Integer productId,
                        Integer imageId) {

                ProductImage image = productImageRepository.findById(imageId)
                                .orElseThrow(() -> new RuntimeException("找不到圖片：" + imageId));

                if (!image.getProduct()
                                .getProductId()
                                .equals(productId)) {

                        throw new RuntimeException(
                                        "圖片不屬於商品：" + productId);
                }

                // 主圖先禁止刪除，避免商品沒有主圖
                if (Boolean.TRUE.equals(image.getIsMain())) {
                        throw new RuntimeException(
                                        "主圖不可直接刪除，請先設定其他圖片為主圖");
                }

                productImageRepository.delete(image);
        }

        @Transactional
        public List<ProductImageResponse> uploadProductImages(
                        Integer productId,
                        MultipartFile[] files,
                        Integer memberId) {

                long authStart = System.currentTimeMillis();

                requireSellerProduct(productId, memberId);

                long authEnd = System.currentTimeMillis();

                System.out.println(
                                "驗證商品耗時：" +
                                                (authEnd - authStart) +
                                                " ms");

                return uploadProductImages(productId, files);
        }

        @Transactional
        public List<ProductImageResponse> uploadProductImages(
                        Integer productId,
                        MultipartFile[] files) {

                long totalStart = System.currentTimeMillis();

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("找不到商品：" + productId));

                if (files == null || files.length == 0) {
                        throw new RuntimeException("請至少上傳一張圖片");
                }

                // ==========================================
                // 1. 先驗證所有檔案
                // ==========================================
                for (MultipartFile file : files) {

                        if (file == null || file.isEmpty()) {
                                throw new RuntimeException("圖片檔案不可為空");
                        }

                        String contentType = file.getContentType();

                        if (contentType == null ||
                                        !contentType.startsWith("image/")) {

                                throw new RuntimeException("只能上傳圖片檔案");
                        }
                }

                // ==========================================
                // 2. Cloudinary 平行上傳
                // 這裡不要碰 JPA
                // ==========================================
                List<CompletableFuture<String>> uploadTasks = Arrays.stream(files)
                                .map(file -> CompletableFuture.supplyAsync(() -> {

                                        try {

                                                long start = System.currentTimeMillis();

                                                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                                                                file.getBytes(),
                                                                ObjectUtils.asMap(
                                                                                "folder",
                                                                                "dinogo/products",
                                                                                "resource_type",
                                                                                "image"));

                                                long end = System.currentTimeMillis();

                                                System.out.println(
                                                                Thread.currentThread().getName()
                                                                                + " Cloudinary 上傳耗時："
                                                                                + (end - start)
                                                                                + " ms");

                                                return uploadResult
                                                                .get("secure_url")
                                                                .toString();

                                        } catch (IOException e) {

                                                throw new CompletionException(e);
                                        }

                                }))
                                .toList();

                // ==========================================
                // 3. 等所有 Cloudinary 上傳完成
                //
                // join() 依原本 files 順序取得結果，
                // 所以圖片順序不會因為平行上傳亂掉
                // ==========================================
                List<String> imageUrls;

                try {

                        imageUrls = uploadTasks.stream()
                                        .map(CompletableFuture::join)
                                        .toList();

                } catch (CompletionException e) {

                        throw new RuntimeException(
                                        "Cloudinary 圖片上傳失敗",
                                        e.getCause());
                }

                // ==========================================
                // 4. Cloudinary 全部完成後
                // 才開始處理 JPA / DB
                // ==========================================
                List<ProductImage> existingImages = productImageRepository
                                .findByProductProductId(productId);

                int maxSortOrder = existingImages.stream()
                                .map(ProductImage::getSortOrder)
                                .filter(Objects::nonNull)
                                .max(Integer::compareTo)
                                .orElse(0);

                List<ProductImageResponse> responses = new ArrayList<>();

                // ==========================================
                // 5. DB 仍然依序寫入
                // ==========================================
                for (String imageUrl : imageUrls) {

                        maxSortOrder++;

                        ProductImage image = ProductImage.builder()
                                        .product(product)
                                        .imageUrl(imageUrl)
                                        .sortOrder(maxSortOrder)
                                        .isMain(false)
                                        .build();

                        ProductImage saved = productImageRepository.save(image);

                        responses.add(
                                        ProductImageResponse.builder()
                                                        .imageId(saved.getImageId())
                                                        .imageUrl(saved.getImageUrl())
                                                        .sortOrder(saved.getSortOrder())
                                                        .isMain(saved.getIsMain())
                                                        .build());
                }

                long totalEnd = System.currentTimeMillis();

                System.out.println(
                                "整個 uploadProductImages 耗時："
                                                + (totalEnd - totalStart)
                                                + " ms");

                return responses;
        }

        // 商品軟刪除
        @Transactional
        public void deleteProduct(Integer productId, Integer memberId) {
                Product product = requireSellerProduct(productId, memberId);

                if (product.getStatus() == 3) {
                        throw new RuntimeException("商品已刪除");
                }

                product.setStatus((byte) 3);

                productRepository.save(product);
        }

        // 商家商品詳情圖片上傳
        public Map<String, String> uploadDescriptionImage(MultipartFile file) {

                if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException("請選擇圖片");
                }

                String contentType = file.getContentType();

                if (contentType == null || !contentType.startsWith("image/")) {
                        throw new IllegalArgumentException("只能上傳圖片");
                }

                try {
                        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                                        file.getBytes(),
                                        ObjectUtils.asMap(
                                                        "folder", "dinogo/product-descriptions",
                                                        "resource_type", "image"));

                        String imageUrl = uploadResult
                                        .get("secure_url")
                                        .toString();

                        return Map.of(
                                        "imageUrl",
                                        imageUrl);

                } catch (IOException e) {
                        throw new RuntimeException(
                                        "商品描述圖片上傳失敗",
                                        e);
                }
        }
}

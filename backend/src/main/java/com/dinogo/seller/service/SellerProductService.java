package com.dinogo.seller.service;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.seller.dto.SellerProductResponse;
import com.dinogo.seller.repository.SellerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;

    public SellerProductService(
            ProductRepository productRepository,
            SellerRepository sellerRepository) {
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
    }

    private void validateActiveSeller(Integer sellerId) {
        boolean sellerIsActive = sellerRepository.existsBySellerIdAndStatus(sellerId, "ACTIVE");

        if (!sellerIsActive) {
            throw new IllegalArgumentException(
                    "Seller not found or inactive.");
        }
    }

    @Transactional(readOnly = true)
    public List<SellerProductResponse> getProducts(Integer sellerId) {
        validateActiveSeller(sellerId);

        return productRepository
                .findBySeller_SellerIdAndStatusNotOrderByProductIdDesc(
                        sellerId,
                        (byte) 3)
                .stream()
                .map(SellerProductResponse::from)
                .toList();
    }

    @Transactional
    public SellerProductResponse disableProduct(
            Integer sellerId,
            Integer productId) {

        validateActiveSeller(sellerId);

        Product product = productRepository
                .findBySeller_SellerIdAndProductId(sellerId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found for seller."));

        product.setStatus((byte) 2);

        return SellerProductResponse.from(
                productRepository.save(product));
    }
}
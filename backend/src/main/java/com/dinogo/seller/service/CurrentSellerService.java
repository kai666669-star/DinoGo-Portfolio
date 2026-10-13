package com.dinogo.seller.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class CurrentSellerService {

    private final SellerRepository sellerRepository;

    public CurrentSellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Transactional(readOnly = true)
    public Integer requireActiveSellerId(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        if (!"ACTIVE".equals(seller.getStatus())) {
            throw new IllegalArgumentException("Seller is inactive.");
        }

        return seller.getSellerId();
    }

    @Transactional(readOnly = true)
    public Integer requireMatchingActiveSellerId(Integer memberId, Integer requestedSellerId) {
        Integer currentSellerId = requireActiveSellerId(memberId);

        if (requestedSellerId != null && !currentSellerId.equals(requestedSellerId)) {
            throw new IllegalArgumentException("Requested seller does not match current seller.");
        }

        return currentSellerId;
    }
}

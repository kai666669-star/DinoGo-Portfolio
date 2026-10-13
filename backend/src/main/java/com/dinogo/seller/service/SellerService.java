package com.dinogo.seller.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.member.service.MemberService;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class SellerService {

    private static final String DEFAULT_SELLER_STATUS = "ACTIVE";

    private final SellerRepository sellerRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    public SellerService(
            SellerRepository sellerRepository,
            MemberRepository memberRepository,
            MemberService memberService) {
        this.sellerRepository = sellerRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
    }

    @Transactional
    public Seller createSellerFromApplication(
            Integer memberId,
            String storeName,
            String storeDescription,
            String storeLogoUrl) {
        if (sellerRepository.existsByMember_MemberId(memberId)) {
            throw new IllegalArgumentException("該會員已經是商家");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("找不到會員"));

        LocalDateTime now = LocalDateTime.now();

        Seller seller = new Seller();
        seller.setMember(member);
        seller.setStoreName(storeName);
        seller.setStoreDescription(storeDescription);
        seller.setStoreLogoUrl(SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl(
                storeLogoUrl,
                "店鋪 Logo URL"));
        seller.setStatus(DEFAULT_SELLER_STATUS);
        seller.setCreatedAt(now);
        seller.setUpdatedAt(now);

        Seller savedSeller = sellerRepository.save(seller);

        memberService.grantSellerRole(memberId);
        memberService.increaseAuthVersion(memberId);
        return savedSeller;
    }

    @Transactional(readOnly = true)
    public Seller getSellerByMemberId(Integer memberId) {
        return sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("尚未建立商家資料"));
    }
}

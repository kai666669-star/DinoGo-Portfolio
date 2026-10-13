package com.dinogo.seller.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.seller.dto.SellerSysmsgResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;
import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.service.MemberSysmsgProviderService;

//rev+msg-start，總共1次修改，第1次//
/**
 * 單體應用內提供 review 與 sysmsg 的商家資料；不額外暴露 HTTP API。
 * 商家收件地址優先由 Seller 模組的商家／會員關聯資料取得；若 Seller 無法提供，
 * 再以 memberId 呼叫 MemberSysmsgProviderService 取得會員登入 Email。
 * 訂閱偏好與 Email 地址分開傳遞。
 */
@Service
public class SellerSysmsgProviderService {

    private final SellerRepository sellerRepository;
    private final MemberSysmsgProviderService memberProvider;

    public SellerSysmsgProviderService(SellerRepository sellerRepository,
            MemberSysmsgProviderService memberProvider) {
        this.sellerRepository = sellerRepository;
        this.memberProvider = memberProvider;
    }

    @Transactional(readOnly = true)
    public SellerSysmsgResponse getSeller(Integer sellerId) {
        Seller seller = sellerRepository.findBySellerIdAndStatusIgnoreCase(sellerId, "ACTIVE")
                .orElseThrow(() -> new NoSuchElementException("Active seller not found: " + sellerId));
        return toResponse(seller);
    }

    /** 管理端訊息歷史需顯示當時寄件商家的名稱；歷史資料不受目前啟用狀態限制。 */
    @Transactional(readOnly = true)
    public String getSellerName(Integer sellerId) {
        return sellerRepository.findBySellerId(sellerId)
                .map(Seller::getStoreName)
                .orElse(null);
    }

    /** OA 廣播取得全部 ACTIVE 商家；每位商家建立獨立 Record。 */
    @Transactional(readOnly = true)
    public List<SellerSysmsgResponse> getAllSellers() {
        return sellerRepository.findAllByStatusIgnoreCase("ACTIVE").stream()
                .map(this::toResponse)
                .toList();
    }

    private SellerSysmsgResponse toResponse(Seller seller) {
        String sellerEmail = seller.getMember() == null
                ? null
                : normalizeEmail(seller.getMember().getEmail());
        Boolean orderPreference = seller.getMember() == null
                ? null
                : seller.getMember().isEmailOrderNotifications();
        Boolean marketingPreference = seller.getMember() == null
                ? null
                : seller.getMember().isEmailMarketingNotifications();

        // Seller 模組先提供；只有地址缺失時才以註冊商家時保留的 memberId fallback。
        if (sellerEmail == null) {
            MemberSysmsgResponse member = memberProvider.getMember(seller.getMemberId());
            sellerEmail = normalizeEmail(member.email());
            orderPreference = member.emailOrderNotifications();
            marketingPreference = member.emailMarketingNotifications();
        }
        return new SellerSysmsgResponse(
                seller.getSellerId(),
                seller.getMemberId(),
                "ACTIVE".equalsIgnoreCase(seller.getStatus()),
                seller.getStoreName(),
                sellerEmail,
                orderPreference,
                marketingPreference);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim();
    }
}
//rev+msg-end，總共1次修改，第1次//

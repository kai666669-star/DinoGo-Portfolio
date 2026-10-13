package com.dinogo.seller.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.seller.dto.SellerApplicationRejectRequest;
import com.dinogo.seller.dto.SellerApplicationRequest;
import com.dinogo.seller.dto.SellerApplicationResponse;
import com.dinogo.seller.entity.SellerApplication;
import com.dinogo.seller.entity.SellerApplicationStatus;
import com.dinogo.seller.repository.SellerApplicationRepository;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class SellerApplicationService {

    private static final List<SellerApplicationStatus> ACTIVE_APPLICATION_STATUSES =
            List.of(SellerApplicationStatus.PENDING, SellerApplicationStatus.APPROVED);

    private final SellerApplicationRepository sellerApplicationRepository;
    private final SellerRepository sellerRepository;
    private final MemberRepository memberRepository;
    private final SellerService sellerService;

    public SellerApplicationService(
            SellerApplicationRepository sellerApplicationRepository,
            SellerRepository sellerRepository,
            MemberRepository memberRepository,
            SellerService sellerService) {
        this.sellerApplicationRepository = sellerApplicationRepository;
        this.sellerRepository = sellerRepository;
        this.memberRepository = memberRepository;
        this.sellerService = sellerService;
    }

    @Transactional
    public SellerApplicationResponse submitApplication(
            Integer memberId,
            SellerApplicationRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("找不到會員"));

        if (sellerRepository.existsByMember_MemberId(memberId)) {
            throw new IllegalArgumentException("該會員已經是商家");
        }
        if (sellerApplicationRepository.existsByMember_MemberIdAndStatusIn(
                memberId,
                ACTIVE_APPLICATION_STATUSES)) {
            throw new IllegalArgumentException("已有審核中或已通過的商家申請");
        }

        SellerApplication application = new SellerApplication();
        application.setMember(member);
        application.setStoreName(request.storeName().trim());
        application.setStoreDescription(trimToNull(request.storeDescription()));
        application.setStoreLogoUrl(SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl(
                trimToNull(request.storeLogoUrl()),
                "店鋪 Logo URL"));
        application.setStatus(SellerApplicationStatus.PENDING);

        return SellerApplicationResponse.from(sellerApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public SellerApplicationResponse getMyLatestApplication(Integer memberId) {
        return sellerApplicationRepository.findTopByMember_MemberIdOrderByCreatedAtDesc(memberId)
                .map(SellerApplicationResponse::from)
                .orElseThrow(() -> new NoSuchElementException("尚未送出商家申請"));
    }

    @Transactional(readOnly = true)
    public List<SellerApplicationResponse> listApplications(SellerApplicationStatus status) {
        List<SellerApplication> applications = status == null
                ? sellerApplicationRepository.findAllByOrderByCreatedAtDesc()
                : sellerApplicationRepository.findByStatusOrderByCreatedAtDesc(status);

        return applications.stream()
                .map(SellerApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerApplicationResponse getApplication(Integer applicationId) {
        return SellerApplicationResponse.from(findApplication(applicationId));
    }

    @Transactional
    public SellerApplicationResponse approveApplication(
            Integer applicationId,
            Integer adminMemberId) {
        SellerApplication application = findApplication(applicationId);
        requirePending(application);

        sellerService.createSellerFromApplication(
                application.getMemberId(),
                application.getStoreName(),
                application.getStoreDescription(),
                application.getStoreLogoUrl());

        application.setStatus(SellerApplicationStatus.APPROVED);
        application.setRejectReason(null);
        application.setReviewedBy(adminMemberId);
        application.setReviewedAt(LocalDateTime.now());

        return SellerApplicationResponse.from(sellerApplicationRepository.save(application));
    }

    @Transactional
    public SellerApplicationResponse rejectApplication(
            Integer applicationId,
            Integer adminMemberId,
            SellerApplicationRejectRequest request) {
        SellerApplication application = findApplication(applicationId);
        requirePending(application);

        application.setStatus(SellerApplicationStatus.REJECTED);
        application.setRejectReason(request.rejectReason().trim());
        application.setReviewedBy(adminMemberId);
        application.setReviewedAt(LocalDateTime.now());

        return SellerApplicationResponse.from(application);
    }

    private SellerApplication findApplication(Integer applicationId) {
        return sellerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("查無商家申請"));
    }

    private void requirePending(SellerApplication application) {
        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw new IllegalArgumentException("只能審核 PENDING 狀態的申請");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

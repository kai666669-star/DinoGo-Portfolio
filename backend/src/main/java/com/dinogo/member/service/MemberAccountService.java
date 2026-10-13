package com.dinogo.member.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dinogo.member.dto.AdminMemberResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberAccountStatusHistory;
import com.dinogo.member.repository.MemberAccountStatusHistoryRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class MemberAccountService {
    private static final Set<OrderStatus> UNFINISHED_ORDER_STATUSES = Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED);
    private final MemberRepository memberRepository;
    private final MemberAccountStatusHistoryRepository historyRepository;
    private final SellerRepository sellerRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberAccountService(MemberRepository memberRepository, MemberAccountStatusHistoryRepository historyRepository,
            SellerRepository sellerRepository, OrderRepository orderRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository; this.historyRepository = historyRepository;
        this.sellerRepository = sellerRepository; this.orderRepository = orderRepository; this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminMemberResponse> listMembers(String status, String keyword) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = normalizeKeyword(keyword);
        return memberRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(member -> normalizedStatus == null || normalizedStatus.equals(member.getStatus()))
                .filter(member -> normalizedKeyword == null || matches(member, normalizedKeyword))
                .map(AdminMemberResponse::from).toList();
    }

    @Transactional
    public AdminMemberResponse suspend(Integer memberId, Integer adminId, String reason) {
        if (memberId.equals(adminId)) throw new IllegalArgumentException("不可停權自己的帳號");
        Member member = findMember(memberId);
        if (!"ACTIVE".equals(member.getStatus())) throw new IllegalArgumentException("只有正常帳號可以停權");
        return AdminMemberResponse.from(changeStatus(member, "SUSPENDED", reason.trim(), adminId));
    }

    @Transactional
    public AdminMemberResponse restore(Integer memberId, Integer adminId) {
        if (memberId.equals(adminId)) throw new IllegalArgumentException("不可操作自己的帳號");
        Member member = findMember(memberId);
        if (!"SUSPENDED".equals(member.getStatus())) throw new IllegalArgumentException("只有已停權帳號可以恢復");
        return AdminMemberResponse.from(changeStatus(member, "ACTIVE", "管理員恢復帳號", adminId));
    }

    @Transactional
    public void deactivate(Integer memberId, String currentPassword) {
        Member member = findMember(memberId);
        if (!"ACTIVE".equals(member.getStatus())) throw new IllegalArgumentException("目前帳號無法註銷");
        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) throw new IllegalArgumentException("目前密碼錯誤");
        if (sellerRepository.existsByMember_MemberId(memberId)) throw new IllegalArgumentException("商家會員不可自行註銷帳號");
        if (orderRepository.existsByBuyerIdAndStatusIn(memberId, UNFINISHED_ORDER_STATUSES)) throw new IllegalArgumentException("尚有未完成訂單，無法註銷帳號");
        changeStatus(member, "DEACTIVATED", "會員自行註銷帳號", memberId);
    }

    private Member changeStatus(Member member, String nextStatus, String reason, Integer changedBy) {
        String previousStatus = member.getStatus();
        if (memberRepository.updateStatusIfCurrent(member.getMemberId(), previousStatus, nextStatus) != 1) {
            throw new IllegalArgumentException("帳號狀態已變更，請重新操作");
        }
        MemberAccountStatusHistory history = new MemberAccountStatusHistory();
        history.setMemberId(member.getMemberId()); history.setPreviousStatus(previousStatus); history.setNewStatus(nextStatus);
        history.setReason(reason); history.setChangedBy(changedBy); history.setChangedAt(LocalDateTime.now());
        historyRepository.save(history);
        return findMember(member.getMemberId());
    }
    private Member findMember(Integer memberId) { return memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("Member not found")); }
    private boolean matches(Member member, String keyword) { return (member.getEmail() + " " + member.getLastName() + member.getFirstName() + " " + member.getMemberId()).toLowerCase(Locale.ROOT).contains(keyword); }
    private String normalizeStatus(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String normalizeKeyword(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT); }
}

package com.dinogo.member.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.seller.repository.SellerRepository;

/**
 * In-process member provider for system-message and review integrations.
 */
@Service
public class MemberSysmsgProviderService {

    private static final String DEFAULT_ROLE = "MEMBER";
    private static final int BUYER_ROLE_ID = 1;
    private static final int SELLER_ROLE_ID = 2;

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;

    public MemberSysmsgProviderService(
            MemberRepository memberRepository,
            SellerRepository sellerRepository) {
        this.memberRepository = memberRepository;
        this.sellerRepository = sellerRepository;
    }

    /**
     * Supplies data for the authenticated member resolved by the caller's JWT.
     */
    @Transactional(readOnly = true)
    public MemberSysmsgResponse getProfile(Integer memberId) {
        return toResponse(requireActiveMember(memberId), true);
    }

    /**
     * Supplies an active recipient for system messages and review validation.
     */
    @Transactional(readOnly = true)
    public MemberSysmsgResponse getMember(Integer memberId) {
        return toResponse(requireActiveMember(memberId), false);
    }

    /**
     * Supplies all active recipients for official-account broadcasts.
     */
    @Transactional(readOnly = true)
    public List<MemberSysmsgResponse> getAllMembers() {
        List<Member> members = memberRepository.findAllByStatusIgnoreCase("ACTIVE");
        Map<Integer, Integer> sellerIdsByMemberId = members.isEmpty()
                ? Map.of()
                : sellerRepository.findByMember_MemberIdIn(members.stream()
                                .map(Member::getMemberId)
                                .toList())
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                seller -> seller.getMember().getMemberId(),
                                seller -> seller.getSellerId()));

        return members.stream()
                .map(member -> toResponse(member, sellerIdsByMemberId.get(member.getMemberId()), false))
                .toList();
    }

    private Member requireActiveMember(Integer memberId) {
        return memberRepository.findById(memberId)
                .filter(this::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
    }

    private boolean isActive(Member member) {
        return "ACTIVE".equalsIgnoreCase(member.getStatus());
    }

    private MemberSysmsgResponse toResponse(Member member, boolean authenticated) {
        Integer sellerId = sellerRepository.findByMember_MemberId(member.getMemberId())
                .map(seller -> seller.getSellerId())
                .orElse(null);
        return toResponse(member, sellerId, authenticated);
    }

    private MemberSysmsgResponse toResponse(
            Member member,
            Integer sellerId,
            boolean authenticated) {
        String role = member.getMemberRoles().stream()
                .map(memberRole -> memberRole.getRole().getRoleName())
                .findFirst()
                .orElse(DEFAULT_ROLE);
        List<Integer> roleIds = resolveRoleIds(member);

        return new MemberSysmsgResponse(
                member.getMemberId(),
                sellerId,
                authenticated,
                member.getEmail(),
                role,
                roleIds,
                member.isEmailOrderNotifications(),
                member.isEmailMarketingNotifications());
    }

    private List<Integer> resolveRoleIds(Member member) {
        TreeSet<Integer> roleIds = member.getMemberRoles().stream()
                .map(memberRole -> memberRole.getRole().getRoleId())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if (roleIds.contains(SELLER_ROLE_ID)) {
            roleIds.add(BUYER_ROLE_ID);
        }
        return List.copyOf(roleIds);
    }
}

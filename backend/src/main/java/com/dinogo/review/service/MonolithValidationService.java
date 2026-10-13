package com.dinogo.review.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.review.exception.InvalidOrderStateException;
import com.dinogo.review.exception.ReviewForbiddenException;
import com.dinogo.salesii.dto.OrderSysmsgResponse;

/** Review 對目前登入會員與歷史訂單快照的集中驗證。 */
@Service
//review-start，總共1次修改，第1次//
public class MonolithValidationService {

    private static final int MEMBER_ROLE_ID = 1;
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private final MemberSysmsgProviderService memberService;

    public MonolithValidationService(MemberSysmsgProviderService memberService) {
        this.memberService = memberService;
    }

    public MemberSysmsgResponse requireCurrentMember(Integer currentMemberId) {
        requireId(currentMemberId, "currentMemberId");
        MemberSysmsgResponse member = memberService.getMember(currentMemberId);
        if (member == null || member.memberId() == null) {
            throw new IllegalStateException("會員 Service 未回傳有效會員資料");
        }
        requireRoles(member, MEMBER_ROLE_ID);
        return member;
    }

    /** 歷史事件只驗證單次取得的訂單快照，不依賴會員、商家目前狀態或角色。 */
    public OrderSysmsgResponse requireCompletedOrder(OrderSysmsgResponse order) {
        requireOrderSnapshot(order);
        requireStatus(order, COMPLETED);
        return order;
    }

    /** 取消清理只驗證訂單識別與狀態，不回查買家或商家。 */
    public OrderSysmsgResponse requireCancelledOrder(OrderSysmsgResponse order) {
        requireOrderSnapshot(order);
        requireStatus(order, CANCELLED);
        return order;
    }

    public void requireOwnership(Integer ownerMemberId, Integer currentMemberId) {
        requireId(ownerMemberId, "ownerMemberId");
        requireId(currentMemberId, "currentMemberId");
        if (!ownerMemberId.equals(currentMemberId)) {
            throw new ReviewForbiddenException("目前會員無權操作此評論資料");
        }
    }

    private void requireOrderSnapshot(OrderSysmsgResponse order) {
        if (order == null) {
            throw new IllegalArgumentException("訂單快照不可為 null");
        }
        requireId(order.orderId(), "orderId");
        if (order.orderNo() == null || order.orderNo().isBlank()) {
            throw new IllegalStateException("訂單快照缺少 orderNo");
        }
        if (order.buyerId() == null || order.buyerId() <= 0
                || order.sellerId() == null || order.sellerId() <= 0) {
            throw new IllegalStateException("訂單快照缺少有效 buyerId 或 sellerId");
        }
    }

    private void requireRoles(MemberSysmsgResponse member, Integer... requiredRoleIds) {
        if (member == null || member.memberId() == null) {
            throw new IllegalStateException("會員 Provider 未回傳有效會員資料");
        }
        if (member.roleIds() == null) {
            throw new IllegalStateException("會員 Provider 未回傳 roleIds");
        }
        for (Integer roleId : requiredRoleIds) {
            if (!member.roleIds().contains(roleId)) {
                throw new ReviewForbiddenException(
                        "會員缺少 review 所需角色，role_id=" + roleId);
            }
        }
    }

    private void requireStatus(OrderSysmsgResponse order, String expectedStatus) {
        String actualStatus = order.status();
        if (actualStatus == null || !expectedStatus.equals(actualStatus.toUpperCase(Locale.ROOT))) {
            throw new InvalidOrderStateException(
                    "訂單狀態必須是 " + expectedStatus + "，目前狀態=" + actualStatus);
        }
    }

    private void requireId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " 必須是正整數");
        }
    }
}
//review-end，總共1次修改，第1次//


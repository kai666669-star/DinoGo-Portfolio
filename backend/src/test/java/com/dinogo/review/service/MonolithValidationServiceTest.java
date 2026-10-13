package com.dinogo.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.review.exception.InvalidOrderStateException;
import com.dinogo.review.exception.ReviewForbiddenException;
import com.dinogo.salesii.dto.OrderSysmsgResponse;

//review-start，總共1次修改，第1次//
/** 登入操作驗證目前會員；歷史事件只驗證傳入的訂單快照。 */
class MonolithValidationServiceTest {

    private MemberSysmsgProviderService members;
    private MonolithValidationService validation;

    @BeforeEach
    void setUp() {
        members = mock(MemberSysmsgProviderService.class);
        validation = new MonolithValidationService(members);
    }

    @Test
    void currentMemberRequiresRoleIdOneFromMemberContract() {
        when(members.getMember(7)).thenReturn(member(7, List.of(1)));

        assertEquals(7, validation.requireCurrentMember(7).memberId());

        when(members.getMember(8)).thenReturn(member(8, List.of(3)));
        assertThrows(ReviewForbiddenException.class,
                () -> validation.requireCurrentMember(8));
    }

    @Test
    void completedHistoricalOrderDoesNotQueryCurrentBuyerOrSellerState() {
        OrderSysmsgResponse order = order("COMPLETED");

        assertEquals(order, validation.requireCompletedOrder(order));

        verifyNoInteractions(members);
    }

    @Test
    void historicalOrderStillRequiresExpectedOrderStatus() {
        OrderSysmsgResponse order = order("PROCESSING");

        assertThrows(InvalidOrderStateException.class,
                () -> validation.requireCompletedOrder(order));
        verifyNoInteractions(members);
    }

    private OrderSysmsgResponse order(String status) {
        return new OrderSysmsgResponse(
                10, "ORD-10", 7, 9, status, List.of());
    }

    private MemberSysmsgResponse member(Integer memberId, List<Integer> roleIds) {
        return new MemberSysmsgResponse(
                memberId, null, false, "member@example.test", "MEMBER",
                roleIds, true, false);
    }
}
//review-end，總共1次修改，第1次//


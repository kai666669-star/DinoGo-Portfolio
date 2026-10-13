package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.sysmsg.service.impl.MsgPermissionServiceImpl;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendSellerEntity;
import com.dinogo.sysmsg.entity.SendStatus;

class MsgPermissionServiceImplTest {
    private final MemberSysmsgProviderService members = Mockito.mock(MemberSysmsgProviderService.class);
    private final MsgPermissionServiceImpl permissions = new MsgPermissionServiceImpl(members);

    @Test
    void roleIdThreeIsTheOnlySystemAdministratorAuthority() {
        when(members.getProfile(9)).thenReturn(member(9, null, "MEMBER", List.of(1, 3)));

        assertDoesNotThrow(() -> permissions.validateSystemAdmin(9));
    }

    @Test
    void legacyAdminRoleNameWithoutRoleIdThreeIsRejected() {
        when(members.getProfile(8)).thenReturn(member(8, null, "ADMIN", List.of(1)));

        assertThrows(SecurityException.class, () -> permissions.validateSystemAdmin(8));
    }

    @Test
    void formerFixedIdAdministratorBackdoorIsRejected() {
        when(members.getProfile(1)).thenReturn(member(1, 1, "SELLER", List.of(2)));

        assertThrows(SecurityException.class, () -> permissions.validateSystemAdmin(1));
    }

    @Test
    void sellerRoleAlsoIncludesGeneralMemberRole() {
        MemberSysmsgResponse seller = member(7, 12, "SELLER", List.of(2));
        when(members.getProfile(7)).thenReturn(seller);

        assertEquals(List.of(2), seller.roleIds());
        assertEquals(12, permissions.validateSeller(7));
    }

    @Test
    void sellerIdWithoutSellerRoleIsRejected() {
        when(members.getProfile(6)).thenReturn(member(6, 11, "MEMBER", List.of(1)));

        assertThrows(SecurityException.class, () -> permissions.validateSeller(6));
    }

    @Test
    void sellerCannotModifyAnotherSellersTemplate() {
        when(members.getProfile(7)).thenReturn(member(7, 12, "SELLER", List.of(2)));
        SendEntity otherSellerTemplate = new SendSellerEntity(
                13, "SC-001", "label", "title", "content", SendStatus.SAVE,
                null, null, null, null, null);

        assertThrows(SecurityException.class,
                () -> permissions.validateTemplateOwner(otherSellerTemplate, 7));
    }

    @Test
    void memberCannotReadAnotherMembersRecord() {
        when(members.getProfile(8)).thenReturn(member(8, null, "MEMBER", List.of(1)));
        SendEntity send = new SendEntity(1, "OC-001", "label", "title", "content", SendStatus.SEND);
        RecordEntity otherMembersRecord = new RecordEntity(send, 9, null);

        assertThrows(SecurityException.class,
                () -> permissions.validateRecordOwner(otherMembersRecord, 8));
    }

    private MemberSysmsgResponse member(Integer memberId, Integer sellerId, String role,
            List<Integer> roleIds) {
        return new MemberSysmsgResponse(memberId, sellerId, true, "member@example.com", role,
                roleIds, true, false);
    }
}

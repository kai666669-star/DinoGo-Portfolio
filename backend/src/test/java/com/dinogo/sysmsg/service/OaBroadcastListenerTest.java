package com.dinogo.sysmsg.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.seller.dto.SellerSysmsgResponse;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.service.impl.OaBroadcastListener;

class OaBroadcastListenerTest {
    @Test
    void createsIndependentMemberAndSellerInboxRecords() {
        MemberSysmsgProviderService members = mock(MemberSysmsgProviderService.class);
        SellerSysmsgProviderService sellers = mock(SellerSysmsgProviderService.class);
        RecordService records = mock(RecordService.class);
        when(members.getAllMembers()).thenReturn(List.of(
                new MemberSysmsgResponse(7, null, false, "m@example.com", "MEMBER",
                        List.of(1), true, false)));
        when(sellers.getAllSellers()).thenReturn(List.of(
                new SellerSysmsgResponse(5, 16, true)));

        new OaBroadcastListener(members, sellers, records, 100)
                .broadcast(new OaBroadcastRequested(88));

        verify(records).createRecords(88, List.of(7), List.of());
        verify(records).createRecords(88, List.of(), List.of(5));
    }
}

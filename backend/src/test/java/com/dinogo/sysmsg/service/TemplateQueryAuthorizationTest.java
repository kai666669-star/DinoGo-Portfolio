package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import com.dinogo.member.service.MemberSysmsgProviderService;
//sysmsg-start，總共2次修改，第1次//
import com.dinogo.salesii.service.OrderSysmsgProviderService;
//sysmsg-end，總共2次修改，第1次//
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.dto.response.OffsetPageResponse;
import com.dinogo.sysmsg.dto.response.SendTemplateResponse;
import com.dinogo.sysmsg.entity.SendStatus;
import com.dinogo.sysmsg.repository.SendRepository;
import com.dinogo.sysmsg.service.impl.SendServiceImpl;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;

class TemplateQueryAuthorizationTest {

    @Test
    void systemTemplatesUseAdminPermissionAndSystemTypesWithoutSellerId() {
        Fixture fixture = new Fixture();
        when(fixture.sends.findSystemTemplates(eq(SendStatus.SAVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        OffsetPageResponse<SendTemplateResponse> page = fixture.service
                .findSystemTemplates(9, 0);

        assertTrue(page.items().isEmpty());
        verify(fixture.permissions).validateSystemAdmin(9);
        verify(fixture.permissions, never()).validateSeller(any());
        verify(fixture.sends).findSystemTemplates(eq(SendStatus.SAVE), any(Pageable.class));
        verify(fixture.sends, never()).findBySenderAndStatus(any(), any(), any(),
                any(Pageable.class));
    }

    @Test
    void sellerTemplatesResolveSellerIdAndRestrictQueryToSc() {
        Fixture fixture = new Fixture();
        when(fixture.permissions.validateSeller(7)).thenReturn(12);
        when(fixture.sends.findBySenderAndStatus(eq(12), eq(SendStatus.SAVE), eq("SC"),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        OffsetPageResponse<SendTemplateResponse> page = fixture.service
                .findSellerTemplates(7, 0);

        assertTrue(page.items().isEmpty());
        verify(fixture.permissions).validateSeller(7);
        verify(fixture.permissions, never()).validateSystemAdmin(any());
        verify(fixture.sends).findBySenderAndStatus(eq(12), eq(SendStatus.SAVE), eq("SC"),
                any(Pageable.class));
        verify(fixture.sends, never()).findSystemTemplates(any(), any());
    }

    private static class Fixture {
        private final SendRepository sends = mock(SendRepository.class);
        private final MsgPermissionService permissions = mock(MsgPermissionService.class);
        private final SendServiceImpl service = new SendServiceImpl(
                sends,
                mock(RecordService.class),
                mock(TemplateNumService.class),
                permissions,
                mock(OrderSysmsgProviderService.class),
                mock(MemberSysmsgProviderService.class),
                mock(SellerSysmsgProviderService.class),
                mock(org.springframework.context.ApplicationEventPublisher.class),
                mock(SendResponseMapper.class),
                //sysmsg-start，總共2次修改，第2次//
                mock(SysmsgImageService.class));
                //sysmsg-end，總共2次修改，第2次//
    }
}

package com.dinogo.sysmsg.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.dto.request.send.SysCreateRequest;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.repository.SendRepository;
import com.dinogo.sysmsg.service.impl.SendServiceImpl;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;

class SystemDirectMessageTest {

    @Test
    void oaWithMemberIdCreatesOnlyOneMemberRecord() {
        SendRepository sends = mock(SendRepository.class);
        RecordService records = mock(RecordService.class);
        TemplateNumService numbers = mock(TemplateNumService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        SendResponseMapper mapper = new SendResponseMapper();
        SendServiceImpl service = new SendServiceImpl(sends, records, numbers,
                mock(MsgPermissionService.class), mock(OrderSysmsgProviderService.class),
                mock(MemberSysmsgProviderService.class), mock(SellerSysmsgProviderService.class),
                events, mapper, mock(SysmsgImageService.class));
        when(numbers.generateMsgFunction("OA")).thenReturn("OA-001");
        when(sends.save(any(SendEntity.class))).thenAnswer(invocation -> {
            SendEntity send = invocation.getArgument(0);
            send.setSendId(101);
            return send;
        });

        SysCreateRequest request = new SysCreateRequest();
        request.setMsgType("OA");
        request.setMsgtoMemberId(42);
        request.setSendTitle("title");
        request.setSendContent("content");

        service.createSystemSend(request, 3);

        verify(records).createSingleMemberRecord(101, 42);
        verify(events, never()).publishEvent(any());
    }
}

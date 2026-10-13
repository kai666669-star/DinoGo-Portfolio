package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.dinogo.member.service.MemberSysmsgProviderService;
//sysmsg-start，總共2次修改，第1次//
import com.dinogo.salesii.service.OrderSysmsgProviderService;
//sysmsg-end，總共2次修改，第1次//
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.dto.request.template.SendTemplateUpdateRequest;
import com.dinogo.sysmsg.dto.response.SendTemplateResponse;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;
import com.dinogo.sysmsg.repository.SendRepository;
import com.dinogo.sysmsg.service.impl.SendServiceImpl;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;

class SendTemplateRenumberingTest {
    private final SendRepository sends = mock(SendRepository.class);
    private final TemplateNumService numbers = mock(TemplateNumService.class);
    private final MsgPermissionService permissions = mock(MsgPermissionService.class);
    private final SendResponseMapper mapper = mock(SendResponseMapper.class);
    private SendServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SendServiceImpl(sends, mock(RecordService.class), numbers, permissions,
                mock(OrderSysmsgProviderService.class), mock(MemberSysmsgProviderService.class),
                mock(SellerSysmsgProviderService.class), mock(ApplicationEventPublisher.class),
                //sysmsg-start，總共2次修改，第2次//
                mapper, mock(SysmsgImageService.class));
                //sysmsg-end，總共2次修改，第2次//
        when(sends.save(any(SendEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toTemplateResponse(any())).thenReturn(new SendTemplateResponse());
    }

    @Test
    void updatesSameTemplateWithoutRenumberingWhenNoSentHistoryExists() {
        SendEntity template = template();
        when(sends.findById(10)).thenReturn(java.util.Optional.of(template));
        when(sends.existsByMsgFunctionAndSendStatus("OA-001", SendStatus.SEND)).thenReturn(false);

        service.updateTemplate(10, update(), 3);

        verify(numbers, never()).generateMsgFunction(any());
        verify(sends, never()).delete(any());
        assertEquals("new title", template.getSendTitle());
    }

    @Test
    void replacesAndRenumbersTemplateWhenSentHistoryExists() {
        SendEntity template = template();
        when(sends.findById(10)).thenReturn(java.util.Optional.of(template));
        when(sends.existsByMsgFunctionAndSendStatus("OA-001", SendStatus.SEND)).thenReturn(true);
        when(numbers.generateMsgFunction("OA")).thenReturn("OA-002");

        service.updateTemplate(10, update(), 3);

        verify(numbers).generateMsgFunction("OA");
        verify(sends).delete(template);
        verify(sends).save(org.mockito.ArgumentMatchers.argThat(
                replacement -> "OA-002".equals(replacement.getMsgFunction())));
    }

    private SendEntity template() {
        return new SendEntity(1, "OA-001", "label", "title", "content", SendStatus.SAVE);
    }

    private SendTemplateUpdateRequest update() {
        SendTemplateUpdateRequest request = new SendTemplateUpdateRequest();
        request.setMsgLabel("new label");
        request.setSendTitle("new title");
        request.setSendContent("new content");
        return request;
    }
}

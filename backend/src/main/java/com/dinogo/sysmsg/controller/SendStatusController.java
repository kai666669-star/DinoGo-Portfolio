package com.dinogo.sysmsg.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.dto.request.record.SendStatusUpdateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.service.SendStatusService;

@RestController
@RequestMapping("/api/sysmsg/sends")
public class SendStatusController {
    private final SendStatusService service; private final ControllerSupport auth;
    public SendStatusController(SendStatusService service,ControllerSupport auth){this.service=service;this.auth=auth;}
    @PatchMapping("/{sendId}/status")
    public SendResponse status(@PathVariable Integer sendId,@Valid @RequestBody SendStatusUpdateRequest request){return service.changeSendStatus(sendId,request,auth.memberId());}
}

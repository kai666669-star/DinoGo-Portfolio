package com.dinogo.sysmsg.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.dinogo.sysmsg.dto.request.send.SysCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.response.OffsetPageResponse;
import com.dinogo.sysmsg.dto.response.RecordResponse;
import com.dinogo.sysmsg.service.RecordService;
import com.dinogo.sysmsg.service.ManualMessageService;

@RestController
@RequestMapping("/api/sysmsg/system/messages")
public class SystemMessageController {
    private final ManualMessageService service; private final RecordService records; private final ControllerSupport auth;
    public SystemMessageController(ManualMessageService service,RecordService records,ControllerSupport auth){this.service=service;this.records=records;this.auth=auth;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SendResponse create(@Valid @RequestBody SysCreateRequest request){return service.createSystemSend(request,auth.memberId());}
    @GetMapping("/records")
    public OffsetPageResponse<RecordResponse> records(@RequestParam(defaultValue = "0") Integer page){return records.getSystemRecords(auth.memberId(),page);}
}
